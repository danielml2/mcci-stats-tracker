package me.danielml.games.minigames;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.sun.jna.platform.win32.OaIdl;
import me.danielml.games.Game;
import me.danielml.util.ScoreboardUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.danielml.MCCIStats.LOGGER;

public class ParkourWarriorSurvivor extends Game  {

    private int lastPlacement;
    private double averagePlacement;
    private ArrayList<Integer> lastPlacements;

    private int[] leapPlacementsInCurrentGame;
    private double averageLeapPlacementsInCurrentGame;
    private ArrayList<Double>[] timesPerLeap;
    private int currentPlayerLeap = 1;
    private int currentGameLeap = 1;
    private int playerCompletedLeaps = 0;
    private long unfinishedLeapTime = 0;
    private double currentLeapAverage, currentLeapBest;
    private boolean eliminated = false;


    @Override
    public void onChatMessageInGame(Text messageText) {
        String messageContent = messageText.getString();

        // The icon at the start is the skull emoji in MCCI font
        if(messageContent.contains("you were eliminated in ") && messageContent.startsWith("[\uE202]")) {
            LOGGER.info("Starts with: " + messageContent.startsWith("[\uE202]"));
            String[] split = messageContent.split("you were eliminated in");
            LOGGER.info("Split: " + Arrays.toString(split));

            String placementText = messageContent.split("you were eliminated in")[1];
            int placement = extractNumberFromText(placementText);
            lastPlacement = placement;
            lastPlacements.add(placement);

            DoubleSummaryStatistics stats = lastPlacements.stream().mapToDouble(p -> p).summaryStatistics();
            averagePlacement = stats.getAverage();
            LOGGER.info("Eliminated at placement: " + placement);
            eliminated = true;

        } else if(messageContent.contains("Leap") && messageContent.contains("complete in") && messageContent.startsWith("[\uE000]")) {

            double finalTimeInSeconds;

            int leapNumber = extractNumberFromText(messageContent.split("Leap")[1]);
            playerCompletedLeaps = leapNumber;


            // Finished after time ran out
            if(playerCompletedLeaps < currentGameLeap) {
                unfinishedLeapTime = System.currentTimeMillis() - unfinishedLeapTime;
                finalTimeInSeconds = (double) unfinishedLeapTime / 1000;
                LOGGER.info("Finished outside of player/time limit, recalculated time is: " + finalTimeInSeconds);
                LOGGER.info("Re-started unfinished time in the case they don't finish on time (again)");
                unfinishedLeapTime = System.currentTimeMillis(); // Started timer for the next leap the player is catching up on.
                currentPlayerLeap = leapNumber + 1;
            } else {
                // If finished in time, use the message for the time
                String[] timerSplit = messageContent.split("complete in: ");
                LOGGER.info("Timer Split: " + Arrays.toString(timerSplit));
                String[] minutesAndSecondsSplit = timerSplit[1].split(":");
                LOGGER.info("Minutes And Seconds Split: " + Arrays.toString(minutesAndSecondsSplit));

                double minutes = Double.parseDouble(minutesAndSecondsSplit[0].replaceAll(" ", ""));
                String[] secondsSeparation = minutesAndSecondsSplit[1].split(" \\[");
                LOGGER.info("Seconds Separation: " + Arrays.toString(secondsSeparation));
                double seconds = Double.parseDouble(secondsSeparation[0].replaceAll(" ", ""));

                finalTimeInSeconds = minutes * 60 + seconds;
            }

            if(timesPerLeap[leapNumber-1] != null)
                timesPerLeap[leapNumber-1].add(finalTimeInSeconds);
            else {
                timesPerLeap[leapNumber-1] = new ArrayList<Double>();
                timesPerLeap[leapNumber-1].add(finalTimeInSeconds);
            }

            if(leapNumber == 8)
            {
                lastPlacement = 1;
                lastPlacements.add(lastPlacement);
                DoubleSummaryStatistics stats = lastPlacements.stream().mapToDouble(p -> p).summaryStatistics();
                averagePlacement = stats.getAverage();
                LOGGER.info("Won the game!");
            }

            LOGGER.info("Leap " + leapNumber + " ended at: " + finalTimeInSeconds + " seconds");
            LOGGER.info("Formatted leap time: " + formatTime(finalTimeInSeconds));
            updateCurrentLeapStats();

        } else if(messageContent.contains("Stand by for the game") && messageContent.startsWith("[\uE075]")) {
            LOGGER.info("Reset screen for start of game!");
            currentPlayerLeap = 1;
            leapPlacementsInCurrentGame = new int[]{-1,-1,-1,-1,-1,-1,-1,-1};
            averageLeapPlacementsInCurrentGame = 0;
            playerCompletedLeaps = 1;
            currentGameLeap = 1;
            eliminated = false;
            updateCurrentLeapStats();
        } else if(messageContent.contains("Leap") && messageContent.contains("started") && messageContent.startsWith("[\uE075]") && !eliminated) {
            currentPlayerLeap = extractNumberFromText(messageContent.split("Leap")[1]);
            LOGGER.info("Leap " + currentPlayerLeap + " started!");
            unfinishedLeapTime = System.currentTimeMillis();
            updateCurrentLeapStats();

        } else if(messageContent.contains("Leap") && messageContent.contains("ended") && messageContent.startsWith("[\uE075]")) {
            // The leap ended message will ALWAYS send after you complete (if you complete in time),
            // only if you don't complete it in time the ended message will send BEFORE the complete message (requiring custom time instead of the message).
            int leapEnded = extractNumberFromText(messageContent.split("Leap")[1]);
            currentGameLeap = leapEnded + 1;
            LOGGER.info("Current Game Leap: " + currentGameLeap);
        }
    }

