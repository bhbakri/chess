package service;

import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.*;
import server.Server;

import static org.junit.jupiter.api.Assertions.*;

public class ClearServiceTest {

    private DataAccess dao;
    private ClearService clear;
    private UserService users;

    @BeforeEach
    void setup() {
        dao = new MemoryDataAccess();
        clear = new ClearService(dao);
        users = new UserService(dao);
    }

    @Test
    void clear_Positive() throws Exception {
        var reg = users.register(new Server.RegisterRequest("u", "p", "e@e"));
        assertNotNull(reg.authToken());

        assertDoesNotThrow(() -> clear.clear());

        // After clear, login should fail (no users)
        assertThrows(SecurityException.class,
                () -> users.login(new Server.LoginRequest("u", "p")));
    }
}
