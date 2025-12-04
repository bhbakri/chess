package server.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import websocket.commands.UserGameCommand;
import websocket.commands.MakeMoveCommand;
import websocket.messages.ServerMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.ErrorMessage;
import websocket.messages.NotificationMessage;

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
                }
            }
        }
    }

    public void onMessage(WsMessageContext ctx) {
        String rawJson = ctx.message();
        UserGameCommand command = gson.fromJson(rawJson, UserGameCommand.class);

        switch (command.getCommandType()) {
            case CONNECT -> handleConnect(ctx, command);
            case MAKE_MOVE -> handleMakeMove(ctx, command, rawJson);
            case LEAVE -> handleLeave(ctx, command);
            case RESIGN -> handleResign(ctx, command);
        }
    }

    private void send(WsContext ctx, ServerMessage message) {
        ctx.send(gson.toJson(message));
    }

    private void broadcastToGame(int gameID, ServerMessage message) {
        Set<WsContext> sessions = gameToSessions.get(gameID);
        if (sessions == null) return;
        String json = gson.toJson(message);
        for (WsContext ctx : sessions) {
            ctx.send(json);
        }
    }

    private void broadcastToGameExcept(int gameID, WsContext except, ServerMessage message) {
        Set<WsContext> sessions = gameToSessions.get(gameID);
        if (sessions == null) return;
        String json = gson.toJson(message);
        for (WsContext ctx : sessions) {
            if (!ctx.sessionId().equals(except.sessionId())) {
                ctx.send(json);
            }
        }
    }

    private void handleConnect(WsMessageContext ctx, UserGameCommand baseCommand) {
        String authToken = baseCommand.getAuthToken();
        Integer gameID = baseCommand.getGameID();

        if (authToken == null || gameID == null) {
            send(ctx, new ErrorMessage("Error: missing authToken or gameID"));
            return;
        }

        try {
            AuthData auth = dao.getAuth(authToken);
            if (auth == null) {
                send(ctx, new ErrorMessage("Error: invalid auth token"));
                return;
            }

            GameData gameData = dao.getGame(gameID);
            if (gameData == null) {
                send(ctx, new ErrorMessage("Error: invalid game id"));
                return;
            }

            String username = auth.username();
            String sessionId = ctx.sessionId();

            sessionToUser.put(sessionId, username);
            sessionToGame.put(sessionId, gameID);
            gameToSessions.computeIfAbsent(gameID, k -> ConcurrentHashMap.newKeySet()).add(ctx);

            send(ctx, new LoadGameMessage(gameData.game()));

            String role;
            if (username.equals(gameData.whiteUsername())) {
                role = "as WHITE";
            } else if (username.equals(gameData.blackUsername())) {
                role = "as BLACK";
            } else {
                role = "as an observer";
            }

            NotificationMessage note = new NotificationMessage(username + " connected " + role);
            broadcastToGameExcept(gameID, ctx, note);

        } catch (DataAccessException ex) {
            send(ctx, new ErrorMessage("Error: " + ex.getMessage()));
        }
    }

    private void handleMakeMove(WsMessageContext ctx, UserGameCommand baseCommand, String rawJson) {
        // TODO
    }

    private void handleLeave(WsMessageContext ctx, UserGameCommand baseCommand) {
        // TODO
    }

    private void handleResign(WsMessageContext ctx, UserGameCommand baseCommand) {
        // TODO
    }
}
