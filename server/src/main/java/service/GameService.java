package service;

import chess.ChessGame;
import dataaccess.*;
import model.*;

import java.util.List;

public class GameService {
    private final DataAccess dao;
    public GameService(DataAccess dao) { this.dao = dao; }

    private String requireAuth(String token) throws DataAccessException {
        var auth = dao.getAuth(token);
        if (auth == null) throw new SecurityException("unauthorized");
        return auth.username();
    }

    public ListGamesResult list(String token) throws DataAccessException {
        requireAuth(token);
        var summaries = dao.listGames().stream()
                .map(g -> new GameSummary(g.gameID(), g.whiteUsername(), g.blackUsername(), g.gameName()))
                .toList();
        return new ListGamesResult(summaries);
    }

    public CreateGameResult create(String token, server.Server.CreateGameRequest r) throws DataAccessException {
        requireAuth(token);
        if (r == null || r.gameName() == null || r.gameName().isBlank())
            throw new IllegalArgumentException("bad request");
        int id = dao.createGame(new GameData(0, null, null, r.gameName(), new ChessGame()));
        return new CreateGameResult(id);
    }

    public EmptyResult join(String token, server.Server.JoinGameRequest r) throws DataAccessException {
        String username = requireAuth(token);
        if (r == null || r.gameID() == null || r.playerColor() == null)
            throw new IllegalArgumentException("bad request");

        var game = dao.getGame(r.gameID());
        if (game == null) throw new IllegalArgumentException("bad request");

        String color = r.playerColor().toUpperCase();
        String white = game.whiteUsername();
        String black = game.blackUsername();

        if ("WHITE".equals(color)) {
            if (white != null) throw new SecurityException("already taken");
            white = username;
        } else if ("BLACK".equals(color)) {
            if (black != null) throw new SecurityException("already taken");
            black = username;
        } else {
            throw new IllegalArgumentException("bad request");
        }

        dao.updateGame(new GameData(game.gameID(), white, black, game.gameName(), game.game()));
        return new EmptyResult();
    }

    public record GameSummary(int gameID, String whiteUsername, String blackUsername, String gameName) {}
    public record ListGamesResult(List<GameSummary> games) {}
    public record CreateGameResult(int gameID) {}
    public record EmptyResult() {}
}
