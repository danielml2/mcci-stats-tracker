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
import java.util.List;

import static me.danielml.MCCIStats.LOGGER;

public class RocketSpleefRush extends Game {

    private int kills, deaths;
    private double kdr;
    private int lastPlacement;
    private List<Integer> placements;
    private double longestTimeAliveSeconds;
    private double averagePlacement;

    private long startTime = 0;
    private boolean hasDied = false;

    @Override
    public void onChatMessageInGame(Text messageText) {
        String messageContent = messageText.getString();
        String username = MinecraftClient.getInstance().getSession().getUsername();


        if(messageContent.startsWith("[")) {
            if(messageContent.contains(username) && messageContent.contains("\uE1F1")) {
                var wordsSplit = messageContent.split(" ");
                boolean isDeath = wordsSplit.length >= 3 && wordsSplit[2].contains(username) && !messageContent.contains("Final duel");
                LOGGER.info("Is death: " + isDeath);
                LOGGER.info(Arrays.toString(wordsSplit));

                // Double check just in case wording changes for some reason
                if(wordsSplit.length >= 3)
                {
                    boolean foundUsername = false;
                    for(String word : wordsSplit) {
                        if(!foundUsername && word.contains(username))
                            foundUsername = true;
                        else if(foundUsername && (word.contains("was") || word.contains("died"))) {
                            isDeath = true;
                            break;
                        }
                    }
                }
                if(!isDeath && !hasDied) {
                    LOGGER.info("Detected a kill!");
                    kills += 1;
                    if(deaths == 0)
                        kdr = kills;
                    else
                        kdr = (double) kills / deaths;
                } else {
                    LOGGER.info("Detected a death!");
                    hasDied = true;
                    updateLongestTimeAlive();
                    deaths += 1;
                    kdr = (double) kills / deaths;
                    ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance()).ifPresent(scoreboard -> {
                        var sidebarRows = ScoreboardUtil.getSidebarRows(scoreboard);
                        for (String row : sidebarRows) {
                            if (row.contains("PLAYERS REMAINING")) {
                                lastPlacement = extractNumberFromText(row.split(":")[1]) + 1;
                                placements.add(lastPlacement);
                                averagePlacement = placements.stream().mapToDouble(p -> p).summaryStatistics().getAverage();
                                LOGGER.info("Player placement: " + lastPlacement);
                                break;
                            }
                        }
                    });
                }
            } else if (messageContent.contains("Game Started") || messageContent.contains("Stand by for the game to begin")) {
                hasDied = false;
                startTime = System.currentTimeMillis();
            } else if(messageContent.contains("Game Over")) {
                if(!hasDied)
                {
                    LOGGER.info("yippie you won!");
                    updateLongestTimeAlive();
                    lastPlacement = 1;
                    placements.add(lastPlacement);
                    averagePlacement = placements.stream().mapToDouble(p -> p).summaryStatistics().getAverage();
                }
            }
        }
    }

    public void updateLongestTimeAlive() {
        long timeAlive = System.currentTimeMillis() - startTime;
        double timeAliveInSeconds = (double) timeAlive / 1000;
        LOGGER.info("Previous longest time: " + longestTimeAliveSeconds);
        longestTimeAliveSeconds = Math.max(longestTimeAliveSeconds, timeAliveInSeconds);
        LOGGER.info("New longest time: " + longestTimeAliveSeconds);
    }

    @Override
    public String displayData() {
        return "Longest time alive: " + formatTime(longestTimeAliveSeconds) + " \n" +
                "Last placement: " + lastPlacement + " \n" +
                "Avg. Team Placement: " +  twoDigitFormat.format(averagePlacement) + " \n" +
                "KDR (Kill/Death Ratio): " + twoDigitFormat.format(kdr) + " (Kills: " + kills + " Deaths: " + deaths + ") \n";
    }

    @Override
    public String previewUI() {
        return "Longest time alive: 1:09 \n" +
                "Last placement: 3 \n" +
                "Avg. Team Placement: 6.5 \n" +
                "KDR (Kill/Death Ratio): 1.5 (Kills: 3 Deaths: 2) \n";
    }

    @Override
    public JsonObject serializeData() {
        JsonObject jsonObject = new JsonObject();
        Gson gson = new Gson();

        jsonObject.addProperty("kills", kills);
        jsonObject.addProperty("deaths", deaths);
        jsonObject.addProperty("kdr", kdr);
        jsonObject.addProperty("last_placement", lastPlacement);
        jsonObject.addProperty("longest_time_alive", longestTimeAliveSeconds);
        var placementsJSON = gson.toJsonTree(placements).getAsJsonArray();
        jsonObject.add("placements", placementsJSON);


        return jsonObject;
    }

    @Override
    public void deserializeData(JsonObject jsonObject) {
        Gson gson = new Gson();

        this.kills = jsonObject.get("kills").getAsInt();
        this.deaths = jsonObject.get("deaths").getAsInt();
        this.kdr = jsonObject.get("kdr").getAsDouble();
        this.lastPlacement = jsonObject.get("last_placement").getAsInt();
        this.longestTimeAliveSeconds = jsonObject.get("longest_time_alive").getAsDouble();

        var type = new TypeToken<ArrayList<Integer>>(){}.getType();
        this.placements = gson.fromJson(jsonObject.get("placements"), type);
        this.averagePlacement = placements.stream().mapToDouble(p -> p).summaryStatistics().getAverage();
    }

    @Override
    public void loadFailSafeDefaultData() {
        kills = 0;
        deaths = 0;
        kdr = 0;
        lastPlacement = 0;
        placements = new ArrayList<>();
        averagePlacement = 0;
        longestTimeAliveSeconds = 0;
    }

    @Override
    public String getSidebarIdentifier() {
        return "ROCKET SPLEEF RUSH";
    }
}