    @Override
    public void onSidebarUpdate(List<String> sidebarRows) {
        if(!eliminated)
            for(int i = 0; i < playerCompletedLeaps; i++) {
                if(leapPlacementsInCurrentGame[i] == -1) {
                    int placement = getLeapPlacementFromSidebar(i+1);
                    if(placement != -1)
                    {
                        LOGGER.info("Found placement for leap " + (i+1) + "!");
                        leapPlacementsInCurrentGame[i] = placement;
                        var gameAvgPlacementsStats = Arrays.stream(leapPlacementsInCurrentGame).filter(p -> p != -1).summaryStatistics();
                        LOGGER.info("Current Game Leap Placements Array: " + Arrays.toString(leapPlacementsInCurrentGame));
                        averageLeapPlacementsInCurrentGame = gameAvgPlacementsStats.getAverage();
                        LOGGER.info("Current Game Leap Placements Average: " + averageLeapPlacementsInCurrentGame);
                        updateCurrentLeapStats();
                    } else {
                        LOGGER.info("No leap placement available yet for leap " + (i+1));
                    }
                }

            }
    }

    @Override
    public void onTitleChange(String title) {
        if(title.contains("Leap 1")) {
            // Extra reset just in case it forgets for some reason and the standby message doesn't work for some reason
            LOGGER.info("Reset screen from title!");
            currentPlayerLeap = 1;
            leapPlacementsInCurrentGame = new int[]{-1,-1,-1,-1,-1,-1,-1,-1};
            averageLeapPlacementsInCurrentGame = 0;
            playerCompletedLeaps = 1;
            currentGameLeap = 1;
            eliminated = false;
            updateCurrentLeapStats();
        }
    }

    @Override
    public String displayData() {
        return "Last Placement: " + lastPlacement + "\n " +
                "Avg. Game Placement: " + ((int)averagePlacement) + " (" + twoDigitFormat.format(averagePlacement) + ") \n " +
                "Avg. Leap Placement (In current game): " + ((int) averageLeapPlacementsInCurrentGame) + " (" + twoDigitFormat.format(averageLeapPlacementsInCurrentGame) + ") \n " +
                "Leap " + currentPlayerLeap + " Avg. Time: " + formatTime(currentLeapAverage) + "\n" +
                "Leap " + currentPlayerLeap + " Best Time: " + formatTime(currentLeapBest);
    }

    @Override
    public String getSidebarIdentifier() {
        return "PARKOUR WARRIOR";
    }

    public void updateCurrentLeapStats() {
        if(timesPerLeap[currentPlayerLeap -1] != null){
            LOGGER.info("Updated current leap stats for Leap " + currentPlayerLeap);
            DoubleSummaryStatistics stats = timesPerLeap[currentPlayerLeap -1].stream().mapToDouble(Double::doubleValue).summaryStatistics();
            currentLeapAverage = stats.getAverage();
            currentLeapBest = stats.getMin();
        } else {
            currentLeapAverage = 0;
            currentLeapBest = 0;
        }
    }

    public int getLeapPlacementFromSidebar(int leapNum) {

        String leapRanksRows;
        var scoreboardOptional = ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance());
        if(scoreboardOptional.isEmpty())
            return -1;
        var scoreboard = scoreboardOptional.get();

        var sidebarRows = ScoreboardUtil.getSidebarRows(scoreboard);
        if(sidebarRows.size() < 2)
            return -1;

        // Rank rows show in Index 1 of the sidebar.
        leapRanksRows = sidebarRows.get(1);

        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(leapRanksRows);

        String[] results = matcher.results().map(MatchResult::group).toArray(String[]::new);

        if(results.length < leapNum)
            return -1;

        return Integer.parseInt(results[leapNum-1]);
    }

    @Override
    public void loadFailSafeDefaultData() {
        lastPlacements = new ArrayList<>();
        this.timesPerLeap = new ArrayList[8];
        this.lastPlacement = 0;
    }

    @Override
    public JsonObject serializeData() {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();

        var timesJSON = gson.toJsonTree(timesPerLeap).getAsJsonArray();
        var placementsJSON = gson.toJsonTree(lastPlacements).getAsJsonArray();
        jsonObject.add("leap_times", timesJSON);
        jsonObject.add("placements", placementsJSON);
        jsonObject.addProperty("last_placement", lastPlacement);

        return jsonObject;
    }

    @Override
    public void deserializeData(JsonObject jsonObject) {
        Gson gson = new Gson();
        var leapTimesType = new TypeToken<ArrayList<Double>[]>() {}.getType();
        var placementsType = new TypeToken<ArrayList<Integer>>() {}.getType();

        this.timesPerLeap = gson.fromJson(jsonObject.get("leap_times"), leapTimesType);
        this.lastPlacements = gson.fromJson(jsonObject.get("placements"), placementsType);
        this.lastPlacement = jsonObject.get("last_placement").getAsInt();
    }
}
