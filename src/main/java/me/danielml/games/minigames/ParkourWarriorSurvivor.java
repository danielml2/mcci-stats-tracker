package me.danielml.games.minigames;

import me.danielml.games.Game;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;

import static me.danielml.MCCIStats.LOGGER;

public class ParkourWarriorSurvivor extends Game  {

    private int lastPlacement;
    private double averagePlacement;
    private ArrayList<Integer> lastPlacements;

    private ArrayList<Double> averageLeapPlacementsInGame;
    private ArrayList<ArrayList<Double>> timesPerLeap;

    @Override
    public void loadData() {
        lastPlacement = 0;
        averagePlacement = 0;
        lastPlacements = new ArrayList<>();
        timesPerLeap = new ArrayList<>();

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
        }

    }

    @Override
    public String displayData() {
        return "Last Placement: " + lastPlacement + "\n " +
                "Average Placement: " + ((int)averagePlacement) + "( " + twoDigitFormat.format(averagePlacement) + ") \n " +
                "";
    }

    @Override
    public String getSidebarIdentifier() {
        return "PARKOUR WARRIOR";
    }
}
