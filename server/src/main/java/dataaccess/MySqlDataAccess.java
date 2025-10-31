package dataaccess;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySqlDataAccess implements DataAccess {

    @Override
    public void clear() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var st = conn.createStatement()) {
            st.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
            st.executeUpdate("TRUNCATE TABLE auth");
            st.executeUpdate("TRUNCATE TABLE game");
            st.executeUpdate("TRUNCATE TABLE user");
            st.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
        } catch (Exception e) {
            throw new DataAccessException("clear failed", e);
        }
    }

    //users
    @Override
    public void insertUser(String username, String passwordHash, String email) throws DataAccessException {
        final String sql = "INSERT INTO user (username, passwordHash, email) VALUES (?,?,?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("insertUser failed", e);
        }
    }

    @Override
    public UserData findUser(String username) throws DataAccessException {
        final String sql = "SELECT username, passwordHash, email FROM user WHERE username=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new UserData(
                        rs.getString("username"),
                        rs.getString("passwordHash"),
                        rs.getString("email")
                );
            }
        } catch (SQLException e) {
            throw new DataAccessException("findUser failed", e);
        }
    }

    //auth
    @Override
    public void insertAuth(String token, String username) throws DataAccessException {
        final String sql = "INSERT INTO auth (token, username) VALUES (?,?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("insertAuth failed", e);
        }
    }

    @Override
    public AuthData findAuth(String token) throws DataAccessException {
        final String sql = "SELECT token, username FROM auth WHERE token=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new AuthData(rs.getString("token"), rs.getString("username"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("findAuth failed", e);
        }
    }

    @Override
    public void deleteAuth(String token) throws DataAccessException {
        final String sql = "DELETE FROM auth WHERE token=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("deleteAuth failed", e);
        }
    }


    //games
    @Override
    public int insertGame(String name, String whiteUsername, String blackUsername, String gameJson) throws DataAccessException {
        final String sql = "INSERT INTO game (name, whiteUsername, blackUsername, gameJson) VALUES (?,?,?,?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, whiteUsername);
            ps.setString(3, blackUsername);
            ps.setString(4, gameJson);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new DataAccessException("no generated key");
            }
        } catch (SQLException e) {
            throw new DataAccessException("insertGame failed", e);
        }
    }

    @Override
    public GameData findGame(int id) throws DataAccessException {
        final String sql = "SELECT id,name,whiteUsername,blackUsername,gameJson FROM game WHERE id=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new GameData(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameJson")
                );
            }
        } catch (SQLException e) {
            throw new DataAccessException("findGame failed", e);
        }
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {
        final String sql = "SELECT id,name,whiteUsername,blackUsername,gameJson FROM game ORDER BY id";
        var out = new ArrayList<GameData>();
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new GameData(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameJson")
                ));
            }
            return out;
        } catch (SQLException e) {
            throw new DataAccessException("listGames failed", e);
        }
    }

    @Override
    public void updateGamePlayers(int id, String whiteUsername, String blackUsername) throws DataAccessException {
        final String sql = """
            UPDATE game
               SET whiteUsername=?, blackUsername=?
             WHERE id=?
        """;
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, whiteUsername);
            ps.setString(2, blackUsername);
            ps.setInt(3, id);
            if (ps.executeUpdate() == 0) throw new DataAccessException("game not found: " + id);
        } catch (SQLException e) {
            throw new DataAccessException("updateGamePlayers failed", e);
        }
    }

    @Override
    public void updateGameState(int id, String gameJson) throws DataAccessException {
        final String sql = "UPDATE game SET gameJson=? WHERE id=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, gameJson);
            ps.setInt(2, id);
            if (ps.executeUpdate() == 0) throw new DataAccessException("game not found: " + id);
        } catch (SQLException e) {
            throw new DataAccessException("updateGameState failed", e);
        }
    }

    public record UserData(String username, String passwordHash, String email) {}
    public record AuthData(String token, String username) {}
    public record GameData(Integer id, String name, String whiteUsername, String blackUsername, String gameJson) {}
}
