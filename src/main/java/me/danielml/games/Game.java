package me.danielml.games;



import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.List;

import static me.danielml.MCCIStats.LOGGER;

public abstract class Game {

    protected final DecimalFormat twoDigitFormat = new DecimalFormat("#.##");

    public void onChatMessageInGame(Text messageText) {}

    public void onTitleChange(String title) {}

    public void onSubtitleChange(String subtitle) {}

    public void onSidebarUpdate(List<String> sidebarRows) {}
    public final void saveData() {
        if(getSidebarIdentifier().equalsIgnoreCase("None"))
            return;

        File statsFolder = new File(FabricLoader.getInstance().getGameDir().toString() + "/mcci-stats");
        String fileName = getSidebarIdentifier().replaceAll(" ", "_").toLowerCase() + ".json";

        File file = new File(statsFolder.getAbsolutePath() + "/" + fileName);
        try {
            if(!file.exists()) {
                statsFolder.mkdir();
                file.createNewFile();
            }

            JsonObject object = serializeData();
            var fileWriter = new FileWriter(file);
            new Gson().toJson(object, fileWriter);
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            LOGGER.forceError("Failed to save data for " + fileName, e);
        }

    }

    public final void loadData() {
        File statsFolder = new File(FabricLoader.getInstance().getGameDir().toString() + "/mcci-stats");
        String fileName = getSidebarIdentifier().replaceAll(" ", "_").toLowerCase() + ".json";

        File file = new File(statsFolder.getAbsolutePath() + "/" + fileName);
        try {
            if(!file.exists()) {
                loadFailSafeDefaultData();
            } else {
                var jsonObject = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
                deserializeData(jsonObject);
            }
        } catch (Exception e) {
            LOGGER.forceError("Failed to save data for " + fileName, e);
            loadFailSafeDefaultData();
        }
    }
    public abstract String displayData();
    public abstract JsonObject serializeData();
    public abstract void deserializeData(JsonObject jsonObject);

    public abstract void loadFailSafeDefaultData();

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
