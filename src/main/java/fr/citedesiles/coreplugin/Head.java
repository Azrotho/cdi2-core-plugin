package fr.citedesiles.coreplugin;

import com.google.gson.JsonObject;

public record Head(int id, int team, int x, int y, int z) {

    public static Head fromJson(JsonObject json) {
        return new Head(
                json.has("id") ? json.get("id").getAsInt() : 0,
                json.get("team").getAsInt(),
                json.get("x").getAsInt(),
                json.get("y").getAsInt(),
                json.get("z").getAsInt()
        );
    }
}
