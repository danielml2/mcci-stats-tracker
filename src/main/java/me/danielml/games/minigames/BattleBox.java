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

import static me.danielml.MCCIStats.LOGGER;

public class BattleBox extends Game {

    private int lastPersonalPlacement;
    private int lastTeamPlacement;
    private ArrayList<Integer> personalPlacements;
    private ArrayList<Integer> teamPlacements;
    private int kills;
    private int deaths;
    private int roundWins, roundLosses;
    private double roundWLR, averagePersonalPlacement, averageTeamPlacement;
    private double kdr;

    @Override
    public void onChatMessageInGame(Text messageText) {

        String messageContent = messageText.getString();
        String username = MinecraftClient.getInstance().getSession().getUsername();

        // Kill messages & Round lost messages
        if(messageContent.startsWith("[")) {
            var wordsSplit = messageContent.split(" ");
            boolean isDeath = wordsSplit.length >= 3 && wordsSplit[2].equalsIgnoreCase(username);
            LOGGER.info("Is death: " + isDeath);
            LOGGER.info(Arrays.toString(wordsSplit));

            // Double check just in case wording changes for some reason
            if(wordsSplit.length >= 3)
                for(int i = 0; i < 3; i++) {
                    if(wordsSplit[i].equalsIgnoreCase(username))
                    {
                        isDeath = true;
                        LOGGER.info("Found on index " + i);
                        break;
                    }

                }

            if(messageContent.contains(username) && !isDeath) {
                LOGGER.info("Detected a kill!");
                kills += 1;
                if(deaths == 0)
                    kdr = kills;
                else
                    kdr = (double) kills / deaths;
            } else if(messageContent.contains(username)) {
                deaths += 1;
                LOGGER.info("Detected a death!");
                kdr = (double) kills / deaths;
            } else if(messageContent.contains("you lost")) {
                roundLosses += 1;
                roundWLR = (double) roundWins / roundLosses;
                LOGGER.info("Round lost! New WLR: " + roundWLR);
            } else if(messageContent.contains("you won")) {
                roundWins += 1;
                if(roundLosses == 0)
                    roundWLR = roundWins;
                else
                    roundWLR = (double) roundWins / roundLosses;
                LOGGER.info("Round won! New WLR: " + roundWLR);
            } else if(messageContent.contains("Game Over")) {
                ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance()).ifPresent(scoreboard -> {

                    var sidebarRows = ScoreboardUtil.getSidebarRows(scoreboard);
                    int placementsFoundCount = 0;
                    for(String row: sidebarRows) {

                        if(row.contains("\uE00E\uE00A\uE006\uE004")) {
                            String teamText = row.split("\uE00E\uE00A\uE006\uE004")[1];
                            lastTeamPlacement = extractNumberFromText(teamText);
                            teamPlacements.add(lastTeamPlacement);
                            placementsFoundCount++;
                        } else if(row.contains(username)) {
                            String personalText = row.split("\uE00E")[1];
                            lastPersonalPlacement = extractNumberFromText(personalText);
                            personalPlacements.add(lastPersonalPlacement);
                            placementsFoundCount++;
                        }
                        if(placementsFoundCount >= 2)
                            break;
                    }

                    averagePersonalPlacement = personalPlacements.stream()
                            .mapToDouble(p -> p)
                            .summaryStatistics()
                            .getAverage();
                    averageTeamPlacement = teamPlacements.stream()
                            .mapToDouble(p -> p)
                            .summaryStatistics()
                            .getAverage();
                    LOGGER.info("New average personal: " + averagePersonalPlacement);
                    LOGGER.info("New average team: " + averageTeamPlacement);
                });
            }
        }
    }



    @Override
    public String displayData() {
        return "Last placements - Team: " + lastTeamPlacement + " Personal: " + lastPersonalPlacement + " \n" +
                "Avg. Personal Placement: " + twoDigitFormat.format(averagePersonalPlacement) + " \n" +
                "Avg. Team Placement: " +  twoDigitFormat.format(averageTeamPlacement) + " \n" +
                "KDR (Kill/Death Ratio): " + twoDigitFormat.format(kdr) + " (Kills: " + kills + " Deaths: " + deaths + ") \n" +
                "Round WLR (Win/Loss Ratio): " + twoDigitFormat.format(roundWLR) + " (Round wins: " + roundWins + " Losses: " + roundLosses + ")";
    }

    @Override
    public void loadFailSafeDefaultData() {
        this.kills = 0;
        this.deaths = 0;
        this.roundWins = 0;
        this.roundLosses = 0;
        this.roundWLR = 0;
        this.kdr = 0;
        this.personalPlacements = new ArrayList<>();
        this.teamPlacements = new ArrayList<>();
        this.lastTeamPlacement = 0;
        this.lastPersonalPlacement = 0;
    }

    @Override
    public JsonObject serializeData() {

        JsonObject jsonObject = new JsonObject();
        Gson gson = new Gson();

        jsonObject.addProperty("kills", kills);
        jsonObject.addProperty("deaths", deaths);
        jsonObject.addProperty("kdr", kdr);
        jsonObject.addProperty("last_team_placement", lastTeamPlacement);
        jsonObject.addProperty("last_personal_placement", lastPersonalPlacement);
        jsonObject.addProperty("round_wins", roundWins);
        jsonObject.addProperty("round_losses", roundLosses);
        jsonObject.addProperty("round_wlr", roundWLR);


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
        this.roundWins = jsonObject.get("round_wins").getAsInt();
        this.roundLosses= jsonObject.get("round_losses").getAsInt();
        this.roundWLR = jsonObject.get("round_wlr").getAsDouble();
        this.kdr = jsonObject.get("kdr").getAsDouble();
        this.lastPersonalPlacement = jsonObject.get("last_personal_placement").getAsInt();
        this.lastTeamPlacement = jsonObject.get("last_team_placement").getAsInt();

        var type = new TypeToken<ArrayList<Integer>>(){}.getType();
        this.personalPlacements = gson.fromJson(jsonObject.get("personal_placements"), type);
        this.teamPlacements = gson.fromJson(jsonObject.get("team_placements"), type);

        this.averagePersonalPlacement = personalPlacements.stream().mapToDouble(p -> p).summaryStatistics().getAverage();
        this.averageTeamPlacement = teamPlacements.stream().mapToDouble(p -> p).summaryStatistics().getAverage();
    }

    @Override
    public String getSidebarIdentifier() {
        return "BATTLE BOX";
    }
}
