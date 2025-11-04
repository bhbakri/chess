package dataaccess;

import chess.ChessGame;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
public class MySqlDataAccessTests {

    static DataAccess dao;

    @BeforeAll
    static void init() throws Exception {
        DbInitializer.init();
        dao = new MySqlDataAccess();
    }

    @BeforeEach
    void reset() throws Exception {
        dao.clear();
    }

    @Test @DisplayName("01. clear() — positive")
    void clearPositive() throws Exception {
        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("x", hash, "x@x"));
        assertNotNull(dao.getUser("x"));
        dao.clear();
        assertNull(dao.getUser("x"));
    }

    //users
    @Test @DisplayName("02. createUser — positive")
    void createUserPositive() throws Exception {
        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("alice", hash, "a@a"));
        var u = dao.getUser("alice");
        assertNotNull(u);
        assertEquals("alice", u.username());
        assertNotEquals("pw", u.passwordHash());
    }

    @Test @DisplayName("03. createUser — negative (duplicate)")
    void createUserDuplicateNegative() throws Exception {
        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("dupe", hash, "d@d"));
        assertThrows(DataAccessException.class,
                () -> dao.createUser(new UserData("dupe", hash, "d@d")));
    }

    @Test @DisplayName("04. getUser — positive")
    void getUserPositive() throws Exception {
        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("carol", hash, "c@c"));
        var u = dao.getUser("carol");
        assertNotNull(u);
        assertEquals("carol", u.username());
    }

    @Test @DisplayName("05. getUser — negative (not found)")
    void getUserNotFoundNegative() throws Exception {
        assertNull(dao.getUser("nope"));
    }

    //auth
    @Test @DisplayName("06. createAuth — positive")
    void createAuthPositive() throws Exception {
        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("alice", hash, "a@a"));
        dao.createAuth(new AuthData("T1", "alice"));
        var a = dao.getAuth("T1");
        assertNotNull(a);
        assertEquals("alice", a.username());
    }

    @Test @DisplayName("07. createAuth — negative (duplicate token)")
    void createAuthDuplicateNegative() throws Exception {
        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("alice", hash, "a@a"));
        dao.createAuth(new AuthData("T2", "alice"));
        assertThrows(DataAccessException.class,
                () -> dao.createAuth(new AuthData("T2", "alice")));
    }

    @Test @DisplayName("08. getAuth — positive")
    void getAuthPositive() throws Exception {
        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("alice", hash, "a@a"));
        dao.createAuth(new AuthData("T3", "alice"));
        assertNotNull(dao.getAuth("T3"));
    }

    @Test @DisplayName("09. getAuth — negative (not found)")
    void getAuthNotFoundNegative() throws Exception {
        assertNull(dao.getAuth("NOPE"));
    }

    @Test @DisplayName("10. deleteAuth — positive")
    void deleteAuthPositive() throws Exception {
        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("alice", hash, "a@a"));
        dao.createAuth(new AuthData("T4", "alice"));
        assertNotNull(dao.getAuth("T4"));
        dao.deleteAuth("T4");
        assertNull(dao.getAuth("T4"));
    }

    @Test @DisplayName("11. deleteAuth — negative (missing token = no-op)")
    void deleteAuthMissingTokenNegative() throws Exception {
        assertDoesNotThrow(() -> dao.deleteAuth("NOPE"));
        assertNull(dao.getAuth("NOPE"));
    }

    //games
    private GameData newGame(String name) {
        return new GameData(0, null, null, name, new ChessGame());
    }

    @Test @DisplayName("12. createGame — positive")
    void createGamePositive() throws Exception {
        int id = dao.createGame(newGame("g1"));
        assertTrue(id > 0);
        var g = dao.getGame(id);
        assertNotNull(g);
        assertEquals("g1", g.gameName());
        assertNull(g.whiteUsername());
        assertNull(g.blackUsername());
        assertNotNull(g.game());
    }

    @Test @DisplayName("13. createGame — negative (null)")
    void createGameNullNegative() {
        assertThrows(DataAccessException.class, () -> dao.createGame(null));
    }

    @Test @DisplayName("14. getGame — positive")
    void getGamePositive() throws Exception {
        int id = dao.createGame(newGame("g2"));
        var g = dao.getGame(id);
        assertNotNull(g);
        assertEquals("g2", g.gameName());
    }

    @Test @DisplayName("15. getGame — negative (not found)")
    void getGameNotFoundNegative() throws Exception {
        assertNull(dao.getGame(999_999));
    }

    @Test @DisplayName("16. listGames — positive (multiple)")
    void listGamesPositiveMultiple() throws Exception {
        dao.createGame(newGame("a"));
        dao.createGame(newGame("b"));
        Collection<GameData> list = dao.listGames();
        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(g -> "a".equals(g.gameName())));
        assertTrue(list.stream().anyMatch(g -> "b".equals(g.gameName())));
    }

    @Test @DisplayName("17. listGames — negative (empty)")
    void listGamesEmptyNegative() throws Exception {
        var list = dao.listGames();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test @DisplayName("18. updateGame — positive")
    void updateGamePositive() throws Exception {
        int id = dao.createGame(newGame("g3"));
        var g = dao.getGame(id);

        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("white", hash, "w@w"));
        dao.createUser(new UserData("black", hash, "b@b"));

        var updated = new GameData(id, "white", "black", "g3", g.game());
        dao.updateGame(updated);

        var after = dao.getGame(id);
        assertEquals("white", after.whiteUsername());
        assertEquals("black", after.blackUsername());
        assertEquals("g3", after.gameName());
    }

    @Test @DisplayName("19. updateGame — negative (missing id)")
    void updateGameMissingIdNegative() {
        var bad = new GameData(999_999, "w", "b", "ghost", new ChessGame());
        assertThrows(DataAccessException.class, () -> dao.updateGame(bad));
    }
}
