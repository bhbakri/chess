package dataaccess;

import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoryDataAccess implements DataAccess {

    private final Map<String, UserData> users = new ConcurrentHashMap<>();
    private final Map<String, AuthData> auths = new ConcurrentHashMap<>();
    private final Map<Integer, GameData> games = new ConcurrentHashMap<>();
    private final AtomicInteger nextGameId = new AtomicInteger(1);

    @Override
    public void clear() {
        users.clear();
        auths.clear();
        games.clear();
        nextGameId.set(1);
    }

    //user functions

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (user == null || user.username() == null) {
            throw new DataAccessException("null user");
        }
        // Dont remove existent users
        users.putIfAbsent(user.username(), user);
    }

    @Override
    public UserData getUser(String username) {
        if (username == null) {
            return null;
        }
        return users.get(username);
    }

    //authentication
    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        if (auth == null || auth.authToken() == null) {
            throw new DataAccessException("null auth");
        }
        auths.put(auth.authToken(), auth);
    }

    @Override
    public AuthData getAuth(String authToken) {
        if (authToken == null) {
            return null;
        }
        return auths.get(authToken);
    }

    @Override
    public void deleteAuth(String authToken) {
        if (authToken != null) {
            auths.remove(authToken);
        }
    }

    @Override
    public int createGame(GameData game) throws DataAccessException {
        if (game == null) {
            throw new DataAccessException("null game");
        }
        int id = nextGameId.getAndIncrement();
        GameData toStore = new GameData(
                id,
                game.whiteUsername(),
                game.blackUsername(),
                game.gameName(),
                game.game()
        );
        games.put(id, toStore);
        return id;
    }

    @Override
    public GameData getGame(int gameID) {
        return games.get(gameID);
    }

    @Override
    public Collection<GameData> listGames() {
        return Collections.unmodifiableCollection(games.values());
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        if (game == null) {
            throw new DataAccessException("null game");
        }
        if (!games.containsKey(game.gameID())) {
            throw new DataAccessException("no such game: " + game.gameID());
        }
        games.put(game.gameID(), game);
    }
}
