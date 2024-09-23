package bgprotobg.net.outpost.placeholders;

import bgprotobg.net.outpost.OutPost;
import bgprotobg.net.outpost.models.Outpost;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class OutpostPlaceholder extends PlaceholderExpansion {

    private final OutPost plugin;

    public OutpostPlaceholder(OutPost plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "outpost";
    }

    @Override
    public String getAuthor() {
        return "YourName";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null || identifier == null) {
            return "";
        }

        if (identifier.endsWith("_percentage")) {
            String outpostKey = mapIdentifierToOutpostKey(identifier.replace("_percentage", ""));
            if (outpostKey != null) {
                Outpost outpost = plugin.getOutpostCommand().getOutposts().get(outpostKey);
                if (outpost != null) {
                    double percentage = outpost.getCapturePercentage();
                    return String.format("%.1f%%", percentage);
                }
            }
            return "0%";
        }

        String outpostKey = mapIdentifierToOutpostKey(identifier);
        if (outpostKey != null) {
            Outpost outpost = plugin.getOutpostCommand().getOutposts().get(outpostKey);
            if (outpost != null && outpost.getCapturingIsland() != null) {
                return outpost.getCapturingIsland();
            }
        }

        return "None";
    }

    private String mapIdentifierToOutpostKey(String identifier) {
        switch (identifier.toLowerCase()) {
            case "money":
                return "MoneyOutpost";
            case "token":
                return "TokensOutpost";
            case "gem":
                return "GemsOutpost";
            case "xp":
                return "CrystalsOutpost";
            default:
                return null;
        }
    }
}
