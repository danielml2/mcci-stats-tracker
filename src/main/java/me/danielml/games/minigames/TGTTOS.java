package me.danielml.games.minigames;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.danielml.games.Game;
import me.danielml.util.ScoreboardUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import static me.danielml.MCCIStats.LOGGER;

public class TGTTOS extends Game {

    private int lastPlacement;
    private ArrayList<Integer> lastPlacements;
    private ArrayList<Integer> lastRoundPlacements;

    private double gamePlacementAverage;
    private double roundAveragePlacements;

    private HashMap<String, Double> bestMapTimes;
    private long roundTime;
    private String currentMap;
    private double bestCurrentMapTime;

    @Override
    public void onChatMessageInGame(Text messageText) {
        String messageContent = messageText.getString();

        if(messageContent.startsWith("[")) {
            LOGGER.info("MCCI: Message started with [, most likely a game message");
            if (messageContent.contains("you finished the round and came in")) {
                LOGGER.info("MCCI: Detected whack from message!");
                int placement = extractNumberFromText(messageContent.split("came in")[1]);
                LOGGER.info("MCCI: Round Placement: " + placement);
                lastRoundPlacements.add(placement);
                var stats = lastRoundPlacements.stream().mapToDouble(p -> p).summaryStatistics();
                roundAveragePlacements = stats.getAverage();
                roundTime = System.currentTimeMillis() - roundTime;
                double timeInSeconds = (double) roundTime / 1000;
                LOGGER.info("MCCI: Round Time: " + timeInSeconds);
                if (!bestMapTimes.containsKey(currentMap))
                    bestMapTimes.put(currentMap, timeInSeconds);
                else {
                    double oldTime = bestMapTimes.get(currentMap);
                    bestMapTimes.replace(currentMap, Math.min(timeInSeconds, oldTime));
                }
                updateMapBestTime();

            } else if (messageContent.contains("Round") && messageContent.contains("started!")) {
                roundTime = System.currentTimeMillis();
                LOGGER.info("MCCI: Searching for map trigger from Round Started Message");
                ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance()).ifPresent(scoreboard -> {

                    var sidebarRows = ScoreboardUtil.getSidebarRows(scoreboard);
                    if (sidebarRows.size() > 0) {
                        String mapString = sidebarRows.get(0);
                        currentMap = mapString.split("MAP: ")[1];
                        currentMap = capitalizeString(currentMap);
                        LOGGER.info("MCCI: Current map: " + currentMap);
                        updateMapBestTime();
                    }
                });
            } else if (messageContent.contains("Game Over")) {
                LOGGER.info("MCCI: Game Ended!");
                var scoreboardOptional = ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance());
                LOGGER.info("MCCI: Scoreboard exists: " + scoreboardOptional.isPresent());
                scoreboardOptional.ifPresent(scoreboard -> {

                    String username = MinecraftClient.getInstance().getSession().getUsername();
                    var sidebarRows = ScoreboardUtil.getSidebarRows(scoreboard);
                    LOGGER.info("MCCI: Searching for placement text in scoreboard");
                    for (String s : sidebarRows) {
                        if (s.contains(username)) {
                            String placementText = s.split("\uE00A\uE006\uE004")[1];
                            int finalPlacement = extractNumberFromText(placementText);
                            lastPlacement = finalPlacement;
                            LOGGER.info("Game placement: " + finalPlacement);
                            lastPlacements.add(finalPlacement);
                            var stats = lastPlacements.stream().mapToDouble(p -> p).summaryStatistics();
                            gamePlacementAverage = stats.getAverage();
                            break;
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onTitleChange(String title) {
        if(title.contains("Round") || title.contains("TGTTOS")) {
            LOGGER.info("MCCI: Searching for map trigger from Title Change");
            ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance()).ifPresent(scoreboard -> {

                var sidebarRows =ScoreboardUtil.getSidebarRows(scoreboard);
                if(sidebarRows.size() > 0) {
                    String mapString = sidebarRows.get(0);
                    var split = mapString.split("MAP: ");
                    if(split.length > 2) {
                        currentMap = mapString.split("MAP: ")[1];
                        currentMap = capitalizeString(currentMap);
                        gamePlacementAverage = this.lastPlacements.stream().mapToDouble(p -> p).summaryStatistics().getAverage();
                        roundAveragePlacements = this.lastRoundPlacements.stream().mapToDouble(p -> p).summaryStatistics().getAverage();
                        LOGGER.info("MCCI: Current map: " + currentMap);
                    } else {
                        LOGGER.forceWarn("Scoreboard line isn't applicable yet! doesn't include the word the map yet! The string: '" + mapString + "'");
                    }
                    updateMapBestTime();
                }
            });
        }
    }

    public void updateMapBestTime() {
        LOGGER.info("MCCI: Updating visual best time for map: " + currentMap);
        bestCurrentMapTime = bestMapTimes.getOrDefault(currentMap, 0.0);
    }

    @Override
    public String displayData() {
        return "Last game placement: " + lastPlacement + "\n" +
                "Avg. game placement: " + twoDigitFormat.format(gamePlacementAverage) + " \n" +
                "Avg. round placement (Overall): " + twoDigitFormat.format(roundAveragePlacements) + " \n"  +
                "Best time for " + currentMap + ": " + formatTime(bestCurrentMapTime);
    }

    @Override
    public String previewUI() {
        return "Last game placement: 2 \n" +
                "Avg. game placement: 5.32 \n" +
                "Avg. round placement (Overall): 8.23 \n"  +
                "Best time for Air Train: " + "00:32.456";
    }

    public String capitalizeString(String text) {
        var words = text.split(" ");

        return Arrays.stream(words).map(word -> {
            String restOfWord = word.substring(1);
            return Character.toUpperCase(word.charAt(0)) + restOfWord.toLowerCase();
        }).collect(Collectors.joining(" "));
    }

    @Override
    public void loadFailSafeDefaultData() {
        this.lastRoundPlacements = new ArrayList<>();
        this.lastPlacements = new ArrayList<>();
        this.bestMapTimes = new HashMap<>();
        this.lastPlacement = 0;
    }

    @Override
    public void deserializeData(JsonObject jsonObject) {

        Gson gson = new Gson();
        var hashmapType = new TypeToken<HashMap<String, Double>>(){}.getType();
        var integerlist = new TypeToken<ArrayList<Integer>>(){}.getType();
        this.lastRoundPlacements = gson.fromJson(jsonObject.get("round_placements"), integerlist);
        this.lastPlacements = gson.fromJson(jsonObject.get("placements"), integerlist);
        this.lastPlacement = jsonObject.get("last_placement").getAsInt();
        this.bestMapTimes = gson.fromJson(jsonObject.get("best_map_times").getAsString(), hashmapType);

        this.roundAveragePlacements = lastRoundPlacements.stream().mapToDouble(p -> p).summaryStatistics().getAverage();
        this.gamePlacementAverage = lastPlacements.stream().mapToDouble(p -> p).summaryStatistics().getAverage();
    }

    @Override
    public JsonObject serializeData() {
        JsonObject jsonObject = new JsonObject();
        Gson gson = new Gson();

        var hashmapType = new TypeToken<HashMap<String, Double>>(){}.getType();
        jsonObject.addProperty("best_map_times", gson.toJson(bestMapTimes, hashmapType));
        jsonObject.add("round_placements", gson.toJsonTree(lastRoundPlacements).getAsJsonArray());
        jsonObject.add("placements", gson.toJsonTree(lastPlacements).getAsJsonArray());
        jsonObject.addProperty("last_placement", lastPlacement);
        return jsonObject;
    }

    @Override
    public String getSidebarIdentifier() {
        return "TGTTOS";
    }
}
