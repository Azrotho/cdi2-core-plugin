package fr.citedesiles.coreplugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

public record NPC(String npc, int releaseDay, List<NPCItem> items) {

    static NPC fromJson(JsonObject json) {
        List<NPCItem> items = new ArrayList<>();
        for (JsonElement e : json.getAsJsonArray("items")) {
            items.add(NPCItem.fromJson(e.getAsJsonObject()));
        }
        return new NPC(
                json.get("npc").getAsString(),
                json.get("release_day").getAsInt(),
                items
        );
    }
}
