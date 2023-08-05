package me.danielml.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.impl.controller.IntegerFieldControllerBuilderImpl;
import dev.isxander.yacl3.impl.controller.TickBoxControllerBuilderImpl;
import me.danielml.screen.DebugScreen;
import me.danielml.screen.StatsHUD;
import me.danielml.screen.UIPlacementScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;

import static me.danielml.MCCIStats.LOGGER;

public class ConfigManager {

    private final HashMap<String, Object> configValues;
    private Option<Integer> hudX, hudY;
    
    private final StatsHUD statsHUD;
    private final DebugScreen debugScreen;

    public ConfigManager(StatsHUD statsHUD, DebugScreen debugScreen) {
        configValues = new HashMap<>();
        this.statsHUD = statsHUD;
        this.debugScreen = debugScreen;
        loadConfigFromFile();
        applySettings();
    }

    private <T> T getConfigValue(String key, Class<T> type) {
        return type.cast(configValues.get(key));
    }

    private void setConfigValue(String key, Object value) {
        configValues.put(key, value);
    }

    public Screen getConfigUI() {
        hudX = Option.<Integer>createBuilder()
                .name(Text.of("HUD X"))
                .available(false)
                .description(OptionDescription.of(Text.literal("The X offset of the HUD's position on the screen (changes slightly between window sizes)")))
                .binding(0,
                        () -> getConfigValue("hudX", Integer.class),
                        (newValue) -> setConfigValue("hudX", newValue)
                )
                .controller(IntegerFieldControllerBuilderImpl::new)
                .build();
        hudY = Option.<Integer>createBuilder()
                .name(Text.of("HUD Y"))
                .available(false)
                .description(OptionDescription.of(Text.literal("The Y offset of the HUD's position on the screen (changes slightly between window sizes)")))
                .binding(0,
                        () -> getConfigValue("hudY", Integer.class),
                        (newValue) -> setConfigValue("hudY", newValue)
                )
                .controller(IntegerFieldControllerBuilderImpl::new)
                .build();

        var colorDescription = Text.literal("In order to change the color of it, find the §eHEX Color Code §fof it using an ")
                .append(Text.literal("§9§nonline color picker like this")
                        .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://g.co/kgs/947HZ8"))))
                .append(" and put it on the option value");

        boolean isInWorld = MinecraftClient.getInstance().world != null;
        LOGGER.info("Minecraft world: " + MinecraftClient.getInstance().world);
        LOGGER.info("Is in world: " + isInWorld);
        var disabledText = isInWorld ? "" : "§c§lNOTE: This option is disabled because you're not in game. ";


        return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("MCCI Stats Tracker"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("UI Settings"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Stats HUD on/off"))
                                .description(OptionDescription.of(Text.literal("Should the HUD render or not (NOTE: This doesn't disable the stats tracking)")))
                                .binding(
                                        true,
                                        () -> getConfigValue("hudEnabled", Boolean.class),
                                        (hudToggleState) -> configValues.put("hudEnabled", hudToggleState)
                                )
                                .controller(opt -> BooleanControllerBuilder.create(opt)
                                        .valueFormatter(val -> Text.of(val ? "ON" : "OFF")))
                                .build())
                        .option(Option.<Color>createBuilder()
                                        .name(Text.literal("HUD Text Color"))
                                        .binding(
                                                DebugScreen.DEFAULT_TEXT_COLOR,
                                                () -> getConfigValue("textColor", Color.class),
                                                (color) -> configValues.put("textColor", color)
                                        )
                                        .description(OptionDescription.of(colorDescription))
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                        .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Draw Text with Shadows"))
                                        .description(OptionDescription.of(Text.literal("Should the text of the HUD rendered with or without shadows")))
                                        .binding(true,
                                                () -> getConfigValue("drawShadows", Boolean.class),
                                                (shadowSetting) -> setConfigValue("drawShadows", shadowSetting))
                                        .controller(TickBoxControllerBuilderImpl::new)
                                        .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("HUD Placement"))
                                .options(Arrays.asList(ButtonOption.createBuilder()
                                                .name(Text.literal("Change HUD Placement"))
                                                .text(Text.literal("Set & Preview"))
                                                .description(OptionDescription.of(
                                                        Text.literal(disabledText + "§fChange the placement of the stats HUD on your screen, and preview different game's text to see how it looks")))
                                                .available(isInWorld) // For some reason, the UI placement screen kind of breaks when going to it from the title screen
                                                .action((yaclScreen, buttonOption) -> MinecraftClient.getInstance().setScreen(
                                                        new UIPlacementScreen(yaclScreen,
                                                                getConfigValue("hudX", Integer.class),
                                                                getConfigValue("hudY", Integer.class),
                                                                this))).build()
                                        ,
                                        hudX,
                                        hudY)
                                ).build()
                        ).build())
                        .save(this::applyAndSave)
                .build()
                .generateScreen(null);
    }


