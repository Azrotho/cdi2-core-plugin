package fr.citedesiles.coreplugin;

import com.google.gson.JsonObject;

public record Player(String uuid, String name, String discordId, int team) {

    static Player fromJson(JsonObject json) {
        return new Player(
                json.get("uuid").getAsString(),
                json.get("name").getAsString(),
                json.get("discord_id").getAsString(),
                json.get("team").getAsInt()
        );
    }
}
