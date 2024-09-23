package bgprotobg.net.outpost.commands;

import bgprotobg.net.outpost.OutPost;
import bgprotobg.net.outpost.database.OutpostDAO;
import bgprotobg.net.outpost.models.Outpost;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class OutpostCommand implements CommandExecutor, Listener, TabCompleter {

    private final OutPost plugin;
    private final Map<UUID, Location[]> playerSelections;
    public OutpostDAO outpostDAO;
    private final Map<String, Outpost> outposts = new HashMap<>();
    private static final List<String> OUTPOST_TYPES = Arrays.asList("Money", "Tokens", "Gems", "XP");

    public OutpostCommand(OutPost plugin, Map<UUID, Location[]> playerSelections, OutpostDAO outpostDAO) {
        this.plugin = plugin;
        this.playerSelections = playerSelections;
        this.outpostDAO = outpostDAO;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            if (!player.hasPermission("outpost.use")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }
            openOutpostMenu(player);
            return true;
        }
        if (!player.hasPermission("outpost.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wand":
                if (args.length < 2) {
                    player.sendMessage("Usage: /outpost wand <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }

                giveOutpostWand(target);
                player.sendMessage(ChatColor.GREEN + "Outpost wand given to " + target.getName());
                break;

            case "confirm":
                if (args.length < 2) {
                    player.sendMessage("Usage: /outpost confirm <type>");
                    return true;
                }
                String outpostType = args[1];

                if (!OUTPOST_TYPES.contains(outpostType)) {
                    player.sendMessage(ChatColor.RED + "Invalid outpost type. Valid types are: " + String.join(", ", OUTPOST_TYPES));
                    return true;
                }

                Location[] positions = playerSelections.get(player.getUniqueId());
                if (positions == null || positions[0] == null || positions[1] == null) {
                    player.sendMessage(ChatColor.RED + "You need to set both positions with the wand first.");
                    return true;
                }

                String outpostName = outpostType + "Outpost";
                Outpost newOutpost = new Outpost(outpostName, positions[0], positions[1], outpostType);
                outposts.put(outpostName, newOutpost);
                outpostDAO.saveOutpost(newOutpost);

                player.sendMessage(ChatColor.GREEN + "Outpost of type " + outpostType + " has been created.");
                playerSelections.remove(player.getUniqueId());
                break;

            case "cancel":
                if (playerSelections.containsKey(player.getUniqueId())) {
                    playerSelections.remove(player.getUniqueId());
                    player.sendMessage(ChatColor.YELLOW + "Outpost creation has been canceled.");
                } else {
                    player.sendMessage(ChatColor.RED + "You have no pending outpost creation.");
                }
                break;

            case "list":
                if (outposts.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "There are no outposts created.");
                } else {
                    player.sendMessage(ChatColor.GREEN + "Outposts:");
                    for (Outpost outpost : outposts.values()) {
                        player.sendMessage(ChatColor.GOLD + "- " + outpost.getName() + " (" + outpost.getType() + ")");
                    }
                }
                break;

            case "delete":
                if (args.length < 2) {
                    player.sendMessage("Usage: /outpost delete <type>");
                    return true;
                }
                String deleteType = args[1];
                String deleteName = deleteType + "Outpost";
                if (outposts.containsKey(deleteName)) {
                    outposts.remove(deleteName);
                    outpostDAO.deleteOutpost(deleteName);
                    player.sendMessage(ChatColor.GREEN + "Outpost " + deleteName + " has been deleted.");
                } else {
                    player.sendMessage(ChatColor.RED + "Outpost of type " + deleteType + " does not exist.");
                }
                break;

            case "reload":
                plugin.reloadConfig();
                player.sendMessage(ChatColor.GREEN + "Config reloaded.");
                break;

            default:
                player.sendMessage("Unknown subcommand.");
                break;
        }

        return true;
    }

    private void giveOutpostWand(Player player) {
        ItemStack wand = new ItemStack(Material.STICK);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Outpost Creator");
            wand.setItemMeta(meta);
        }
        player.getInventory().addItem(wand);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("wand", "confirm", "cancel", "list", "delete", "reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("confirm")) {
            return OUTPOST_TYPES;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            return OUTPOST_TYPES;
        }
        return Collections.emptyList();
    }

    private void openOutpostMenu(Player player) {
        ConfigurationSection guiConfig = plugin.getConfig().getConfigurationSection("gui");
        String title = ChatColor.translateAlternateColorCodes('&', guiConfig.getString("title", "&2&LOutPost Menu"));
        int rows = guiConfig.getInt("rows", 3);

        Inventory gui = Bukkit.createInventory(null, rows * 9, title);

        Material fillerMaterial = Material.matchMaterial(guiConfig.getString("filler-item", "GRAY_STAINED_GLASS_PANE"));
        if (fillerMaterial == null) fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;

        ItemStack fillerItem = new ItemStack(fillerMaterial);
        ItemMeta fillerMeta = fillerItem.getItemMeta();
        fillerMeta.setDisplayName(" ");
        fillerItem.setItemMeta(fillerMeta);

        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, fillerItem);
        }

        ConfigurationSection outpostsSection = guiConfig.getConfigurationSection("outposts");
        for (String key : outpostsSection.getKeys(false)) {
            ConfigurationSection outpostConfig = outpostsSection.getConfigurationSection(key);

            Material material = Material.matchMaterial(outpostConfig.getString("material", "PAPER"));
            if (material == null) material = Material.PAPER;

            ItemStack outpostItem = new ItemStack(material);
            ItemMeta outpostMeta = outpostItem.getItemMeta();

            String displayName = ChatColor.translateAlternateColorCodes('&', outpostConfig.getString("display-name", "&2&LOutPost"));
            outpostMeta.setDisplayName(displayName);

            Outpost outpost = outposts.get(key);

            List<String> loreList = outpostConfig.getStringList("lore").stream()
                    .map(line -> {
                        line = line.replace("<island name money>", "%outpost_money%")
                                .replace("<island name token>", "%outpost_token%")
                                .replace("<island name gem>", "%outpost_gem%")
                                .replace("<island name xp>", "%outpost_xp%");

                        line = ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, line));

                        if (outpost != null) {
                            String percentage = String.format("%.1f%%", outpost.getCapturePercentage());
                            line = line.replace("<outpost_percentage>", percentage);
                        } else {
                            line = line.replace("<outpost_percentage>", "0%");
                        }

                        return line;
                    })
                    .collect(Collectors.toList());
            if (outpost != null) {
                loreList.add(ChatColor.GRAY + "Percentage: " + ChatColor.YELLOW + String.format("%.1f%%", outpost.getCapturePercentage()));
            }

            outpostMeta.setLore(loreList);
            outpostItem.setItemMeta(outpostMeta);

            int slot = outpostConfig.getInt("slot", 13);
            gui.setItem(slot, outpostItem);
        }

        player.openInventory(gui);
    }





    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getConfigurationSection("gui").getString("title", "&2&LOutPost Menu")))) {
            event.setCancelled(true);
        }
    }

    public Map<String, Outpost> getOutposts() {
        return outposts;
    }
}
