package me.danielml.games;



import net.minecraft.text.Text;

import java.text.DecimalFormat;

import static me.danielml.MCCIStats.LOGGER;

public abstract class Game {

    protected DecimalFormat twoDigitFormat = new DecimalFormat("#.##");

    public void onChatMessageInGame(Text messageText) {}

    public void onTitleOrSubtitleChange(Text title, Text subtitle) {}

    public void saveData() {}

    public void loadData() {}
    public abstract String displayData();

    public abstract String getSidebarIdentifier();


    public final int extractNumberFromText(String text) {
        int endIndex = 0;
        int startIndex = 0;

        for(char ch : text.toCharArray()) {
            if(ch == ' ') {
                startIndex++;
            } else if(!Character.isDigit(ch))
            {
                LOGGER.info("End of numbers! Found non-digit character: '" + ch + "'");
                break;
            }
            endIndex++;
        }

        return Integer.parseInt(text.substring(startIndex, endIndex));
    }
}
