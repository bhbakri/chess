package dataaccess;

import java.sql.*;

public class DatabaseManager {
    private final String url;

    public DatabaseManager(String filePath) {
        this.url = "jdbc:sqlite:" + filePath;
    }

    public Connection getConnection() throws SQLException {
        //kinda confused here but i think itll work
        var conn = DriverManager.getConnection(url);
        conn.setAutoCommit(false);
        return conn;
    }

    public void migrate() throws DataAccessException {
        try (var conn = getConnection()) {
            try (var stmt = conn.createStatement()) {
                // users
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users(
                      username TEXT PRIMARY KEY,
                      password TEXT NOT NULL,
                      email    TEXT NOT NULL
                    );
                """);
                // auth
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS auth(
                      authToken TEXT PRIMARY KEY,
                      username  TEXT NOT NULL,
                      FOREIGN KEY(username) REFERENCES users(username) ON DELETE CASCADE
                    );
                """);
                // games
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS games(
                      gameID         INTEGER PRIMARY KEY AUTOINCREMENT,
                      whiteUsername  TEXT,
                      blackUsername  TEXT,
                      gameName       TEXT NOT NULL,
                      gameJson       TEXT NOT NULL,
                      FOREIGN KEY(whiteUsername) REFERENCES users(username),
                      FOREIGN KEY(blackUsername) REFERENCES users(username)
                    );
                """);
            }
            conn.commit();
        } catch (Exception e) {
            throw new DataAccessException("migration failed: " + e.getMessage());
        }
    }
}
