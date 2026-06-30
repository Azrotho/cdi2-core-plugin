package fr.citedesiles.coreplugin;

import com.google.gson.JsonObject;

public record NPCItem(String material, int releaseDay) {

    static NPCItem fromJson(JsonObject json) {
        return new NPCItem(
                json.get("material").getAsString(),
                json.get("release_day").getAsInt()
        );
    }
}
