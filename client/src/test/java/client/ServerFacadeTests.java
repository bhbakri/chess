package client;

import org.junit.jupiter.api.*;
import server.Server;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
public class ServerFacadeTests {

    private static Server server;
    private static int port;
    private ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void setup() throws Exception {
        facade = new ServerFacade(port);
        facade.clearDb();
    }

    @Test
    @DisplayName("01. register — positive")
    void registerPositive() throws Exception {
        var auth = facade.register("alice", "pw", "a@a");
        assertNotNull(auth);
        assertEquals("alice", auth.username());
        assertNotNull(auth.authToken());
    }

    @Test
    @DisplayName("02. register — negative (duplicate username)")
    void registerDuplicateNegative() throws Exception {
        facade.register("dupe", "pw", "d@d");
        assertThrows(Exception.class,
                () -> facade.register("dupe", "pw", "d@d"));
    }

    @Test
    @DisplayName("03. login — positive")
    void loginPositive() throws Exception {
        facade.register("bob", "pw", "b@b");
        facade = new ServerFacade(port);
        var auth = facade.login("bob", "pw");
        assertNotNull(auth);
        assertEquals("bob", auth.username());
        assertNotNull(auth.authToken());
    }

    @Test
    @DisplayName("04. login — negative (bad password)")
    void loginBadPasswordNegative() throws Exception {
        facade.register("carol", "pw", "c@c");
        facade = new ServerFacade(port);
        assertThrows(Exception.class,
                () -> facade.login("carol", "wrong"));
    }

    @Test
    @DisplayName("05. logout — positive")
    void logoutPositive() throws Exception {
        facade.register("dave", "pw", "d@d");
        assertDoesNotThrow(() -> facade.logout());
        assertThrows(Exception.class,
                () -> facade.listGames());
    }

    @Test
    @DisplayName("06. logout — negative (no active session)")
    void logoutNoSessionNegative() {
        assertThrows(Exception.class,
                () -> facade.logout());
    }

    @Test
    @DisplayName("07. createGame — positive")
    void createGamePositive() throws Exception {
        facade.register("eve", "pw", "e@e");
        int id = facade.createGame("g1");
        assertTrue(id > 0);
        List<ServerFacade.GameInfo> games = facade.listGames();
        assertTrue(games.stream().anyMatch(g -> g.gameID().equals(id)));
    }

    @Test
    @DisplayName("08. createGame — negative (unauthorized)")
    void createGameUnauthorizedNegative() {
        assertThrows(Exception.class,
                () -> facade.createGame("noauth"));
    }

    @Test
    @DisplayName("09. listGames — positive")
    void listGamesPositive() throws Exception {
        facade.register("fred", "pw", "f@f");
        facade.createGame("a");
        facade.createGame("b");
        var games = facade.listGames();
        assertEquals(2, games.size());
    }

    @Test
    @DisplayName("10. listGames — negative (unauthorized)")
    void listGamesUnauthorizedNegative() {
        assertThrows(Exception.class,
                () -> facade.listGames());
    }

    @Test
    @DisplayName("11. joinGame — positive")
    void joinGamePositive() throws Exception {
        facade.register("player", "pw", "p@p");
        int id = facade.createGame("joinable");
        facade.joinGame(id, "WHITE");
        var games = facade.listGames();
        var game = games.stream()
                .filter(g -> g.gameID().equals(id))
                .findFirst()
                .orElseThrow();
        assertEquals("player", game.whiteUsername());
    }

    @Test
    @DisplayName("12. joinGame — negative (bad game id)")
    void joinGameBadIdNegative() throws Exception {
        facade.register("x", "pw", "x@x");
        assertThrows(Exception.class,
                () -> facade.joinGame(999999, "WHITE"));
    }

    @Test
    @DisplayName("13. clearDb — positive")
    void clearDbPositive() throws Exception {
        facade.register("temp", "pw", "t@t");
        facade.createGame("tempGame");
        facade.clearDb();
        facade = new ServerFacade(port);
        var auth = facade.register("temp", "pw", "t@t");
        assertNotNull(auth);
    }

    @Test
    @DisplayName("14. clearDb — negative (already empty)")
    void clearDbEmptyNegative() {
        assertDoesNotThrow(() -> facade.clearDb());
    }
}
