package server.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import chess.ChessGame;
import chess.ChessMove;
import chess.InvalidMoveException;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import model.AuthData;
import model.GameData;
import service.GameService;
import service.UserService;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler {

    private final DataAccess dao;
    private final UserService userService;
    private final GameService gameService;

    private final Gson gson = new GsonBuilder().create();

    private final Map<String, Integer> sessionToGame = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();
    private final Map<Integer, Set<WsContext>> gameToSessions = new ConcurrentHashMap<>();
    private final Set<Integer> finishedGames = ConcurrentHashMap.newKeySet();

    public WebSocketHandler(DataAccess dao, UserService userService, GameService gameService) {
        this.dao = dao;
        this.userService = userService;
        this.gameService = gameService;
    }

    public void onConnect(WsConnectContext ctx) {}

    public void onClose(WsCloseContext ctx) {
        String sessionId = ctx.sessionId();
        Integer gameID = sessionToGame.remove(sessionId);
        sessionToUser.remove(sessionId);

        if (gameID != null) {
            Set<WsContext> sessions = gameToSessions.get(gameID);
            if (sessions != null) {
                sessions.remove(ctx);
                if (sessions.isEmpty()) {
                    gameToSessions.remove(gameID);
                    finishedGames.remove(gameID);
                }
            }
        }
    }

    public void onMessage(WsMessageContext ctx) {
        String raw = ctx.message();
        UserGameCommand cmd = gson.fromJson(raw, UserGameCommand.class);
        switch (cmd.getCommandType()) {
            case CONNECT -> handleConnect(ctx, cmd);
            case MAKE_MOVE -> handleMakeMove(ctx, cmd, raw);
            case LEAVE -> handleLeave(ctx, cmd);
            case RESIGN -> handleResign(ctx, cmd);
        }
    }

    private void send(WsContext ctx, ServerMessage msg) {
        ctx.send(gson.toJson(msg));
    }

    private void broadcastToGame(int gameID, ServerMessage msg) {
        Set<WsContext> sessions = gameToSessions.get(gameID);
        if (sessions == null) return;
        String json = gson.toJson(msg);
        for (WsContext c : sessions) c.send(json);
    }

    private void broadcastToGameExcept(int gameID, WsContext except, ServerMessage msg) {
        Set<WsContext> sessions = gameToSessions.get(gameID);
        if (sessions == null) return;
        String json = gson.toJson(msg);
        for (WsContext c : sessions)
            if (!c.sessionId().equals(except.sessionId()))
                c.send(json);
    }

    private void handleConnect(WsMessageContext ctx, UserGameCommand cmd) {
        String token = cmd.getAuthToken();
        Integer gameID = cmd.getGameID();
        if (token == null || gameID == null) {
            send(ctx, new ErrorMessage("Error: bad connect"));
            return;
        }
        try {
            AuthData auth = dao.getAuth(token);
            if (auth == null) {
                send(ctx, new ErrorMessage("Error: bad auth"));
                return;
            }
            GameData game = dao.getGame(gameID);
            if (game == null) {
                send(ctx, new ErrorMessage("Error: bad game id"));
                return;
            }
            String user = auth.username();
            String sid = ctx.sessionId();

            sessionToUser.put(sid, user);
            sessionToGame.put(sid, gameID);
            gameToSessions.computeIfAbsent(gameID, k -> ConcurrentHashMap.newKeySet()).add(ctx);

            send(ctx, new LoadGameMessage(game.game()));

            String role =
                    user.equals(game.whiteUsername()) ? "as WHITE" :
                            user.equals(game.blackUsername()) ? "as BLACK" :
                                    "as an observer";

            broadcastToGameExcept(gameID, ctx, new NotificationMessage(user + " connected " + role));

        } catch (Exception e) {
            send(ctx, new ErrorMessage("Error: connect failed"));
        }
    }

    private void handleMakeMove(WsMessageContext ctx, UserGameCommand base, String rawJson) {
        String token = base.getAuthToken();
        Integer gameID = base.getGameID();
        if (token == null || gameID == null) {
            send(ctx, new ErrorMessage("Error"));
            return;
        }
        if (finishedGames.contains(gameID)) {
            send(ctx, new ErrorMessage("Error"));
            return;
        }
        try {
            AuthData auth = dao.getAuth(token);
            if (auth == null) {
                send(ctx, new ErrorMessage("Error"));
                return;
            }
            GameData gameData = dao.getGame(gameID);
            if (gameData == null) {
                send(ctx, new ErrorMessage("Error"));
                return;
            }
            String user = auth.username();

            ChessGame.TeamColor moverColor =
                    user.equals(gameData.whiteUsername()) ? ChessGame.TeamColor.WHITE :
                            user.equals(gameData.blackUsername()) ? ChessGame.TeamColor.BLACK : null;

            if (moverColor == null) {
                send(ctx, new ErrorMessage("Error"));
                return;
            }

            ChessGame game = gameData.game();

            if (game.getTeamTurn() != moverColor) {
                send(ctx, new ErrorMessage("Error"));
                return;
            }

            MakeMoveCommand mm = gson.fromJson(rawJson, MakeMoveCommand.class);
            ChessMove move = mm.getMove();

            try {
                game.makeMove(move);
            } catch (InvalidMoveException e) {
                send(ctx, new ErrorMessage("Error"));
                return;
            }

            GameData updated = new GameData(
                    gameData.gameID(),
                    gameData.whiteUsername(),
                    gameData.blackUsername(),
                    gameData.gameName(),
                    game
            );
            dao.updateGame(updated);

            broadcastToGame(gameID, new LoadGameMessage(game));
            broadcastToGameExcept(gameID, ctx, new NotificationMessage(user + " moved"));

            ChessGame.TeamColor turn = game.getTeamTurn();

            if (game.isInCheckmate(turn)) {
                finishedGames.add(gameID);
                broadcastToGame(gameID, new NotificationMessage("checkmate"));
            } else if (game.isInStalemate(turn)) {
                finishedGames.add(gameID);
                broadcastToGame(gameID, new NotificationMessage("stalemate"));
            } else if (game.isInCheck(turn)) {
                broadcastToGame(gameID, new NotificationMessage("check"));
            }

        } catch (Exception e) {
            send(ctx, new ErrorMessage("Error"));
        }
    }

    private void handleLeave(WsMessageContext ctx, UserGameCommand cmd) {
        String token = cmd.getAuthToken();
        Integer gameID = cmd.getGameID();
        try {
            AuthData auth = dao.getAuth(token);
            if (auth == null) return;

            GameData game = dao.getGame(gameID);
            if (game == null) return;

            String user = auth.username();
            String sid = ctx.sessionId();

            String white = game.whiteUsername();
            String black = game.blackUsername();

            if (user.equals(white)) white = null;
            if (user.equals(black)) black = null;

            dao.updateGame(new GameData(game.gameID(), white, black, game.gameName(), game.game()));

            broadcastToGameExcept(gameID, ctx, new NotificationMessage(user + " left"));

            sessionToUser.remove(sid);
            sessionToGame.remove(sid);
            gameToSessions.get(gameID).remove(ctx);

        } catch (Exception ignored) {}
    }

    private void handleResign(WsMessageContext ctx, UserGameCommand cmd) {
        String token = cmd.getAuthToken();
        Integer gameID = cmd.getGameID();

        try {
            AuthData auth = dao.getAuth(token);
            if (auth == null) {
                send(ctx, new ErrorMessage("Error"));
                return;
            }
            GameData game = dao.getGame(gameID);
            if (game == null) return;

            String user = auth.username();

            boolean isPlayer = user.equals(game.whiteUsername()) || user.equals(game.blackUsername());
            if (!isPlayer) {
                send(ctx, new ErrorMessage("Error"));
                return;
            }

            if (finishedGames.contains(gameID)) {
                send(ctx, new ErrorMessage("Error"));
                return;
            }

            finishedGames.add(gameID);
            broadcastToGame(gameID, new NotificationMessage(user + " resigned"));

        } catch (Exception e) {
            send(ctx, new ErrorMessage("Error"));
        }
    }
}
