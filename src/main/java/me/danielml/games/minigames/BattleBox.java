package me.danielml.games.minigames;

import com.google.gson.JsonObject;
import me.danielml.games.Game;
import me.danielml.util.ScoreboardUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;

import static me.danielml.MCCIStats.LOGGER;
import static me.danielml.MCCIStats.onScoreboardUpdate;

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

    public BattleBox() {
        this.kills = 0;
        this.deaths = 0;
        this.roundWins = 0;
        this.roundWLR = 0;
        this.averageTeamPlacement = 0;
        this.averagePersonalPlacement = 0;
        this.kdr = 0;
        this.personalPlacements = new ArrayList<>();
        this.teamPlacements = new ArrayList<>();
        this.lastTeamPlacement = 0;
        this.lastPersonalPlacement = 0;
    }
    @Override
    public void onChatMessageInGame(Text messageText) {

        String messageContent = messageText.getString();
        String username = MinecraftClient.getInstance().getSession().getUsername();

        // Kill messages & Round lost messages
        if(messageContent.startsWith("[\uE202]")) {
            if(messageContent.contains("by \uE229 " + username)) {
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
            }
        }
        // Game Over prefix
        else if(messageContent.startsWith("[\uE070]")) {
            if(messageContent.contains("Game Over")) {
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
        } else if(messageContent.startsWith("[\uE000]")) {
            if(messageContent.contains("you won")) {
                roundWins += 1;
                if(roundLosses == 0)
                    roundWLR = roundWins;
                else
                    roundWLR = (double) roundWins / roundLosses;
                LOGGER.info("Round won! New WLR: " + roundWLR);
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

    }

    @Override
    public JsonObject serializeData() {
        return new JsonObject();
    }

    @Override
    public void deserializeData(JsonObject jsonObject) {

    }

    @Override
    public String getSidebarIdentifier() {
        return "BATTLE BOX";
    }
}
