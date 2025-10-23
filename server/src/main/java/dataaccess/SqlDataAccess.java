package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

public class SqlDataAccess implements DataAccess {

    private final DatabaseManager db;
    private final Gson gson = new Gson();

    public SqlDataAccess(DatabaseManager db) {
        this.db = db;
    }

    //system
    @Override
    public void clear() throws DataAccessException {
        try (var conn = db.getConnection();
             var s1 = conn.createStatement();
             var s2 = conn.createStatement();
             var s3 = conn.createStatement()) {
            s1.executeUpdate("DELETE FROM auth;");
            s2.executeUpdate("DELETE FROM games;");
            s3.executeUpdate("DELETE FROM users;");
            conn.commit();
        } catch (Exception e) {
            throw new DataAccessException("clear failed: " + e.getMessage());
        }
    }

    //users
    @Override
    public void createUser(UserData u) throws DataAccessException {
        if (u == null || u.username() == null) {
            throw new DataAccessException("null user");
        }
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("INSERT INTO users(username,password,email) VALUES(?,?,?)")) {
            ps.setString(1, u.username());
            ps.setString(2, u.password());
            ps.setString(3, u.email());
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw new DataAccessException("createUser: " + e.getMessage());
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        if (username == null) {
            return null;
        }
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("SELECT username,password,email FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserData(rs.getString(1), rs.getString(2), rs.getString(3));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("getUser: " + e.getMessage());
        }
    }

    //auth
    @Override
    public void createAuth(AuthData a) throws DataAccessException {
        if (a == null || a.authToken() == null) {
            throw new DataAccessException("null auth");
        }
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("INSERT INTO auth(authToken,username) VALUES(?,?)")) {
            ps.setString(1, a.authToken());
            ps.setString(2, a.username());
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw new DataAccessException("createAuth: " + e.getMessage());
        }
    }

    @Override
    public AuthData getAuth(String token) throws DataAccessException {
        if (token == null) {
            return null;
        }
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("SELECT authToken,username FROM auth WHERE authToken=?")) {
            ps.setString(1, token);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? new AuthData(rs.getString(1), rs.getString(2)) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("getAuth: " + e.getMessage());
        }
    }

    @Override
    public void deleteAuth(String token) throws DataAccessException {
        if (token == null) {
            return;
        }
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("DELETE FROM auth WHERE authToken=?")) {
            ps.setString(1, token);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw new DataAccessException("deleteAuth: " + e.getMessage());
        }
    }

    // games
    @Override
    public int createGame(GameData g) throws DataAccessException {
        if (g == null) {
            throw new DataAccessException("null game");
        }
        String json = gson.toJson(g.game() == null ? new ChessGame() : g.game());

        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("""
                        INSERT INTO games(whiteUsername,blackUsername,gameName,gameJson)
                        VALUES(?,?,?,?)
                     """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, g.whiteUsername());
            ps.setString(2, g.blackUsername());
            ps.setString(3, g.gameName());
            ps.setString(4, json);
            ps.executeUpdate();

            try (var keys = ps.getGeneratedKeys()) {
                int id = (keys.next()) ? keys.getInt(1) : -1;
                conn.commit();
                return id;
            }
        } catch (SQLException e) {
            throw new DataAccessException("createGame: " + e.getMessage());
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("""
                         SELECT gameID, whiteUsername, blackUsername, gameName, gameJson
                         FROM games WHERE gameID=?
                     """)) {
            ps.setInt(1, gameID);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                var game = gson.fromJson(rs.getString("gameJson"), ChessGame.class);
                return new GameData(
                        rs.getInt("gameID"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameName"),
                        game
                );
            }
        } catch (SQLException e) {
            throw new DataAccessException("getGame: " + e.getMessage());
        }
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        var out = new ArrayList<GameData>();
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("""
                         SELECT gameID, whiteUsername, blackUsername, gameName, gameJson
                         FROM games ORDER BY gameID
                     """);
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                var game = gson.fromJson(rs.getString("gameJson"), ChessGame.class);
                out.add(new GameData(
                        rs.getInt("gameID"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameName"),
                        game
                ));
            }
            return out;
        } catch (SQLException e) {
            throw new DataAccessException("listGames: " + e.getMessage());
        }
    }

    @Override
    public void updateGame(GameData g) throws DataAccessException {
        if (g == null) {
            throw new DataAccessException("null game");
        }
        String json = gson.toJson(g.game());
        try (var conn = db.getConnection();
             var ps = conn.prepareStatement("""
                         UPDATE games
                         SET whiteUsername=?, blackUsername=?, gameName=?, gameJson=?
                         WHERE gameID=?
                     """)) {
            ps.setString(1, g.whiteUsername());
            ps.setString(2, g.blackUsername());
            ps.setString(3, g.gameName());
            ps.setString(4, json);
            ps.setInt(5, g.gameID());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DataAccessException("no such game: " + g.gameID());
            }
            conn.commit();
        } catch (SQLException e) {
            throw new DataAccessException("updateGame: " + e.getMessage());
        }
    }
}
