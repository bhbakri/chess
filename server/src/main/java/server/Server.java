package server;

import com.google.gson.Gson;
import io.javalin.Javalin;
import dataaccess.*;
import service.*;

public class Server {

    private final Javalin javalin;
    private final Gson gson = new Gson();

    private final DataAccess dao = new MemoryDataAccess();
    private final ClearService clearSvc = new ClearService(dao);
    private final UserService userSvc = new UserService(dao);
    private final GameService gameSvc = new GameService(dao);

    public Server() {
        javalin = Javalin.create(cfg -> cfg.staticFiles.add("web"));

        // exception → http mapping
        javalin.exception(IllegalArgumentException.class, (e, ctx) ->
                ctx.status(400)
                        .result(gson.toJson(new ErrorMsg("Error: bad request")))
                        .contentType("application/json"));

        javalin.exception(SecurityException.class, (e, ctx) -> {
            String msg = e.getMessage();
            if ("already taken".equals(msg)) {
                ctx.status(403)
                        .result(gson.toJson(new ErrorMsg("Error: already taken")))
                        .contentType("application/json");
            } else {
                ctx.status(401)
                        .result(gson.toJson(new ErrorMsg("Error: unauthorized")))
                        .contentType("application/json");
            }
        });

        javalin.exception(DataAccessException.class, (e, ctx) ->
                ctx.status(500)
                        .result(gson.toJson(new ErrorMsg("Error: " + e.getMessage())))
                        .contentType("application/json"));

        javalin.exception(Exception.class, (e, ctx) ->
                ctx.status(500)
                        .result(gson.toJson(new ErrorMsg("Error: " + e.getMessage())))
                        .contentType("application/json"));

        // routes
        javalin.delete("/db", ctx -> {
            clearSvc.clear();
            ctx.status(200)
                    .result(gson.toJson(new Empty()))
                    .contentType("application/json");
        });

        javalin.post("/user", ctx -> {
            var req = gson.fromJson(ctx.body(), RegisterRequest.class);
            var res = userSvc.register(req);
            ctx.status(200).result(gson.toJson(res)).contentType("application/json");
        });

        javalin.post("/session", ctx -> {
            var req = gson.fromJson(ctx.body(), LoginRequest.class);
            var res = userSvc.login(req);
            ctx.status(200).result(gson.toJson(res)).contentType("application/json");
        });

        javalin.delete("/session", ctx -> {
            var token = ctx.header("authorization");
            var res = userSvc.logout(new LogoutRequest(token));
            ctx.status(200).result(gson.toJson(res)).contentType("application/json");
        });

        javalin.get("/game", ctx -> {
            var token = ctx.header("authorization");
            var res = gameSvc.list(token);
            ctx.status(200).result(gson.toJson(res)).contentType("application/json");
        });

        javalin.post("/game", ctx -> {
            var token = ctx.header("authorization");
            var req = gson.fromJson(ctx.body(), CreateGameRequest.class);
            var res = gameSvc.create(token, req);
            ctx.status(200).result(gson.toJson(res)).contentType("application/json");
        });

        javalin.put("/game", ctx -> {
            var token = ctx.header("authorization");
            var req = gson.fromJson(ctx.body(), JoinGameRequest.class);
            var res = gameSvc.join(token, req);
            ctx.status(200).result(gson.toJson(res)).contentType("application/json");
        });
    }

    public int run(int desiredPort) { javalin.start(desiredPort); return javalin.port(); }
    public void stop() { javalin.stop(); }

    record ErrorMsg(String message) {}
    record Empty() {}

    public static record RegisterRequest(String username, String password, String email) {}
    public static record LoginRequest(String username, String password) {}
    public static record LogoutRequest(String authToken) {}
    public static record CreateGameRequest(String gameName) {}
    public static record JoinGameRequest(String playerColor, Integer gameID) {}
}
