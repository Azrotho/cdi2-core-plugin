package fr.citedesiles.coreplugin;

import com.google.gson.JsonObject;

public record Transaction(int id, int teamId, String memberUuid, double totalValue, String reason, int quantity) {

    static Transaction fromJson(JsonObject json) {
        return new Transaction(
                json.get("id").getAsInt(),
                json.get("team_id").getAsInt(),
                json.get("member_uuid").getAsString(),
                json.get("total_value").getAsDouble(),
                json.get("reason").getAsString(),
                json.get("quantity").getAsInt()
        );
    }
}
