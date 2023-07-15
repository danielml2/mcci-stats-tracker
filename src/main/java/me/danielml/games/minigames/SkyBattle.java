package me.danielml.games.minigames;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.danielml.games.Game;
import me.danielml.util.ScoreboardUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.objectweb.asm.TypeReference;

import java.util.ArrayList;
import static me.danielml.MCCIStats.LOGGER;

public class SkyBattle extends Game {

    private int lastSurvivorPlacement;
    private int lastPersonalPlacement;
    private int lastTeamPlacement;

    private ArrayList<Integer> personalPlacements;
    private ArrayList<Integer> teamPlacements;
    private ArrayList<Integer> survivorPlacements;

    private int kills, deaths;

    private double averageSurvivorPlacement, averageTeamPlacement, averagePersonalPlacement;
    private double kdr;
    private int startingPlayerAmount;

    @Override
    public void onChatMessageInGame(Text messageText) {
        String messageContent = messageText.getString();

        String username = MinecraftClient.getInstance().getSession().getUsername();
        if(messageContent.startsWith("[\uE202]")) {
            if(messageContent.contains(username) && !messageContent.startsWith("[\uE202] \uE229 " + username)) {
                kills += 1;
                LOGGER.info("Kill detected!");
                if(deaths == 0)
                    kdr = kills;
                else
                    kdr = (double) kills / deaths;
                LOGGER.info("New KDR: " + kdr);
            }
        } else if(messageContent.startsWith("\uE016\uE00F") && messageContent.contains("Outlived")) {
            if(messageContent.contains("players")) {
                LOGGER.info("Detected a death!");
                deaths += 1;
                kdr = (double) kills / deaths;

                lastSurvivorPlacement = startingPlayerAmount - extractNumberFromText(messageContent.split("Outlived")[1]);
                survivorPlacements.add(lastSurvivorPlacement);

                averageSurvivorPlacement = survivorPlacements.stream()
                        .mapToDouble(p -> p)
                        .summaryStatistics()
                        .getAverage();
            } else if(messageContent.contains("team")) {
                lastTeamPlacement = 8 - extractNumberFromText(messageContent.split("Outlived")[1]);
                teamPlacements.add(lastTeamPlacement);
                averageTeamPlacement = teamPlacements.stream()
                        .mapToDouble(p -> p)
                        .summaryStatistics()
                        .getAverage();
            }
        } else if(messageContent.startsWith("[\uE079]")) {
            if(messageContent.contains("Game Over")) {
                ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance()).ifPresent(scoreboard -> {

                    var sidebarRows = ScoreboardUtil.getSidebarRows(scoreboard);
                    for(String row : sidebarRows) {
                        if(row.contains(username)) {
                            String personalText = row.split("\uE00E")[1];
                            lastPersonalPlacement = extractNumberFromText(personalText);
                            personalPlacements.add(lastPersonalPlacement);

                            averagePersonalPlacement = personalPlacements
                                    .stream()
                                    .mapToDouble(p -> p)
                                    .summaryStatistics()
                                    .getAverage();
                            break;
                        }
                    }
                });
            } else if(messageContent.contains("Game Started")) {
                ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance()).ifPresent(scoreboard -> {

                    var sidebarRows = ScoreboardUtil.getSidebarRows(scoreboard);
                    if(sidebarRows.size() >= 9) {

                        String playersText = sidebarRows.get(8);
                        startingPlayerAmount = extractNumberFromText(playersText.split(":")[1]);
                    }
                });
            } else if(messageContent.startsWith("[") && messageContent.contains("you survived")) {
                lastTeamPlacement = 1;
                teamPlacements.add(lastTeamPlacement);
                lastSurvivorPlacement = 1;
                survivorPlacements.add(lastSurvivorPlacement);
                averageTeamPlacement = teamPlacements.stream().mapToDouble(p -> p).summaryStatistics().getAverage();
            }
        }
        super.onChatMessageInGame(messageText);
    }


    @Override
    public String displayData() {
        return "Latest placements for Survivor: " + lastSurvivorPlacement + " Personal: " + lastPersonalPlacement + " \n" +
                " Team: " + lastTeamPlacement + " \n" +
                "Avg. Survivor Placement " + twoDigitFormat.format(averageSurvivorPlacement) + " \n" +
                "Avg. Personal Placement: " + twoDigitFormat.format(averagePersonalPlacement) + " \n"
                + "Avg. Team placement: " + twoDigitFormat.format(averageTeamPlacement) + " \n" +
                "KDR: " + twoDigitFormat.format(kdr) + " (Kills: " + kills + " Deaths: " + deaths + ")";

    }

    @Override
    public void loadFailSafeDefaultData() {
        this.kills = 0;
        this.deaths = 0;
        this.kdr = 0;
        this.teamPlacements = new ArrayList<>();
        this.survivorPlacements = new ArrayList<>();
        this.personalPlacements = new ArrayList<>();
        this.lastTeamPlacement = 0;
        this.lastPersonalPlacement = 0;
        this.lastSurvivorPlacement= 0;
    }

    @Override
    public JsonObject serializeData() {
        JsonObject jsonObject = new JsonObject();

        Gson gson = new Gson();

        jsonObject.addProperty("kills", kills);
        jsonObject.addProperty("deaths", deaths);
        jsonObject.addProperty("kdr", kdr);
        jsonObject.addProperty("last_survivor_placement", lastSurvivorPlacement);
        jsonObject.addProperty("last_team_placement", lastTeamPlacement);
        jsonObject.addProperty("last_personal_placement", lastPersonalPlacement);

        var survivorPlacementsJSON = gson.toJsonTree(survivorPlacements).getAsJsonArray();
        jsonObject.add("survivor_placements", survivorPlacementsJSON);
        var personalPlacementsJSON = gson.toJsonTree(personalPlacements).getAsJsonArray();
        jsonObject.add("personal_placements", personalPlacementsJSON);
        var teamPlacementsJSON = gson.toJsonTree(teamPlacements).getAsJsonArray();
        jsonObject.add("team_placements", teamPlacementsJSON);

        return jsonObject;
    }

    @Override
    public void deserializeData(JsonObject jsonObject) {

        Gson gson = new Gson();

        this.kills = jsonObject.get("kills").getAsInt();
        this.deaths = jsonObject.get("deaths").getAsInt();
        this.kdr = jsonObject.get("kdr").getAsDouble();
        this.lastSurvivorPlacement = jsonObject.get("last_survivor_placement").getAsInt();
        this.lastPersonalPlacement = jsonObject.get("last_personal_placement").getAsInt();
        this.lastTeamPlacement = jsonObject.get("last_team_placement").getAsInt();

        var type = new TypeToken<ArrayList<Integer>>(){}.getType();
        this.personalPlacements = gson.fromJson(jsonObject.get("personal_placements"), type);
        this.survivorPlacements = gson.fromJson(jsonObject.get("survivor_placements"), type);
        this.teamPlacements = gson.fromJson(jsonObject.get("team_placements"), type);
    }

    @Override
    public String getSidebarIdentifier() {
        return "SKY BATTLE";
    }
}
