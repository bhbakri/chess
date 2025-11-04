package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;

public class UserService {
    private final DataAccess dao;

    public UserService(DataAccess dao) {
        this.dao = dao;
    }

    public AuthResult register(server.Server.RegisterRequest r) throws DataAccessException {
        if (r == null || blank(r.username()) || blank(r.password()) || blank(r.email())) {
            throw new IllegalArgumentException("bad request");
        }
        if (dao.getUser(r.username()) != null) {
            throw new SecurityException("already taken");
        }

        // hash password
        String hash = BCrypt.hashpw(r.password(), BCrypt.gensalt(12));

        dao.createUser(new UserData(r.username(), hash, r.email()));

        // Issue auth token
        String token = UUID.randomUUID().toString();
        dao.createAuth(new AuthData(token, r.username()));
        return new AuthResult(r.username(), token);
    }

    public AuthResult login(server.Server.LoginRequest r) throws DataAccessException {
        if (r == null || blank(r.username()) || blank(r.password())) {
            throw new IllegalArgumentException("bad request");
        }
        var user = dao.getUser(r.username());

        if (user == null || user.passwordHash() == null || !BCrypt.checkpw(r.password(), user.passwordHash())) {
            throw new SecurityException("unauthorized");
        }

        String token = UUID.randomUUID().toString();
        dao.createAuth(new AuthData(token, r.username()));
        return new AuthResult(r.username(), token);
    }

    public EmptyResult logout(server.Server.LogoutRequest r) throws DataAccessException {
        var auth = dao.getAuth(r.authToken());
        if (auth == null) {
            throw new SecurityException("unauthorized");
        }
        dao.deleteAuth(r.authToken());
        return new EmptyResult();
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }

    public record AuthResult(String username, String authToken) {}
    public record EmptyResult() {}
}
