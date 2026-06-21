package fr.citedesiles.coreplugin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Client HTTP pour l'API CDI2 (cdi2-core-server).
 * <pre>{@code
 * CoreCDI api = new CoreCDI("http://localhost:3000", "bipboup");
 * Pour check --> api.ping(); // true
 * }</pre>
 */
public class CoreCDI {

    private static final Gson GSON = new Gson();
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient http;

    public CoreCDI(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    // Public API

    public boolean ping() {
        JsonObject res = request("GET", "/ping", null);
        return "pong!".equals(res.get("message").getAsString());
    }

    public String quoi() {
        JsonObject res = request("GET", "/quoi", null);
        return res.get("message").getAsString();
    }

    // Admin API

    public String testToken(String token) {
        JsonObject res = request("GET", "/test-token", null, token);
        return res.get("user").getAsString();
    }

    public String createToken(String owner) {
        JsonObject body = new JsonObject();
        body.addProperty("owner", owner);
        JsonObject res = request("POST", "/token", body);
        return res.get("token").getAsString();
    }

    public void deleteToken(String tokenToDelete) {
        JsonObject body = new JsonObject();
        body.addProperty("token", tokenToDelete);
        request("DELETE", "/token", body);
    }

    public Team createTeam(String name, String tag, String color, String leader) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("tag", tag);
        body.addProperty("color", color);
        body.addProperty("leader", leader);
        JsonObject res = request("POST", "/teams", body);
        int id = res.get("id").getAsInt();
        return getTeam(id);
    }

    public String requestVerification(String uuid, String playerName) {
        JsonObject body = new JsonObject();
        body.addProperty("uuid", uuid);
        body.addProperty("player_name", playerName);
        JsonObject res = request("POST", "/verify", body);
        return res.get("code").getAsString();
    }

    public void checkVerification(String uuid, String code, String discordId) {
        JsonObject body = new JsonObject();
        body.addProperty("uuid", uuid);
        body.addProperty("code", code);
        body.addProperty("discordId", discordId);
        request("POST", "/verify/check", body);
    }

    /**
     * Lie un compte Discord via le code de vérification (sans UUID).
     * Utilisé par le bot Discord.
     * @return le nom du joueur lié
     */
    public String linkDiscord(String code, String discordId) {
        JsonObject body = new JsonObject();
        body.addProperty("code", code);
        body.addProperty("discordId", discordId);
        JsonObject res = request("POST", "/verify/check", body);
        return res.has("player_name") ? res.get("player_name").getAsString() : null;
    }

    // player

    public Player getPlayer(String uuid) {
        JsonObject res = request("GET", "/players/" + uuid, null);
        return Player.fromJson(res.getAsJsonObject("player"));
    }

    public Player getPlayerByDiscord(String discordId) {
        JsonObject res = request("GET", "/players/discord/" + discordId, null);
        return Player.fromJson(res.getAsJsonObject("player"));
    }

    public List<Player> getPlayers() {
        JsonObject res = request("GET", "/players", null);
        JsonArray arr = res.getAsJsonArray("players");
        List<Player> players = new ArrayList<>();
        for (JsonElement e : arr) {
            players.add(Player.fromJson(e.getAsJsonObject()));
        }
        return players;
    }

    public void deletePlayer(String uuid) {
        request("DELETE", "/players/" + uuid, null);
    }

    public void setPlayerTeam(String uuid, int team) {
        JsonObject body = new JsonObject();
        body.addProperty("team", team);
        request("POST", "/players/" + uuid + "/team", body);
    }

    public void setPlayerTeam(String uuid, Team team) {
        setPlayerTeam(uuid, team.id());
    }

    public void setPlayerName(String uuid, String name) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        request("POST", "/players/" + uuid + "/name", body);
    }

    // team

    public List<Team> getTeams() {
        JsonObject res = request("GET", "/teams", null);
        JsonArray arr = res.getAsJsonArray("teams");
        List<Team> teams = new ArrayList<>();
        for (JsonElement e : arr) {
            teams.add(Team.fromJson(e.getAsJsonObject()));
        }
        return teams;
    }

    public Team getTeam(int id) {
        JsonObject res = request("GET", "/teams/" + id, null);
        return Team.fromJson(res.getAsJsonObject("team"));
    }

    public List<Player> getTeamPlayers(int teamId) {
        JsonObject res = request("GET", "/teams/" + teamId + "/players", null);
        JsonArray arr = res.getAsJsonArray("players");
        List<Player> players = new ArrayList<>();
        for (JsonElement e : arr) {
            players.add(Player.fromJson(e.getAsJsonObject()));
        }
        return players;
    }

    public void deleteTeam(int id) {
        request("DELETE", "/teams/" + id, null);
    }

    public void setTeamColor(int id, String color) {
        JsonObject body = new JsonObject();
        body.addProperty("color", color);
        request("POST", "/teams/" + id + "/color", body);
    }

    public void setTeamLeader(int id, String leaderUuid) {
        JsonObject body = new JsonObject();
        body.addProperty("leader", leaderUuid);
        request("POST", "/teams/" + id + "/leader", body);
    }

    public void setTeamTag(int id, String tag) {
        JsonObject body = new JsonObject();
        body.addProperty("tag", tag);
        request("POST", "/teams/" + id + "/tag", body);
    }

    public void setTeamName(int id, String name) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        request("POST", "/teams/" + id + "/name", body);
    }

    // transaction

    public void createTransaction(int teamId, String memberUuid, double totalValue, String reason, int quantity) {
        JsonObject body = new JsonObject();
        body.addProperty("team_id", teamId);
        body.addProperty("member_uuid", memberUuid);
        body.addProperty("total_value", totalValue);
        body.addProperty("reason", reason);
        body.addProperty("quantity", quantity);
        request("POST", "/transaction", body);
    }

    public List<Transaction> getTeamTransactions(int teamId) {
        JsonObject res = request("GET", "/transactions/" + teamId, null);
        JsonArray arr = res.getAsJsonArray("transactions");
        List<Transaction> transactions = new ArrayList<>();
        for (JsonElement e : arr) {
            transactions.add(Transaction.fromJson(e.getAsJsonObject()));
        }
        return transactions;
    }

    public double getTeamMoney(int teamId) {
        JsonObject res = request("GET", "/team/" + teamId + "/money", null);
        return res.get("total_money").getAsDouble();
    }

    // interne

    private JsonObject request(String method, String path, JsonObject body) {
        return request(method, path, body, apiKey);
    }

    private JsonObject request(String method, String path, JsonObject body, String token) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(TIMEOUT)
                    .header("Authorization", token)
                    .header("Accept", "application/json");

            if (body != null) {
                builder.header("Content-Type", "application/json")
                       .method(method, HttpRequest.BodyPublishers.ofString(GSON.toJson(body)));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            JsonObject json;
            try {
                json = GSON.fromJson(response.body(), JsonObject.class);
            } catch (Exception e) {
                throw new ApiException(response.statusCode(), response.body());
            }

            if (response.statusCode() >= 400) {
                String error = json.has("error") ? json.get("error").getAsString() : response.body();
                throw new ApiException(response.statusCode(), error);
            }

            return json;
        } catch (ApiException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            String msg = e.getMessage();
            throw new ApiException(0, "Network error: " + (msg != null ? msg : e.getClass().getSimpleName()));
        }
    }

    // exception pour l'api

    public static class ApiException extends RuntimeException {
        private final int statusCode;

        public ApiException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public String toString() {
            return "ApiException[" + statusCode + "] " + getMessage();
        }
    }
}
