package bgprotobg.net.outpost;

import bgprotobg.net.outpost.commands.OutpostCommand;
import bgprotobg.net.outpost.database.DatabaseManager;
import bgprotobg.net.outpost.database.OutpostDAO;
import bgprotobg.net.outpost.events.PlayerMovementListener;
import bgprotobg.net.outpost.events.WandListener;
import bgprotobg.net.outpost.models.Outpost;
import bgprotobg.net.outpost.placeholders.OutpostPlaceholder;
import me.lavaturtle.moltengangs.MoltenGangs;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OutPost extends JavaPlugin {

    private OutpostCommand outpostCommand;
    private int captureDuration;
    private double moneyBoost;
    private final HashMap<UUID, Location[]> playerSelections = new HashMap<>();
    private MoltenGangs moltenGangs;
    private final Map<String, Outpost> outposts = new HashMap<>();
    private PlayerMovementListener playerMovementListener;
    private DatabaseManager databaseManager;
    private OutpostDAO outpostDAO;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        databaseManager = new DatabaseManager();
        databaseManager.connect(getDataFolder());
        outpostDAO = new OutpostDAO(databaseManager.getConnection());

        outpostCommand = new OutpostCommand(this, playerSelections, outpostDAO);

        captureDuration = getConfig().getInt("capture-duration-seconds", 30);
        moneyBoost = getConfig().getDouble("money-boost", 0.5);

        if (getServer().getPluginManager().isPluginEnabled("MoltenGangs")) {
            moltenGangs = (MoltenGangs) getServer().getPluginManager().getPlugin("MoltenGangs");
        } else {
            getLogger().warning("MoltenGangs plugin not found! Disabling OutPost plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new OutpostPlaceholder(this).register();
        } else {
            getLogger().warning("Could not find PlaceholderAPI! This plugin requires PlaceholderAPI to function.");
        }

        outpostCommand = new OutpostCommand(this, playerSelections, outpostDAO);
        this.getCommand("outpost").setExecutor(outpostCommand);
        getServer().getPluginManager().registerEvents(new WandListener(this, playerSelections), this);
        getServer().getPluginManager().registerEvents(new PlayerMovementListener(this, moltenGangs, outpostDAO), this);
        playerMovementListener = new PlayerMovementListener(this, moltenGangs, outpostDAO);

        Map<String, Outpost> loadedOutposts = outpostDAO.loadOutposts();
        outpostCommand.getOutposts().putAll(loadedOutposts);
    }

    @Override
    public void onDisable() {
        if (outpostCommand != null && outpostDAO != null) {
            for (Outpost outpost : outpostCommand.getOutposts().values()) {
                try {
                    outpostDAO.saveOutpost(outpost);
                } catch (Exception e) {
                    getLogger().severe("Failed to save outpost: " + outpost.getName());
                    e.printStackTrace();
                }
            }
            Bukkit.getScheduler().cancelTasks(this);
        }

        if (databaseManager != null) {
            try {
                databaseManager.disconnect();
            } catch (Exception e) {
                getLogger().severe("Failed to disconnect the database.");
                e.printStackTrace();
            }
        }
    }


    private void saveOutposts() {
        for (Outpost outpost : outposts.values()) {
            outpostDAO.saveOutpost(outpost);
        }
    }

    public int getCaptureDuration() {
        return captureDuration;
    }

    public double getMoneyBoost() {
        return moneyBoost;
    }

    public double getTokenBoost() {
        return getConfig().getDouble("boosts.token", 0.0);
    }

    public double getGemBoost() {
        return getConfig().getDouble("boosts.gem", 0.0);
    }

    public double getCrystalBoost() {
        return getConfig().getDouble("boosts.exp", 0.0);
    }

    public MoltenGangs getMoltenGangs() {
        return moltenGangs;
    }

    public OutpostCommand getOutpostCommand() {
        return outpostCommand;
    }
}
