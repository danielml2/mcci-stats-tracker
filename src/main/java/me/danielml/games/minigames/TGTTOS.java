package me.danielml.games.minigames;

import me.danielml.games.Game;
import me.danielml.util.ScoreboardUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ai.goal.TrackIronGolemTargetGoal;
import net.minecraft.text.Text;

import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

    public TGTTOS() {
        this.lastPlacement = 0;
        this.lastPlacements = new ArrayList<>();
        this.lastRoundPlacements = new ArrayList<>();
        bestMapTimes = new HashMap<>();

        this.gamePlacementAverage = 0;
        this.roundAveragePlacements = 0;
        this.currentMap = "";
    }

    @Override
    public void onChatMessageInGame(Text messageText) {
        String messageContent = messageText.getString();

        if(messageContent.startsWith("[\uE000]")) {

            if (messageContent.contains("you finished the round and came in")) {
                int placement = extractNumberFromText(messageContent.split("came in")[1]);
                lastRoundPlacements.add(placement);
                var stats = lastRoundPlacements.stream().mapToDouble(p -> p).summaryStatistics();
                roundAveragePlacements = stats.getAverage();
                roundTime = System.currentTimeMillis() - roundTime;

                double timeInSeconds = (double) roundTime / 1000;
                if (!bestMapTimes.containsKey(currentMap))
                    bestMapTimes.put(currentMap, timeInSeconds);
                else {
                    double oldTime = bestMapTimes.get(currentMap);
                    bestMapTimes.replace(currentMap, Math.min(timeInSeconds, oldTime));
                }
                updateMapBestTime();

            }
        } else if(messageContent.startsWith("[\uE07A]")) {
             if(messageContent.contains("Round") && messageContent.contains("started!")) {
                roundTime = System.currentTimeMillis();
                ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance()).ifPresent(scoreboard -> {

                    var sidebarRows =ScoreboardUtil.getSidebarRows(scoreboard);
                    if(sidebarRows.size() > 0) {
                        String mapString = sidebarRows.get(0);
                        currentMap = mapString.split("MAP: ")[1];
                        currentMap = capitalizeString(currentMap);
                        LOGGER.info("Current map: " + currentMap);
                        updateMapBestTime();
                    }
                });
            } else if(messageContent.contains("Game Over")) {
                 ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance()).ifPresent(scoreboard -> {

                     String username = MinecraftClient.getInstance().getSession().getUsername();
                     var sidebarRows =ScoreboardUtil.getSidebarRows(scoreboard);

                     for(String s: sidebarRows) {
                         if(s.contains(username)) {
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
        if(title.contains("Round")) {
            ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance()).ifPresent(scoreboard -> {

                var sidebarRows =ScoreboardUtil.getSidebarRows(scoreboard);
                if(sidebarRows.size() > 0) {
                    String mapString = sidebarRows.get(0);
                    currentMap = mapString.split("MAP: ")[1];
                    currentMap = capitalizeString(currentMap);
                    LOGGER.info("Current map: " + currentMap);
                    updateMapBestTime();
                }
            });
        }
    }

    public void updateMapBestTime() {
        bestCurrentMapTime = bestMapTimes.getOrDefault(currentMap, 0.0);
    }

    @Override
    public String displayData() {
        return "Last game placement: " + lastPlacement + "\n" +
                "Average game placement: " + twoDigitFormat.format(gamePlacementAverage) + " \n" +
                "Average round placement (Overall): " + twoDigitFormat.format(roundAveragePlacements) + " \n"  +
                "Best time for current map (" + currentMap + "): " + formatTime(bestCurrentMapTime);
    }

    public String capitalizeString(String text) {
        var words = text.split(" ");

        return Arrays.stream(words).map(word -> {
            String restOfWord = word.substring(1);
            return Character.toUpperCase(word.charAt(0)) + restOfWord.toLowerCase();
        }).collect(Collectors.joining(" "));
    }

    @Override
    public String getSidebarIdentifier() {
        return "TGTTOS";
    }
}
