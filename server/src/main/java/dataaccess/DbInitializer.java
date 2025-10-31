package dataaccess;

public final class DbInitializer {
    private DbInitializer() {}

    public static void init() throws Exception {
        DatabaseManager.createDatabase();

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS user (
                    username VARCHAR(50) PRIMARY KEY,
                    passwordHash VARCHAR(255) NOT NULL,
                    email VARCHAR(120),
                    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS auth (
                    token CHAR(36) PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (username) REFERENCES user(username) ON DELETE CASCADE
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS game (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    whiteUsername VARCHAR(50),
                    blackUsername VARCHAR(50),
                    gameJson MEDIUMTEXT NOT NULL,
                    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (whiteUsername) REFERENCES user(username) ON DELETE SET NULL,
                    FOREIGN KEY (blackUsername) REFERENCES user(username) ON DELETE SET NULL
                )
            """);
        }
    }
}