    private void applyAndSave() {
        applySettings();
        serializeAndSave();
    }

    public void applySettings() {
        var hudEnabled = getConfigValue("hudEnabled", Boolean.class);
        var hudX = getConfigValue("hudX", Integer.class);
        var hudY = getConfigValue("hudY", Integer.class);
        var textColor = getConfigValue("textColor", Color.class);
        var drawShadows = getConfigValue("drawShadows", Boolean.class);


        statsHUD.setTextColor(textColor);
        statsHUD.setPosition(hudX, hudY);
        statsHUD.setHudEnabled(hudEnabled);
        statsHUD.setDrawWithShadows(drawShadows);

        debugScreen.setTextColor(textColor);
        debugScreen.setPosition(hudX,hudY);
        debugScreen.setHudEnabled(hudEnabled);
        debugScreen.setDrawWithShadows(drawShadows);
    }

    public void requestSetPosition(int x, int y) {
        hudX.requestSet(x);
        hudY.requestSet(y);
    }

    private  void serializeAndSave() {
        JsonObject configJSON = new JsonObject();
        configJSON.addProperty("hudX", getConfigValue("hudX", Integer.class));
        configJSON.addProperty("hudY", getConfigValue("hudY", Integer.class));
        configJSON.addProperty("hudEnabled", getConfigValue("hudEnabled", Boolean.class));
        configJSON.addProperty("drawShadows", getConfigValue("drawShadows", Boolean.class));
        configJSON.add("textColor", serializeColor(getConfigValue("textColor", Color.class)));

        File configFolder = new File(FabricLoader.getInstance().getConfigDir().toString() + "");
        String fileName = "mcci-stats-tracker.config.json";

        File file = new File(configFolder.getAbsolutePath() + "/" + fileName);

        try {
            if(!file.exists()) {
                configFolder.mkdir();
                file.createNewFile();
            }

            var fileWriter = new FileWriter(file);
            new Gson().toJson(configJSON, fileWriter);
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            LOGGER.forceError("Failed to save config! ", e);
        }
    }

    private void loadConfigFromFile() {
        File configFolder = new File(FabricLoader.getInstance().getConfigDir().toString() + "");
        String fileName = "mcci-stats-tracker.config.json";

        File file = new File(configFolder.getAbsolutePath() + "/" + fileName);

        try {
            if(!file.exists())
            {
                LOGGER.forceWarn("Didn't find config file, loading default values instead.");
                loadDefaults();
                return;
            }
            var jsonObject = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
            configValues.put("hudEnabled", jsonObject.get("hudEnabled").getAsBoolean());
            configValues.put("drawShadows", jsonObject.get("hudEnabled").getAsBoolean());
            configValues.put("hudX", jsonObject.get("hudX").getAsInt());
            configValues.put("hudY", jsonObject.get("hudY").getAsInt());
            configValues.put("textColor", deserializeColor(jsonObject.getAsJsonObject("textColor")));
            applySettings();
        } catch (Exception e) {
            LOGGER.forceError("Failed to load config file! ", e);
        }
    }


    private JsonObject serializeColor(Color color) {
        JsonObject object = new JsonObject();
        object.addProperty("red", color.getRed());
        object.addProperty("blue", color.getBlue());
        object.addProperty("green", color.getGreen());
        return object;
    }

    private void loadDefaults() {
        configValues.put("hudEnabled", true);
        configValues.put("textColor", DebugScreen.DEFAULT_TEXT_COLOR);
        configValues.put("drawShadows", true);
        configValues.put("hudX", 0);
        configValues.put("hudY", 0);
    }

    private static Color deserializeColor(JsonObject serializedColor) {
        return new Color(
                serializedColor.get("red").getAsInt(),
                serializedColor.get("blue").getAsInt(),
                serializedColor.get("green").getAsInt()
        );
    }

    public StatsHUD getStatsHUD() {
        return statsHUD;
    }

    public DebugScreen getDebugScreen() {
        return debugScreen;
    }
}
