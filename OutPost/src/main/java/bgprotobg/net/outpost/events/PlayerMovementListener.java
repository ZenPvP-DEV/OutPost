package bgprotobg.net.outpost.events;

import bgprotobg.net.outpost.OutPost;
import bgprotobg.net.outpost.database.OutpostDAO;
import bgprotobg.net.outpost.models.Outpost;
import com.edwardbelt.edprison.api.models.EconomyModel;
import com.edwardbelt.edprison.events.EdPrisonAddMultiplierCurrency;
import me.lavaturtle.moltengangs.MoltenGangs;
import me.lavaturtle.moltengangs.object.Gang;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerMovementListener implements Listener {

    private final OutPost plugin;
    public OutpostDAO outpostDAO;
     final EconomyModel economyModel = new EconomyModel();

    private final Map<String, UUID> capturingPlayers = new ConcurrentHashMap<>();
    public final Map<String, UUID> capturedByIslands = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> countdownTasks = new ConcurrentHashMap<>();
    private final Map<UUID, String> gangCurrencyTypes = new ConcurrentHashMap<>();
    private final MoltenGangs moltenGangs;
    private Economy economy;
    private final Map<UUID, Double> islandBoosts = new ConcurrentHashMap<>();

    public PlayerMovementListener(OutPost plugin, MoltenGangs moltenGangs, OutpostDAO outpostDAO) {
        this.plugin = plugin;
        this.moltenGangs = moltenGangs;
        this.outpostDAO = outpostDAO;
        setupEconomy();
        loadCapturedOutpostsAndApplyBoosts();
        reapplyBoosts();
    }

    private void setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();

        if (to == null || from == null || to.equals(from)) {
            return;
        }

        Map<String, Outpost> outposts = plugin.getOutpostCommand().getOutposts();
        for (Outpost outpost : outposts.values()) {
            boolean toInside = isInside(outpost, to);
            boolean fromInside = isInside(outpost, from);
            if (toInside && !fromInside) {
                handleEnterOutpost(player, outpost);
            } else if (!toInside && fromInside) {
                handleLeaveOutpost(player, outpost);
            }
        }
    }

    private boolean isInside(Outpost outpost, Location loc) {
        Location min = new Location(outpost.getPos1().getWorld(),
                Math.min(outpost.getPos1().getX(), outpost.getPos2().getX()),
                0,
                Math.min(outpost.getPos1().getZ(), outpost.getPos2().getZ()));
        Location max = new Location(outpost.getPos1().getWorld(),
                Math.max(outpost.getPos1().getX(), outpost.getPos2().getX()),
                255,
                Math.max(outpost.getPos1().getZ(), outpost.getPos2().getZ()));

        double x = loc.getX();
        double z = loc.getZ();

        return (x >= min.getX() && x <= max.getX()) && (z >= min.getZ() && z <= max.getZ());
    }

    private void handleEnterOutpost(Player player, Outpost outpost) {
        UUID playerId = player.getUniqueId();
        Gang playerGang = moltenGangs.getGangStorage().getGang(player);

        if (playerGang == null) {
            sendMessage(player, "messages.must_be_part_of_island");
            return;
        }

        String currentCapturingGangName = outpost.getCapturingIsland();
        String playerGangName = playerGang.getName();

        if (currentCapturingGangName != null && currentCapturingGangName.equals(playerGangName)) {
            sendMessage(player, "messages.already_captured_by_island");
            return;
        }

        if (capturingPlayers.containsKey(outpost.getName())) {
            sendMessage(player, "messages.another_player_capturing");
            return;
        }

        sendMessage(player, "messages.start_capturing");
        capturingPlayers.put(outpost.getName(), playerId);

        int taskId = new BukkitRunnable() {
            int secondsLeft = plugin.getCaptureDuration();
            double captureStep = 100.0 / secondsLeft;

            @Override
            public void run() {
                if (!isInside(outpost, player.getLocation())) {
                    cancelCapture(player, outpost);
                    cancel();
                    return;
                }

                double newCapturePercentage = outpost.getCapturePercentage() + captureStep;
                outpost.setCapturePercentage(newCapturePercentage);
                outpostDAO.saveOutpost(outpost);

                String actionBarMessage = plugin.getConfig().getString("messages.capture_action_bar");
                if (actionBarMessage != null) {
                    actionBarMessage = actionBarMessage.replace("{seconds_left}", String.valueOf(secondsLeft));
                    player.sendActionBar(ChatColor.translateAlternateColorCodes('&', actionBarMessage));
                }

                if (secondsLeft <= 0) {
                    captureOutpost(player, playerGang, outpost);
                    cancel();
                }

                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0, 20).getTaskId();

        countdownTasks.put(playerId, taskId);

        int broadcastInterval = plugin.getConfig().getInt("messages.capturing_broadcast_interval_seconds", 10);
        String capturingMessage = plugin.getConfig().getString("messages.broadcast_capturing");

        new BukkitRunnable() {
            @Override
            public void run() {
                UUID currentCapturingPlayerId = capturingPlayers.get(outpost.getName());
                if (currentCapturingPlayerId != null && currentCapturingPlayerId.equals(playerId)) {
                    String message = capturingMessage
                            .replace("{gang_name}", playerGang.getName())
                            .replace("{outpost_name}", outpost.getName());
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
                } else {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, broadcastInterval * 20);
    }

    private void removeBoostByGangName(String gangName) {
        Gang gang = moltenGangs.getGangStorage().getGang(gangName);
        if (gang != null) {
            for (UUID memberId : gang.getMembers().keySet()) {
                islandBoosts.remove(memberId);
                gangCurrencyTypes.remove(memberId);
            }
        }
    }


    private void handleLeaveOutpost(Player player, Outpost outpost) {
        UUID playerId = player.getUniqueId();
        UUID capturingPlayerId = capturingPlayers.get(outpost.getName());

        if (capturingPlayerId == null || !capturingPlayerId.equals(playerId)) {
            return;
        }

        Gang playerGang = moltenGangs.getGangStorage().getGang(player);
        if (playerGang != null) {
            for (UUID memberId : playerGang.getMembers().keySet()) {
                if (!memberId.equals(playerId)) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null && isInside(outpost, member.getLocation())) {
                        return;
                    }
                }
            }
        }
        outpost.setCapturePercentage(0.0);
        outpostDAO.saveOutpost(outpost);

        cancelCapture(player, outpost);
    }


    private void cancelCapture(Player player, Outpost outpost) {
        UUID playerId = player.getUniqueId();
        capturingPlayers.remove(outpost.getName());
        Integer taskId = countdownTasks.remove(playerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        sendMessage(player, "messages.stop_capturing");
    }

    private void captureOutpost(Player player, Gang gang, Outpost outpost) {
        UUID playerId = player.getUniqueId();
        String previousCapturingGangName = outpost.getCapturingIsland();

        if (previousCapturingGangName != null) {
            removeBoostByGangName(previousCapturingGangName);

            Gang previousGang = moltenGangs.getGangStorage().getGang(previousCapturingGangName);
            if (previousGang != null) {
                previousGang.getMembers().keySet().forEach(memberId -> {
                    Player gangMember = Bukkit.getPlayer(memberId);
                    if (gangMember != null && gangMember.isOnline()) {
                        sendFormattedMessage(gangMember, "messages.lost_outpost", outpost.getName());
                    }
                });
                Bukkit.broadcastMessage(getFormattedMessage("messages.broadcast_lost_outpost", previousGang.getName(), outpost.getName()));
            }
        }

        capturingPlayers.remove(outpost.getName());
        countdownTasks.remove(playerId);
        outpost.setCapturingIsland(gang.getName());
        outpost.setCapturePercentage(100.0);
        outpostDAO.saveOutpost(outpost);

        Bukkit.broadcastMessage(getFormattedMessage("messages.broadcast_captured", gang.getName(), outpost.getName()));

        applyBoost(gang, outpost.getType());

        int broadcastInterval = plugin.getConfig().getInt("messages.broadcast_interval_seconds", 300);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (outpost.getCapturingIsland().equals(gang.getName())) {
                    Bukkit.broadcastMessage(getFormattedMessage("messages.broadcast_controlling", gang.getName(), outpost.getName()));
                } else {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, broadcastInterval * 20);
    }

    private void applyBoost(Gang gang, String outpostType) {
        if (plugin.getMoltenGangs() == null) {
            return;
        }

        double boost = 0.0;
        String currency = "";

        switch (outpostType) {
            case "Money":
                boost = plugin.getMoneyBoost();
                currency = "money";
                break;
            case "Tokens":
                boost = plugin.getTokenBoost();
                currency = "tokens";
                break;
            case "Gems":
                boost = plugin.getGemBoost();
                currency = "gems";
                break;
            case "XP":
                boost = plugin.getCrystalBoost();
                currency = "xp";
                break;
            default:
                return;
        }

        for (UUID memberId : gang.getMembers().keySet()) {
            islandBoosts.put(memberId, boost);
            gangCurrencyTypes.put(memberId, currency);

            Player player = Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                Bukkit.getLogger().info("Boost of " + boost + " applied to " + currency + " for player " + player.getName());
            }
        }
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();

        if (plugin.getMoltenGangs() == null) {
            return;
        }

        Gang playerGang = plugin.getMoltenGangs().getGangStorage().getGang(player.getUniqueId());
        if (playerGang != null && islandBoosts.containsKey(playerGang.getLeader())) {
            double boost = islandBoosts.get(playerGang.getLeader());
            String outpostType = getOutpostTypeForIsland(playerGang.getLeader());

            if ("XP".equals(outpostType)) {
                int originalAmount = event.getAmount();
                int boostedAmount = (int) Math.round(originalAmount * (1 + boost));
                event.setAmount(boostedAmount);
            }
        }
    }


    private String getOutpostTypeForIsland(UUID islandId) {
        double boost = islandBoosts.getOrDefault(islandId, 0.0);

        if (boost == plugin.getMoneyBoost()) {
            return "Money";
        } else if (boost == plugin.getTokenBoost()) {
            return "Tokens";
        } else if (boost == plugin.getGemBoost()) {
            return "Gems";
        } else if (boost == plugin.getCrystalBoost()) {
            return "XP";
        }

        return "";
    }

    private void sendMessage(Player player, String configPath) {
        String message = plugin.getConfig().getString(configPath);
        if (message != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    private void sendFormattedMessage(Player player, String configPath, String... placeholders) {
        String message = plugin.getConfig().getString(configPath);
        if (message != null) {
            player.sendMessage(formatMessage(message, placeholders));
        }
    }

    private String getFormattedMessage(String configPath, String... placeholders) {
        String message = plugin.getConfig().getString(configPath);
        if (message != null) {
            return formatMessage(message, placeholders);
        }
        return "";
    }

    private String formatMessage(String message, String... placeholders) {
        if (placeholders != null && placeholders.length > 0) {
            for (int i = 0; i < placeholders.length; i++) {
                message = message.replace("{" + i + "}", placeholders[i]);
            }
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    public void reapplyBoosts() {
        Map<String, Outpost> outposts = outpostDAO.loadOutposts();
        for (Outpost outpost : outposts.values()) {
            if (outpost.getBoost() > 0) {
                Gang gang = moltenGangs.getGangStorage().getGang(outpost.getCapturingIsland());
                if (gang != null) {
                    applyBoost(gang, outpost.getType());
                    for (UUID memberId : gang.getMembers().keySet()) {
                        capturedByIslands.put(outpost.getName(), memberId);
                    }
                }
            }
        }
    }

    private void loadCapturedOutpostsAndApplyBoosts() {
        Map<String, Outpost> capturedOutposts = outpostDAO.loadCapturedOutposts();
        for (Outpost outpost : capturedOutposts.values()) {
            String gangName = outpost.getCapturingIsland();
            Gang gang = moltenGangs.getGangStorage().getGang(gangName);
            if (gang != null) {
                for (UUID memberId : gang.getMembers().keySet()) {
                    capturedByIslands.put(outpost.getName(), memberId);
                }
                applyBoost(gang, outpost.getType());
            }
        }
    }


    @EventHandler
    public void onEdPrisonAddMultiplierCurrency(EdPrisonAddMultiplierCurrency event) {
        UUID playerUUID = event.getUUID();
        Player player = Bukkit.getPlayer(playerUUID);

        if (player == null || !player.isOnline()) {
            return;
        }

        Gang playerGang = moltenGangs.getGangStorage().getGang(playerUUID);
        if (playerGang == null) {
            return;
        }

        if (islandBoosts.containsKey(playerUUID)) {
            double boost = islandBoosts.get(playerUUID);
            String currencyType = event.getCurrency();
            String storedCurrencyType = gangCurrencyTypes.get(playerUUID);

            if (currencyType.equalsIgnoreCase(storedCurrencyType)) {
                event.addMultiplier(boost);
                Bukkit.getLogger().info("Applied boost to " + currencyType + " for player " + player.getName() +
                        ". New multiplier: " + event.getMultiplier());
            }
        }
    }
}
