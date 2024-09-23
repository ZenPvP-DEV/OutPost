package bgprotobg.net.outpost.models;

import org.bukkit.Location;

public class Outpost {
    private final String name;
    private final Location pos1;
    private final Location pos2;
    private String capturingIsland;
    private final String type;
    private double boost;
    private double capturePercentage;


    public Outpost(String name, Location pos1, Location pos2, String type) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.type = type;
        this.capturePercentage = 0.0;
    }

    public Outpost(String name, Location pos1, Location pos2, String capturingIsland, String type, double boost, double capturePercentage) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.capturingIsland = capturingIsland;
        this.type = type;
        this.boost = boost;
        this.capturePercentage = capturePercentage;
    }
    public double getBoost() {
        return boost;
    }

    public void setBoost(double boost) {
        this.boost = boost;
    }

    public String getName() {
        return name;
    }

    public Location getPos1() {
        return pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public String getCapturingIsland() {
        return capturingIsland;
    }

    public void setCapturingIsland(String capturingIsland) {
        this.capturingIsland = capturingIsland;
    }

    public String getType() {
        return type;
    }
    public double getCapturePercentage() {
        return capturePercentage;
    }

    public void setCapturePercentage(double capturePercentage) {
        this.capturePercentage = capturePercentage;
    }
}
