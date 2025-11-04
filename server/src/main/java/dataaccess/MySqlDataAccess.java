package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

public class MySqlDataAccess implements DataAccess {

    private static final Gson GSON = new Gson();

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

    // users
    @Override
    public void createUser(UserData user) throws DataAccessException {
        final String sql = "INSERT INTO user (username, passwordHash, email) VALUES (?,?,?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.username());
            ps.setString(2, user.passwordHash());
            ps.setString(3, user.email());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("createUser failed", e);
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        final String sql = "SELECT username, passwordHash, email FROM user WHERE username=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new UserData(
                        rs.getString("username"),
                        rs.getString("passwordHash"),
                        rs.getString("email")
                );
            }
        } catch (Exception e) {
            throw new DataAccessException("getUser failed", e);
        }
    }

    // auth
    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        final String sql = "INSERT INTO auth (token, username) VALUES (?,?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, auth.authToken());
            ps.setString(2, auth.username());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("createAuth failed", e);
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        final String sql = "SELECT token, username FROM auth WHERE token=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, authToken);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new AuthData(
                        rs.getString("token"),
                        rs.getString("username")
                );
            }
        } catch (Exception e) {
            throw new DataAccessException("getAuth failed", e);
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        final String sql = "DELETE FROM auth WHERE token=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, authToken);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("deleteAuth failed", e);
        }
    }

    // games
    @Override
    public int createGame(GameData game) throws DataAccessException {
        final String sql = "INSERT INTO game (name, whiteUsername, blackUsername, gameJson) VALUES (?,?,?,?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // serializing
            String json = GSON.toJson(game.game()); // game.game() returns ChessGame

            ps.setString(1, game.gameName());
            ps.setString(2, game.whiteUsername());
            ps.setString(3, game.blackUsername());
            ps.setString(4, json);
            ps.executeUpdate();

            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DataAccessException("createGame: no generated key");
        } catch (Exception e) {
            throw new DataAccessException("createGame failed", e);
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        final String sql = "SELECT id, name, whiteUsername, blackUsername, gameJson FROM game WHERE id=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameID);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                ChessGame cg = GSON.fromJson(rs.getString("gameJson"), ChessGame.class);

                // (id, white, black, name, game)
                return new GameData(
                        rs.getInt("id"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("name"),
                        cg
                );
            }
        } catch (Exception e) {
            throw new DataAccessException("getGame failed", e);
        }
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        final String sql = "SELECT id, name, whiteUsername, blackUsername, gameJson FROM game ORDER BY id";
        var out = new ArrayList<GameData>();
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                ChessGame cg = GSON.fromJson(rs.getString("gameJson"), ChessGame.class);
                // (id, white, black, name, game)
                out.add(new GameData(
                        rs.getInt("id"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("name"),
                        cg
                ));
            }
            return out;
        } catch (Exception e) {
            throw new DataAccessException("listGames failed", e);
        }
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        final String sql = """
            UPDATE game
               SET name=?,
                   whiteUsername=?,
                   blackUsername=?,
                   gameJson=?
             WHERE id=?
        """;
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {

            // serializing the gamechess
            String json = GSON.toJson(game.game());

            ps.setString(1, game.gameName());
            ps.setString(2, game.whiteUsername());
            ps.setString(3, game.blackUsername());
            ps.setString(4, json);
            ps.setInt(5, game.gameID());

            if (ps.executeUpdate() == 0) {
                throw new DataAccessException("updateGame: game not found id=" + game.gameID());
            }
        } catch (Exception e) {
            throw new DataAccessException("updateGame failed", e);
        }
    }
}
