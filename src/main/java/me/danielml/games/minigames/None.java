package me.danielml.games.minigames;

import com.google.gson.JsonObject;
import me.danielml.games.Game;

public class None extends Game {
    @Override
    public String displayData() {
        return "";
    }

    @Override
    public String getSidebarIdentifier() {
        return "None";
    }
    @Override
    public void loadFailSafeDefaultData() {}

    @Override
    public JsonObject serializeData() {
        return new JsonObject();
    }

    @Override
    public void deserializeData(JsonObject jsonObject) {}
}
