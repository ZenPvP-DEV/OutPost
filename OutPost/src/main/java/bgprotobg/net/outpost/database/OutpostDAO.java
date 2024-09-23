package bgprotobg.net.outpost.database;

import bgprotobg.net.outpost.models.Outpost;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class OutpostDAO {
    private final Connection connection;

    public OutpostDAO(Connection connection) {
        this.connection = connection;
        updateSchema();
        createTableIfNotExists();
    }
    public void updateSchema() {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(outposts);");
            boolean capturePercentageExists = false;

            while (rs.next()) {
                if ("capturePercentage".equalsIgnoreCase(rs.getString("name"))) {
                    capturePercentageExists = true;
                    break;
                }
            }
            if (!capturePercentageExists) {
                stmt.execute("ALTER TABLE outposts ADD COLUMN capturePercentage REAL DEFAULT 0.0;");
                System.out.println("Column 'capturePercentage' added to table 'outposts'.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createTableIfNotExists() {
        try {
            String sql = "CREATE TABLE IF NOT EXISTS outposts (" +
                    "name TEXT PRIMARY KEY," +
                    "world TEXT NOT NULL," +
                    "pos1 TEXT NOT NULL," +
                    "pos2 TEXT NOT NULL," +
                    "capturingIsland TEXT," +
                    "type TEXT NOT NULL," +
                    "boost REAL," +
                    "capturePercentage REAL DEFAULT 0.0" +
                    ");";
            connection.createStatement().executeUpdate(sql);
            System.out.println("Table 'outposts' created or already exists.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to create 'outposts' table.");
        }
    }




    public void saveOutpost(Outpost outpost) {
        String checkSql = "SELECT COUNT(*) FROM outposts WHERE name = ?";
        String insertSql = "INSERT INTO outposts (name, world, pos1, pos2, capturingIsland, type, boost, capturePercentage) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String updateSql = "UPDATE outposts SET world = ?, pos1 = ?, pos2 = ?, capturingIsland = ?, type = ?, boost = ?, capturePercentage = ? WHERE name = ?";

        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setString(1, outpost.getName());
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
                    pstmt.setString(1, outpost.getPos1().getWorld().getName());
                    pstmt.setString(2, locationToString(outpost.getPos1()));
                    pstmt.setString(3, locationToString(outpost.getPos2()));
                    pstmt.setString(4, outpost.getCapturingIsland());
                    pstmt.setString(5, outpost.getType());
                    pstmt.setDouble(6, outpost.getBoost());
                    pstmt.setDouble(7, outpost.getCapturePercentage());
                    pstmt.setString(8, outpost.getName());
                    pstmt.executeUpdate();
                }
            } else {
                try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
                    pstmt.setString(1, outpost.getName());
                    pstmt.setString(2, outpost.getPos1().getWorld().getName());
                    pstmt.setString(3, locationToString(outpost.getPos1()));
                    pstmt.setString(4, locationToString(outpost.getPos2()));
                    pstmt.setString(5, outpost.getCapturingIsland());
                    pstmt.setString(6, outpost.getType());
                    pstmt.setDouble(7, outpost.getBoost());
                    pstmt.setDouble(8, outpost.getCapturePercentage());
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




    public Map<String, Outpost> loadOutposts() {
        Map<String, Outpost> outposts = new HashMap<>();
        String sql = "SELECT * FROM outposts";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                String world = rs.getString("world");
                String pos1Str = rs.getString("pos1");
                String pos2Str = rs.getString("pos2");
                String capturingIsland = rs.getString("capturingIsland");
                String type = rs.getString("type");
                double boost = rs.getDouble("boost");
                double capturePercentage = rs.getDouble("capturePercentage");

                Location pos1 = stringToLocation(world, pos1Str);
                Location pos2 = stringToLocation(world, pos2Str);

                Outpost outpost = new Outpost(name, pos1, pos2, capturingIsland, type, boost, capturePercentage);
                outposts.put(name, outpost);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return outposts;
    }



    public void deleteOutpost(String name) {
        String sql = "DELETE FROM outposts WHERE name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String locationToString(Location loc) {
        return loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    private Location stringToLocation(String worldName, String str) {
        String[] parts = str.split(",");
        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        double z = Double.parseDouble(parts[2]);
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }
    public Map<String, Outpost> loadCapturedOutposts() {
        Map<String, Outpost> outposts = new HashMap<>();
        String sql = "SELECT * FROM outposts WHERE capturingIsland IS NOT NULL";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                String world = rs.getString("world");
                String pos1Str = rs.getString("pos1");
                String pos2Str = rs.getString("pos2");
                String capturingIsland = rs.getString("capturingIsland");
                String type = rs.getString("type");
                double boost = rs.getDouble("boost");
                double capturePercentage = rs.getDouble("capturePercentage");

                Location pos1 = stringToLocation(world, pos1Str);
                Location pos2 = stringToLocation(world, pos2Str);

                Outpost outpost = new Outpost(name, pos1, pos2, capturingIsland, type, boost, capturePercentage);
                outposts.put(name, outpost);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return outposts;
    }
}
