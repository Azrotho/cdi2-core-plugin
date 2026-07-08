package fr.citedesiles.coreplugin;

import com.google.gson.JsonObject;

import java.util.List;

public record Team(int id, String name, String tag, String color, String leader, int staff, int verification) {

    static Team fromJson(JsonObject json) {
        return new Team(
                json.get("id").getAsInt(),
                json.get("name").getAsString(),
                json.get("tag").getAsString(),
                json.get("color").getAsString(),
                json.get("leader").getAsString(),
                json.get("staff").getAsInt(),
                json.get("verification").getAsInt()
        );
    }

    public List<Player> players(CoreCDI api) {
        return api.getTeamPlayers(this.id);
    }
}
