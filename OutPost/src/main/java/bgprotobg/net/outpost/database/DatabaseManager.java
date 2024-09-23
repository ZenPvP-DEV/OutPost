package bgprotobg.net.outpost.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private Connection connection;

    public void connect(File dataFolder) {
        try {
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "outposts.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void createTableIfNotExists() {
        try {
            String sql = "CREATE TABLE IF NOT EXISTS outposts (" +
                    "name TEXT PRIMARY KEY," +
                    "world TEXT," +
                    "x1 DOUBLE," +
                    "y1 DOUBLE," +
                    "z1 DOUBLE," +
                    "x2 DOUBLE," +
                    "y2 DOUBLE," +
                    "z2 DOUBLE," +
                    "capturingGang TEXT," +
                    "type TEXT" +
                    ");";
            connection.createStatement().executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
