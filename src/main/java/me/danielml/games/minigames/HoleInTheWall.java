package me.danielml.games.minigames;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import me.danielml.games.Game;
import me.danielml.util.ScoreboardUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import static me.danielml.MCCIStats.LOGGER;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;

public class HoleInTheWall extends Game {

    private int lastPlacement;
    private double averagePlacement;
    private int topWallSpeedSurvived;
    private ArrayList<Integer> lastPlacements;

    public HoleInTheWall() {
        lastPlacement = 0;
        averagePlacement = 0;
        topWallSpeedSurvived = 0;
        lastPlacements = new ArrayList<>();
    }

    @Override
    public void onChatMessageInGame(Text messageText) {
        String messageContent = messageText.getString();

        // The icon at the start is the skull emoji in MCCI font
        if(messageContent.contains("you were eliminated in ") && messageContent.startsWith("[\uE202]"))
        {

            String placementText = messageContent.split("you were eliminated in")[1];
            int placement = extractNumberFromText(placementText);
            LOGGER.info("Eliminated at placement: " + placement);

            lastPlacement = placement;
            lastPlacements.add(placement);
            DoubleSummaryStatistics stats = lastPlacements.stream().mapToDouble(p -> p).summaryStatistics();
            averagePlacement = stats.getAverage();

            ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance())
                    .ifPresent(scoreboard -> {
                        String wallSpeedText = ScoreboardUtil.getSidebarRows(scoreboard).get(0); // Wall Speed is at Index 0
                        int wallSpeed = extractNumberFromText(wallSpeedText.split("WALL SPEED: ")[1]);
                        topWallSpeedSurvived = Math.max(topWallSpeedSurvived, wallSpeed);
                    });
        } else if(messageContent.startsWith("[") && messageContent.contains("you survived")) {
            lastPlacement = 1;
            lastPlacements.add(lastPlacement);
            DoubleSummaryStatistics stats = lastPlacements.stream().mapToDouble(p -> p).summaryStatistics();
            averagePlacement = stats.getAverage();

            ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance())
                    .ifPresent(scoreboard -> {
                        String wallSpeedText = ScoreboardUtil.getSidebarRows(scoreboard).get(0); // Wall Speed is at Index 0
                        int wallSpeed = extractNumberFromText(wallSpeedText.split("WALL SPEED: ")[1]);
                        topWallSpeedSurvived = Math.max(topWallSpeedSurvived, wallSpeed);
                    });
        }
    }

    @Override
    public String displayData() {
        return "Last Placement: " + lastPlacement + "\n " +
                "Avg. Placement " + ((int)averagePlacement) + "(" + twoDigitFormat.format(averagePlacement) + ") \n " +
                "Top Wall Speed: " + topWallSpeedSurvived;
    }

    @Override
    public String previewUI() {
        return "Last Placement: 5 \n " +
                "Avg. Placement 3 (3.89) \n " +
                "Top Wall Speed: 70";
    }

    @Override
    public String getSidebarIdentifier() {
        return "HOLE IN THE WALL";
    }

    @Override
    public JsonObject serializeData() {
        Gson gson = new Gson();
        JsonObject object = new JsonObject();

        object.addProperty("last_placement", lastPlacement);
        var placementsJSON = gson.toJsonTree(lastPlacements).getAsJsonArray();
        object.add("placements", placementsJSON);
        object.addProperty("top_wall_speed", topWallSpeedSurvived);

        return object;
    }

    @Override
    public void deserializeData(JsonObject jsonObject) {
        Gson gson = new Gson();
        this.lastPlacement = jsonObject.get("last_placement").getAsInt();
        var listType = new TypeToken<ArrayList<Integer>>() {}.getType();
        this.lastPlacements = gson.fromJson(jsonObject.get("placements"), listType);
        this.topWallSpeedSurvived = jsonObject.get("top_wall_speed").getAsInt();
        averagePlacement = lastPlacements.stream().mapToDouble(p -> p).summaryStatistics().getAverage();
    }

    @Override
    public void loadFailSafeDefaultData() {
        lastPlacement = 0;
        averagePlacement = 0;
        topWallSpeedSurvived = 0;
        lastPlacements = new ArrayList<>();

    }
}
