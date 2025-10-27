package service;
import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.*;
import server.Server;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {

    private DataAccess dao;
    private UserService users;
    private GameService games;

    private String authAlice;
    private String authBob;

    @BeforeEach
    void setup() throws Exception {
        dao = new MemoryDataAccess();
        users = new UserService(dao);
        games = new GameService(dao);

        authAlice = users.register(new Server.RegisterRequest("alice", "pw", "a@a")).authToken();
        authBob   = users.register(new Server.RegisterRequest("bob", "pw", "b@b")).authToken();
    }

    // list
    @Test
    void listPositive() throws Exception {
        var list = games.list(authAlice);
        assertNotNull(list.games());
        assertTrue(list.games().isEmpty());
    }

    @Test
    void listNegativeUnauthorized() {
        assertThrows(SecurityException.class, () -> games.list("badtoken"));
    }

    // create
    @Test
    void createPositive() throws Exception {
        var res = games.create(authAlice, new Server.CreateGameRequest("g1"));
        assertTrue(res.gameID() > 0);
    }

    @Test
    void createNegativeBadRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> games.create(authAlice, new Server.CreateGameRequest("")));
    }

    // join
    @Test
    void joinPositive() throws Exception {
        var g = games.create(authAlice, new Server.CreateGameRequest("g1"));
        assertDoesNotThrow(() -> games.join(authAlice, new Server.JoinGameRequest("WHITE", g.gameID())));
        var list = games.list(authAlice);
        assertEquals(1, list.games().size());
        assertEquals("alice", list.games().get(0).whiteUsername());
        assertNull(list.games().get(0).blackUsername());
    }

    @Test
    void joinNegativeColorTaken() throws Exception {
        var g = games.create(authAlice, new Server.CreateGameRequest("g1"));
        games.join(authAlice, new Server.JoinGameRequest("BLACK", g.gameID()));
        assertThrows(SecurityException.class,
                () -> games.join(authBob, new Server.JoinGameRequest("BLACK", g.gameID())));
    }

    @Test
    void joinNegativeBadColor() throws Exception {
        var g = games.create(authAlice, new Server.CreateGameRequest("g1"));
        assertThrows(IllegalArgumentException.class,
                () -> games.join(authAlice, new Server.JoinGameRequest("GREEN", g.gameID())));
    }

    @Test
    void joinNegativeBadGameId() {
        assertThrows(IllegalArgumentException.class,
                () -> games.join(authAlice, new Server.JoinGameRequest("WHITE", null)));
    }

    @Test
    void joinNegativeUnauthorized() throws Exception {
        var g = games.create(authAlice, new Server.CreateGameRequest("g1"));
        assertThrows(SecurityException.class,
                () -> games.join("badtoken", new Server.JoinGameRequest("WHITE", g.gameID())));
    }
}
