package me.danielml.games.minigames;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.danielml.games.Game;
import me.danielml.util.ScoreboardUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import static me.danielml.MCCIStats.LOGGER;

import java.util.ArrayList;

public class Dynaball extends Game {

    private double averagePersonalPlacement;
    private ArrayList<Integer> personalPlacements;

    private double longestTimeSurvivedInSeconds;
    private int wins, losses;
    private double wlr;
    private int kills, deaths;
    private double kdr;
    private boolean isDead = false;
    private long currentGameTime;


    @Override
    public void onChatMessageInGame(Text messageText) {
        String messageContent = messageText.getString();

        // Checks if the message is a mini-game message or not
        if(!messageContent.startsWith("["))
            return;

        String username = MinecraftClient.getInstance().getSession().getUsername();

        if(messageContent.contains("Game Started!")) {
            currentGameTime = System.currentTimeMillis();
            isDead = false;
        } else if(messageContent.contains(username)) {
            if(messageContent.contains("you were eliminated")) {
                LOGGER.info("Death detected!");
                deaths += 1;
                isDead = true;
                updateLongestTimeLived();
            } else if(!isDead && !messageContent.contains("you finished")){
                LOGGER.info("Detected a kill!");
                kills += 1;
            }
            if(deaths > 0)
                kdr = (double) kills / deaths;
            else
                kdr = kills;
        } else if(messageContent.contains("Team, you finished 1st!")) {
            wins += 1;
            if(losses > 0)
                wlr = (double) wins / losses;
            else
                wlr = wins;
            updateLongestTimeLived();
        } else if(messageContent.contains("Team, you were eliminated.")) {
            losses += 1;
            wlr = (double) wins / losses;
        } else if(messageContent.contains("Game Over!")) {
            // We call for the update here instead of the win/loss messages to ensure the scoreboard updates to the correct ranking
            updateAveragePersonalPlacement(username);
        }


    }


    public void updateLongestTimeLived() {
        double aliveTimeInSeconds = (double) (System.currentTimeMillis() - currentGameTime) / 1000;
        LOGGER.info("Previous longest time survived: " + longestTimeSurvivedInSeconds);
        longestTimeSurvivedInSeconds = Math.max(longestTimeSurvivedInSeconds, aliveTimeInSeconds);
        LOGGER.info("New longest time survived: " + longestTimeSurvivedInSeconds);
    }

    public void updateAveragePersonalPlacement(String username) {
        ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance()).ifPresent(scoreboard -> {

            var sidebarRows = ScoreboardUtil.getSidebarRows(scoreboard);
            for (String row : sidebarRows) {
                if (row.contains(username)) {
                    String personalText = row.split("\uE00E")[1];
                    int personalPlacement = extractNumberFromText(personalText);
                    personalPlacements.add(personalPlacement);

                    averagePersonalPlacement = personalPlacements
                            .stream()
                            .mapToDouble(p -> p)
                            .summaryStatistics()
                            .getAverage();
                    break;
                }
            }
        });
    }

    @Override
    public String displayData() {
        return "WLR: " + twoDigitFormat.format(wlr) + " (Wins: " + wins + " Losses: " + losses + ")" + "\n" +
                "KDR: " + twoDigitFormat.format(kdr) + " (Kills: " + kills + " Deaths: " + deaths + ") \n" +
                "Avg. Personal Placement: " + twoDigitFormat.format(averagePersonalPlacement) + " \n" +
                "Longest time survived: " + formatTime(longestTimeSurvivedInSeconds) + " \n";
    }

    @Override
    public String previewUI() {
        return "WLR: 0.5 (Wins: 50 Losses: 100) \n" +
                "KDR: 0.94 (Kills: 80 Deaths: 85) \n" +
                "Avg. Personal Placement: 4.52 \n" +
                "Longest time survived: 04:06 \n";
    }

    @Override
    public JsonObject serializeData() {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("kills", kills);
        jsonObject.addProperty("deaths", deaths);
        jsonObject.addProperty("kdr", kdr);
        jsonObject.addProperty("wins", wins);
        jsonObject.addProperty("losses", losses);
        jsonObject.addProperty("wlr", wlr);
        jsonObject.addProperty("longest_time_alive", longestTimeSurvivedInSeconds);

        var personalPlacementsJSON = gson.toJsonTree(personalPlacements).getAsJsonArray();
        jsonObject.add("personal_placements", personalPlacementsJSON);


        return jsonObject;
    }

    @Override
    public void deserializeData(JsonObject jsonObject) {
        Gson gson = new Gson();

        kills = jsonObject.get("kills").getAsInt();
        deaths = jsonObject.get("deaths").getAsInt();
        kdr = jsonObject.get("kdr").getAsDouble();
        wins = jsonObject.get("wins").getAsInt();
        losses = jsonObject.get("losses").getAsInt();
        wlr = jsonObject.get("wlr").getAsDouble();
        longestTimeSurvivedInSeconds = jsonObject.get("longest_time_alive").getAsDouble();

        var type = new TypeToken<ArrayList<Integer>>(){}.getType();
        this.personalPlacements = gson.fromJson(jsonObject.get("personal_placements"), type);
    }

    @Override
    public void loadFailSafeDefaultData() {
        averagePersonalPlacement = 0;
        kills = 0;
        deaths = 0;
        kdr = 0;
        wins = 0;
        losses = 0;
        wlr = 0;
        longestTimeSurvivedInSeconds = 0;
        personalPlacements = new ArrayList<>();
    }

    @Override
    public String getSidebarIdentifier() {
        return "DYNABALL";
    }
}
