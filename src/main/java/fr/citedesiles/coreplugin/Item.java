package fr.citedesiles.coreplugin;

import com.google.gson.JsonObject;

public record Item(String material, int price, int maxDecrease, int maxIncrease, int currentPrice) {

    static Item fromJson(JsonObject json) {
        return new Item(
                json.get("material").getAsString(),
                json.get("price").getAsInt(),
                json.get("max_decrease").getAsInt(),
                json.get("max_increase").getAsInt(),
                json.get("current_price").getAsInt()
        );
    }
}
