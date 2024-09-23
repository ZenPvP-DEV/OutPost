package bgprotobg.net.outpost.events;

import bgprotobg.net.outpost.OutPost;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WandListener implements Listener {

    private final OutPost plugin;
    private final Map<UUID, Location[]> playerSelections;

    public WandListener(OutPost plugin, Map<UUID, Location[]> playerSelections) {
        this.plugin = plugin;
        this.playerSelections = playerSelections;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.STICK && item.getItemMeta() != null &&
                "§e§lOutpost Creator".equals(item.getItemMeta().getDisplayName())) {

            event.setCancelled(true);

            if (event.getAction().toString().contains("LEFT_CLICK")) {
                Location pos1 = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : null;
                if (pos1 != null) {
                    playerSelections.putIfAbsent(player.getUniqueId(), new Location[2]);
                    playerSelections.get(player.getUniqueId())[0] = pos1;
                    player.sendMessage(ChatColor.GREEN + "First position set.");
                }

            } else if (event.getAction().toString().contains("RIGHT_CLICK")) {
                Location pos2 = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : null;
                if (pos2 != null) {
                    playerSelections.putIfAbsent(player.getUniqueId(), new Location[2]);
                    playerSelections.get(player.getUniqueId())[1] = pos2;
                    player.sendMessage(ChatColor.GREEN + "Second position set.");

                    Location[] positions = playerSelections.get(player.getUniqueId());
                    if (positions[0] != null && positions[1] != null) {
                        player.sendMessage(ChatColor.GOLD + "You have selected both positions. Type /outpost confirm <name> to create the outpost or /outpost cancel to cancel.");
                    }
                }
            }
        }
    }
}
