package client;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ServerFacade {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;

    private String authToken;

    private final Gson gson = new Gson();

    public ServerFacade(int port) {
        this.baseUrl = "http://localhost:" + port;
        // validate port
    }

    // dtos
    public record AuthData(String username, String authToken) {}
    public record RegisterReq(String username, String password, String email) {}
    public record LoginReq(String username, String password) {}
    public record CreateGameReq(String gameName) {}
    public record JoinGameReq(Integer gameID, String playerColor) {}
    public record GameInfo(Integer gameID, String gameName, String whiteUsername, String blackUsername) {}
    public record ListGamesRes(List<GameInfo> games) {}

    // helpers
    private HttpRequest.Builder base(String path) {
        var b = HttpRequest.newBuilder(URI.create(baseUrl + path));
        if (authToken != null) {
            b.header("Authorization", authToken);
        }
        // TODO: maybe add default headers?
        return b;
    }

    private static void ensure2xx(HttpResponse<?> res, String friendly) throws IOException {
        if (res.statusCode() / 100 != 2) {
            // TODO: maybe include body in error?
            throw new IOException(friendly);
        }
    }

    // API calls
    public AuthData register(String u, String p, String e) throws Exception {
        var body = gson.toJson(new RegisterReq(u, p, e));
        var req = base("/user")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();

        var res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        ensure2xx(res, "Registration failed. Try another username.");

        var auth = gson.fromJson(res.body(), AuthData.class);
        authToken = auth.authToken();
        return auth;
    }

    public AuthData login(String u, String p) throws Exception {
        var body = gson.toJson(new LoginReq(u, p));
        var req = base("/session")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();

        var res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensure2xx(res, "Login failed. Check username/password.");

        var auth = gson.fromJson(res.body(), AuthData.class);
        authToken = auth.authToken();

        return auth;
    }

    public void logout() throws Exception {
        // TODO: maybe early return if authToken == null?
        var req = base("/session").DELETE().build();
        var res = http.send(req, HttpResponse.BodyHandlers.discarding());
        ensure2xx(res, "Logout failed.");

        authToken = null;
    }

    public int createGame(String name) throws Exception {
        // TODO: name validation?
        var body = gson.toJson(new CreateGameReq(name));
        var req = base("/game")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();

        var res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensure2xx(res, "Could not create game.");

        // will make a proper response DTO
        var json = gson.fromJson(res.body(), java.util.Map.class);

        return ((Number) json.get("gameID")).intValue();
    }

    public List<GameInfo> listGames() throws Exception {
        var req = base("/game").GET().build();

        // use streamer parser
        var res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensure2xx(res, "Could not list games.");

        return gson.fromJson(res.body(), ListGamesRes.class).games();
    }

    public void joinGame(int gameId, String color) throws Exception {
        var body = gson.toJson(new JoinGameReq(gameId, color));

        var req = base("/game")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();

        var res = http.send(req, HttpResponse.BodyHandlers.discarding());

        ensure2xx(res, color == null ? "Could not observe game." : "Could not join game.");
    }

    // optional for tests
    public void clearDb() throws Exception {
        // risky
        var req = HttpRequest.newBuilder(URI.create(baseUrl + "/db")).DELETE().build();
        http.send(req, HttpResponse.BodyHandlers.discarding());
    }
}
