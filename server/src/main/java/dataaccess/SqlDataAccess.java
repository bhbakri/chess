package dataaccess;

import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;
import chess.ChessGame;

import java.sql.*;
import java.util.*;

public class SqlDataAccess implements DataAccess {

    private final DatabaseManager db;
    private final Gson gson = new Gson();

    public SqlDataAccess(DatabaseManager db) { this.db = db; }

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
}
