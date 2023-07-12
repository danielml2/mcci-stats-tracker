package me.danielml.games.minigames;

import me.danielml.games.Game;
import me.danielml.util.ScoreboardUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import javax.print.DocFlavor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static me.danielml.MCCIStats.LOGGER;

public class ParkourWarriorSurvivor extends Game  {

    private int lastPlacement;
    private double averagePlacement;
    private ArrayList<Integer> lastPlacements;

    private int[] leapPlacementsInCurrentGame;
    private double averageLeapPlacementsInCurrentGame;
    private ArrayList<Double>[] timesPerLeap;
    private int currentLeap = 1;
    private double currentLeapAverage, currentLeapBest;
    private boolean eliminated = false;

    public ParkourWarriorSurvivor() {
        lastPlacement = 0;
        averagePlacement = 0;
        lastPlacements = new ArrayList<>();
        this.averageLeapPlacementsInCurrentGame = 0;
        this.leapPlacementsInCurrentGame = new int[]{-1,-1,-1,-1,-1,-1,-1,-1};
        this.timesPerLeap = new ArrayList[8];
        this.currentLeapBest = 0;
    }

    @Override
    public void loadData() {

    }

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

            String[] timerSplit = messageContent.split("complete in: ");
            LOGGER.info("Timer Split: " + Arrays.toString(timerSplit));
            String[] minutesAndSecondsSplit = timerSplit[1].split(":");
            LOGGER.info("Minutes And Seconds Split: " + Arrays.toString(minutesAndSecondsSplit));

            double minutes = Double.parseDouble(minutesAndSecondsSplit[0].replaceAll(" ", ""));
            String[] secondsSeparation = minutesAndSecondsSplit[1].split(" \\[");
            LOGGER.info("Seconds Separation: " + Arrays.toString(secondsSeparation));
            double seconds = Double.parseDouble(secondsSeparation[0].replaceAll(" ", ""));

            double finalTimeInSeconds = minutes * 60 + seconds;
            int leapNumber = extractNumberFromText(messageContent.split("Leap")[1]);

            if(timesPerLeap[leapNumber-1] != null)
                timesPerLeap[leapNumber-1].add(finalTimeInSeconds);
            else {
                timesPerLeap[leapNumber-1] = new ArrayList<Double>();
                timesPerLeap[leapNumber-1].add(finalTimeInSeconds);
            }

            LOGGER.info("Leap " + leapNumber + " ended at: " + finalTimeInSeconds + " seconds");
            LOGGER.info("Formatted leap time: " + formatTime(finalTimeInSeconds));

        } else if(messageContent.contains("Stand by for the game") && messageContent.startsWith("[\uE075]")) {
            currentLeap = 1;
            updateCurrentLeapStats();
            leapPlacementsInCurrentGame = new int[]{-1,-1,-1,-1,-1,-1,-1,-1};
            eliminated = false;

        } else if(messageContent.contains("Leap") && messageContent.contains("started") && messageContent.startsWith("[\uE075]") && !eliminated) {
            currentLeap = extractNumberFromText(messageContent.split("Leap")[1]);
            LOGGER.info("Leap " + currentLeap + " started!");
            updateCurrentLeapStats();
        }

    }

    @Override
    public void onSidebarUpdate(List<String> sidebarRows) {
        for(int i = 0; i < currentLeap; i++) {
            if(leapPlacementsInCurrentGame[i] == -1) {
                int placement = getLeapPlacementFromSidebar(i+1);
                if(placement != -1)
                {
                    LOGGER.info("Found placement for leap " + (i+1) + "!");
                    leapPlacementsInCurrentGame[i] = placement;
                    var gameAvgPlacementsStats = Arrays.stream(leapPlacementsInCurrentGame).filter(p -> p != -1).summaryStatistics();
                    LOGGER.info("Current Game Leap Array: " + Arrays.toString(leapPlacementsInCurrentGame));
                    averageLeapPlacementsInCurrentGame = gameAvgPlacementsStats.getAverage();
                    LOGGER.info("Current Game Leap Average: " + averageLeapPlacementsInCurrentGame);
                    updateCurrentLeapStats();
                } else {
                    LOGGER.info("No leap placement available yet for leap " + (i+1));
                }
            }

        }
    }

    @Override
    public String displayData() {
        return "Last Placement: " + lastPlacement + "\n " +
                "Average Game Placement: " + ((int)averagePlacement) + "(" + twoDigitFormat.format(averagePlacement) + ") \n " +
                "Average Leap Placement (In current game): " + ((int) averageLeapPlacementsInCurrentGame) + "(" + twoDigitFormat.format(averageLeapPlacementsInCurrentGame) + ") \n " +
                "Current Leap (" + currentLeap + ") Average Time: " + formatTime(currentLeapAverage) + " \n" +
                "Current Leap (" + currentLeap + ") Best Time: " + formatTime(currentLeapBest);
    }

    @Override
    public String getSidebarIdentifier() {
        return "PARKOUR WARRIOR";
    }

    public void updateCurrentLeapStats() {
        if(timesPerLeap[currentLeap-1] != null){
            LOGGER.info("Updated current leap stats for Leap " + currentLeap);
            DoubleSummaryStatistics stats = timesPerLeap[currentLeap-1].stream().mapToDouble(Double::doubleValue).summaryStatistics();
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

}
