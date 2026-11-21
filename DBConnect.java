import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.File;

public class DBConnect {

    // Resolves the correct path to jupiter.db whether running from root or Website directory
    public static String getDbUrl() {
        if (new File("Website/jupiter.db").exists() || new File("Website").isDirectory()) {
            return "jdbc:sqlite:Website/jupiter.db";
        }
        return "jdbc:sqlite:jupiter.db";
    }

    // Reusable connection method for JupiterServer or other classes
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC Driver not found! Make sure sqlite-jdbc.jar is added to the classpath.", e);
        }
        return DriverManager.getConnection(getDbUrl());
    }

    public static void main(String[] args) {
        try {
            // Explicitly load the SQLite driver class
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ ERROR: SQLite JDBC Driver not found in classpath!");
            System.err.println("👉 Make sure to run with: java -cp \".;sqlite-jdbc.jar\" DBConnect");
            return;
        }

        String url = getDbUrl();
        System.out.println("Connecting to: " + url);

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                System.out.println("✅ Connected to SQLite database successfully!");
            }
        } catch (SQLException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }
}
