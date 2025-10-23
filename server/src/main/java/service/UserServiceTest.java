package service;

import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.*;
import server.Server;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    private DataAccess dao;
    private UserService users;

    @BeforeEach
    void setup() {
        dao = new MemoryDataAccess();
        users = new UserService(dao);
    }

    @Test
    void registerPositive() throws Exception {
        var res = users.register(new Server.RegisterRequest("alice", "pw", "a@a"));
        assertEquals("alice", res.username());
        assertNotNull(res.authToken());
    }

    @Test
    void registerNegativeDuplicate() throws Exception {
        users.register(new Server.RegisterRequest("alice", "pw", "a@a"));
        assertThrows(SecurityException.class,
                () -> users.register(new Server.RegisterRequest("alice", "pw", "a@a")));
    }

    @Test
    void loginPositive() throws Exception {
        users.register(new Server.RegisterRequest("bob", "pw", "b@b"));
        var res = users.login(new Server.LoginRequest("bob", "pw"));
        assertEquals("bob", res.username());
        assertNotNull(res.authToken());
    }

    @Test
    void loginNegativeUnauthorized() throws Exception {
        users.register(new Server.RegisterRequest("bob", "pw", "b@b"));
        assertThrows(SecurityException.class,
                () -> users.login(new Server.LoginRequest("bob", "WRONG")));
    }

    @Test
    void logoutPositive() throws Exception {
        var reg = users.register(new Server.RegisterRequest("carol", "pw", "c@c"));
        assertDoesNotThrow(() -> users.logout(new Server.LogoutRequest(reg.authToken())));
        // same token again should be unauthorized
        assertThrows(SecurityException.class,
                () -> users.logout(new Server.LogoutRequest(reg.authToken())));
    }

    @Test
    void logoutNegativeUnauthorized() {
        assertThrows(SecurityException.class,
                () -> users.logout(new Server.LogoutRequest("no-such-token")));
    }
}
