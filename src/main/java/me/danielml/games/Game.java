package me.danielml.games;



import net.minecraft.text.Text;

import java.text.DecimalFormat;
import java.util.List;

import static me.danielml.MCCIStats.LOGGER;

public abstract class Game {

    protected final DecimalFormat twoDigitFormat = new DecimalFormat("#.##");

    public void onChatMessageInGame(Text messageText) {}

    public void onTitleChange(String title) {}

    public void onSubtitleChange(String subtitle) {

    }

    public void onSidebarUpdate(List<String> sidebarRows) {}
    public void saveData() {}

    public void loadData() {}
    public abstract String displayData();

    public abstract String getSidebarIdentifier();


    public final int extractNumberFromText(String text) {
        int endIndex = 0;
        int startIndex = 0;

        String newText = text.replaceAll(" ", "");
        for(char ch : newText.toCharArray()) {
            if(ch == ' ') {
                startIndex++;
            } else if(!Character.isDigit(ch))
            {
                LOGGER.info("End of numbers! Found non-digit character: '" + ch + "'");
                break;
            }
            endIndex++;
        }

        return Integer.parseInt(newText.substring(startIndex, endIndex));
    }

    public final String formatTime(double timeInSeconds) {
        int minutes = (int) timeInSeconds / 60;
        double remainingSeconds = timeInSeconds % 60;
        String formattedMinutes = String.format("%02d", minutes);
        String formattedSeconds = String.format("%05.2f", remainingSeconds);
        return formattedMinutes + ":" + formattedSeconds;
    }
}
