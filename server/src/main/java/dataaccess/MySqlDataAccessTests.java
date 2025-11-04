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
        DbInitializer.init();          // make sure DB and tables exist
        dao = new MySqlDataAccess();   // test the MySQL implementation
    }

    @BeforeEach
    void reset() throws Exception {
        dao.clear();                   // each test starts from a clean DB
    }

    @Test
    @DisplayName("01. clear() — positive")
    void clear_positive() throws Exception {
        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("x", hash, "x@x"));
        assertNotNull(dao.getUser("x"));
        dao.clear();
        assertNull(dao.getUser("x"));
    }

    //users
    @Test
    @DisplayName("02. createUser — positive")
    void createUser_positive() throws Exception {
        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("alice", hash, "a@a"));
        var u = dao.getUser("alice");
        assertNotNull(u);
        assertEquals("alice", u.username());
        assertNotEquals("pw", u.passwordHash()); // ensure hash stored
    }

    @Test
    @DisplayName("03. createUser — negative (duplicate)")
    void createUser_duplicate_negative() throws Exception {
        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("dupe", hash, "d@d"));
        assertThrows(DataAccessException.class,
                () -> dao.createUser(new UserData("dupe", hash, "d@d")));
    }

    @Test
    @DisplayName("04. getUser — positive")
    void getUser_positive() throws Exception {
        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("carol", hash, "c@c"));
        var u = dao.getUser("carol");
        assertNotNull(u);
        assertEquals("carol", u.username());
    }

    @Test
    @DisplayName("05. getUser — negative (not found)")
    void getUser_notFound_negative() throws Exception {
        assertNull(dao.getUser("nope"));
    }

    //auth
    @Test
    @DisplayName("06. createAuth — positive")
    void createAuth_positive() throws Exception {
        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("alice", hash, "a@a"));
        dao.createAuth(new AuthData("T1", "alice"));
        var a = dao.getAuth("T1");
        assertNotNull(a);
        assertEquals("alice", a.username());
    }

    @Test
    @DisplayName("07. createAuth — negative (duplicate token)")
    void createAuth_duplicate_negative() throws Exception {
        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("alice", hash, "a@a"));
        dao.createAuth(new AuthData("T2", "alice"));
        assertThrows(DataAccessException.class,
                () -> dao.createAuth(new AuthData("T2", "alice")));
    }

    @Test
    @DisplayName("08. getAuth — positive")
    void getAuth_positive() throws Exception {
        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("alice", hash, "a@a"));
        dao.createAuth(new AuthData("T3", "alice"));
        assertNotNull(dao.getAuth("T3"));
    }

    @Test
    @DisplayName("09. getAuth — negative (not found)")
    void getAuth_notFound_negative() throws Exception {
        assertNull(dao.getAuth("NOPE"));
    }

    @Test
    @DisplayName("10. deleteAuth — positive")
    void deleteAuth_positive() throws Exception {
        var hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        dao.createUser(new UserData("alice", hash, "a@a"));
        dao.createAuth(new AuthData("T4", "alice"));
        assertNotNull(dao.getAuth("T4"));
        dao.deleteAuth("T4");
        assertNull(dao.getAuth("T4"));
    }

    @Test
    @DisplayName("11. deleteAuth — negative (missing token = no-op)")
    void deleteAuth_missingToken_negative() throws Exception {
        assertDoesNotThrow(() -> dao.deleteAuth("NOPE"));
        assertNull(dao.getAuth("NOPE"));
    }

    //games
    private GameData newGame(String name) {
        return new GameData(0, null, null, name, new chess.ChessGame());
    }

    @Test
    @DisplayName("12. createGame — positive")
    void createGame_positive() throws Exception {
        int id = dao.createGame(newGame("g1"));
        assertTrue(id > 0);
        var g = dao.getGame(id);
        assertNotNull(g);
        assertEquals("g1", g.gameName());
        assertNull(g.whiteUsername());
        assertNull(g.blackUsername());
        assertNotNull(g.game());
    }

    @Test
    @DisplayName("13. createGame — negative (null)")
    void createGame_null_negative() {
        assertThrows(DataAccessException.class, () -> dao.createGame(null));
    }

    @Test
    @DisplayName("14. getGame — positive")
    void getGame_positive() throws Exception {
        int id = dao.createGame(newGame("g2"));
        var g = dao.getGame(id);
        assertNotNull(g);
        assertEquals("g2", g.gameName());
    }

    @Test
    @DisplayName("15. getGame — negative (not found)")
    void getGame_notFound_negative() throws Exception {
        assertNull(dao.getGame(999_999));
    }

    @Test
    @DisplayName("16. listGames — positive (multiple)")
    void listGames_positive_multiple() throws Exception {
        dao.createGame(newGame("a"));
        dao.createGame(newGame("b"));
        Collection<GameData> list = dao.listGames();
        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(g -> "a".equals(g.gameName())));
        assertTrue(list.stream().anyMatch(g -> "b".equals(g.gameName())));
    }

    @Test
    @DisplayName("17. listGames — negative (empty)")
    void listGames_empty_negative() throws Exception {
        var list = dao.listGames();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("18. updateGame — positive")
    void updateGame_positive() throws Exception {
        int id = dao.createGame(newGame("g3"));
        var g = dao.getGame(id);

        // ✅ add these lines: create users that will be referenced by FK
        var hash = org.mindrot.jbcrypt.BCrypt.hashpw("pw", org.mindrot.jbcrypt.BCrypt.gensalt());
        dao.createUser(new model.UserData("white", hash, "w@w"));
        dao.createUser(new model.UserData("black", hash, "b@b"));

        var updated = new model.GameData(id, "white", "black", "g3", g.game());
        dao.updateGame(updated);

        var after = dao.getGame(id);
        org.junit.jupiter.api.Assertions.assertEquals("white", after.whiteUsername());
        org.junit.jupiter.api.Assertions.assertEquals("black", after.blackUsername());
        org.junit.jupiter.api.Assertions.assertEquals("g3", after.gameName());
    }


    @Test
    @DisplayName("19. updateGame — negative (missing id)")
    void updateGame_missingId_negative() {
        var bad = new GameData(999_999, "w", "b", "ghost", new ChessGame());
        assertThrows(DataAccessException.class, () -> dao.updateGame(bad));
    }
}
