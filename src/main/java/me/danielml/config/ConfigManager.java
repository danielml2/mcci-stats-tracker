package me.danielml.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.impl.controller.IntegerFieldControllerBuilderImpl;
import dev.isxander.yacl3.impl.controller.TickBoxControllerBuilderImpl;
import me.danielml.MCCIStats;
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
import java.util.function.Supplier;

import static me.danielml.MCCIStats.LOGGER;

public class ConfigManager {

    private final HashMap<String, Object> configValues;
    private Option<Integer> hudX, hudY;
    
    private final StatsHUD statsHUD;
    private final DebugScreen debugScreen;
    private MCCIStats mcciStats;
    private HashMap<String, Object> defaults = new HashMap<>();


    public ConfigManager(StatsHUD statsHUD, DebugScreen debugScreen, MCCIStats mcciStats) {
        this.mcciStats = mcciStats;
        configValues = new HashMap<>();
        this.statsHUD = statsHUD;
        this.debugScreen = debugScreen;
        createDefaults();
        loadConfigFromFile();
        applySettings();
    }

    private <T> T getConfigValue(String key, T defaultValue) {
        Class<T> clazz = (Class<T>) defaultValue.getClass();
        return configValues.containsKey(key) ? clazz.cast(configValues.get(key)) : defaultValue;
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
                        () -> getConfigValue("hudX", 0),
                        (newValue) -> setConfigValue("hudX", newValue)
                )
                .controller(IntegerFieldControllerBuilderImpl::new)
                .build();
        hudY = Option.<Integer>createBuilder()
                .name(Text.of("HUD Y"))
                .available(false)
                .description(OptionDescription.of(Text.literal("The Y offset of the HUD's position on the screen (changes slightly between window sizes)")))
                .binding(0,
                        () -> getConfigValue("hudY", 0),
                        (newValue) -> setConfigValue("hudY", newValue)
                )
                .controller(IntegerFieldControllerBuilderImpl::new)
                .build();

        var colorDescription = Text.literal("In order to change the color of it, find the §eHEX Color Code §fof it using an ")
                .append(Text.literal("§9§lonline color picker like this")
                        .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://g.co/kgs/947HZ8"))))
                .append(" and put it on the option value");

        boolean isInWorld = MinecraftClient.getInstance().world != null;
        LOGGER.info("Minecraft world: " + MinecraftClient.getInstance().world);
        LOGGER.info("Is in world: " + isInWorld);
        var placementUIDescription = isInWorld ? "§fChange the placement of the stats HUD on your screen, and preview different game's text to see how it looks"
                : "§c§lNOTE: This option is disabled because you're not in game.";


        return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("MCCI Stats Tracker"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("UI Settings"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Stats HUD on/off"))
                                .description(OptionDescription.of(Text.literal("Should the HUD render or not (NOTE: This doesn't disable the stats tracking)")))
                                .binding(
                                        true,
                                        () -> getConfigValue("hudEnabled", true),
                                        (hudToggleState) -> configValues.put("hudEnabled", hudToggleState)
                                )
                                .controller(opt -> BooleanControllerBuilder.create(opt)
                                        .valueFormatter(val -> Text.of(val ? "ON" : "OFF")))
                                .build())
                        .option(Option.<Color>createBuilder()
                                        .name(Text.literal("HUD Text Color"))
                                        .binding(
                                                DebugScreen.DEFAULT_TEXT_COLOR,
                                                () -> getConfigValue("textColor", DebugScreen.DEFAULT_TEXT_COLOR),
                                                (color) -> configValues.put("textColor", color)
                                        )
                                        .description(OptionDescription.of(colorDescription))
                                        .controller(ColorControllerBuilder::create)
                                        .build())
                        .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Draw Text with Shadows"))
                                        .description(OptionDescription.of(Text.literal("Should the text of the HUD rendered with or without shadows")))
                                        .binding(true,
                                                () -> getConfigValue("drawShadows", true),
                                                (shadowSetting) -> setConfigValue("drawShadows", shadowSetting))
                                        .controller(opt -> BooleanControllerBuilder.create(opt)
                                                .valueFormatter(val -> Text.of(val ? "ON" : "OFF")))
                                        .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Hide stats HUD when holding the player list button"))
                                .description(OptionDescription.of(Text.literal("Should the stats hud be hidden when showing the player list, alternatively in controls you can set a keybind for hiding the hud while you hold it")))
                                .binding(true,
                                        () -> getConfigValue("hideOnList", true),
                                        (hideOnList) -> setConfigValue("hideOnList", hideOnList))
                                .controller(opt -> BooleanControllerBuilder.create(opt)
                                        .valueFormatter(val -> Text.of(val ? "ON" : "OFF")))
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("HUD Placement"))
                                .options(Arrays.asList(ButtonOption.createBuilder()
                                                .name(Text.literal("Change HUD Placement"))
                                                .text(Text.literal("Set & Preview"))
                                                .description(OptionDescription.of(Text.literal(placementUIDescription)))
                                                .available(isInWorld) // For some reason, the UI placement screen kind of breaks when going to it from the title screen
                                                .action((yaclScreen, buttonOption) -> MinecraftClient.getInstance().setScreen(
                                                        new UIPlacementScreen(yaclScreen,
                                                                getConfigValue("hudX", 0),
                                                                getConfigValue("hudY", 0),
                                                                this))).build()
                                        ,
                                        hudX,
                                        hudY)
                                ).build()
                        ).build())
                    .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Technical Settings"))
                        .option(Option.<ChatListenerMode>createBuilder()
                                .name(Text.literal("Chat Listener Mode"))
                                .controller(opt -> EnumControllerBuilder.create(opt)
                                        .enumClass(ChatListenerMode.class))
                                .description(OptionDescription.of(Text.literal("The way the mod listens for in-game chat messages, sometimes the default option the mod uses (Fabric Events) breaks for some people, so this option lets you use other ways in case they would work")))
                                .binding(ChatListenerMode.FABRIC_EVENTS,
                                        () -> getConfigValue("chatListenerMode", ChatListenerMode.FABRIC_EVENTS),
                                        (value) -> setConfigValue("chatListenerMode", value))
                                .build())

                        .build())
                        .save(this::applyAndSave)
                .build()
                .generateScreen(null);
    }


    private void applyAndSave() {
        applySettings();
        serializeAndSave();
    }

    public void applySettings() {
        var hudEnabled = getConfigValue("hudEnabled", true);
        var hudX = getConfigValue("hudX", 0);
        var hudY = getConfigValue("hudY", 0);
        var textColor = getConfigValue("textColor", DebugScreen.DEFAULT_TEXT_COLOR);
        var hideOnPlayerList = getConfigValue("hideOnList", true);
        var drawShadows = getConfigValue("drawShadows", true);


        mcciStats.setChatListenerMode(getConfigValue("chatListenerMode", ChatListenerMode.FABRIC_EVENTS));
        statsHUD.setTextColor(textColor);
        statsHUD.setPosition(hudX, hudY);
        statsHUD.setHudEnabled(hudEnabled);
        statsHUD.setDrawWithShadows(drawShadows);
        statsHUD.setHideOnPlayerList(hideOnPlayerList);

        debugScreen.setTextColor(textColor);
        debugScreen.setPosition(hudX,hudY);
        debugScreen.setHudEnabled(hudEnabled);
        debugScreen.setDrawWithShadows(drawShadows);

        LOGGER.info("MCCI Stats: Finished applying all the config entry values");
        for(var entry : configValues.entrySet()) {
            LOGGER.forceInfo("MCCI Stats: Applied config entry: " + entry.getKey() + " set to " + entry.getValue().toString());
        }
    }

    public void requestSetPosition(int x, int y) {
        hudX.requestSet(x);
        hudY.requestSet(y);
    }

    private  void serializeAndSave() {
        JsonObject configJSON = new JsonObject();
        configJSON.addProperty("hudX", getConfigValue("hudX", 0));
        configJSON.addProperty("hudY", getConfigValue("hudY",0));
        configJSON.addProperty("hudEnabled", getConfigValue("hudEnabled", true));
        configJSON.addProperty("drawShadows", getConfigValue("drawShadows", true));
        configJSON.addProperty("hideOnList", getConfigValue("hideOnList", true));
        configJSON.addProperty("chatListenerMode", getConfigValue("chatListenerMode", ChatListenerMode.FABRIC_EVENTS).name());
        configJSON.add("textColor", serializeColor(getConfigValue("textColor", DebugScreen.DEFAULT_TEXT_COLOR)));

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
            loadConfigEntry("hudEnabled", () -> jsonObject.get("hudEnabled").getAsBoolean());
            loadConfigEntry("drawShadows", () -> jsonObject.get("drawShadows").getAsBoolean());
            loadConfigEntry("hudX", () -> jsonObject.get("hudX").getAsInt());
            loadConfigEntry("hudY", () -> jsonObject.get("hudY").getAsInt());
            loadConfigEntry("textColor", () -> deserializeColor(jsonObject.getAsJsonObject("textColor")));
            loadConfigEntry("hideOnList", () -> jsonObject.get("hideOnList").getAsBoolean());
            loadConfigEntry("chatListenerMode", () -> ChatListenerMode.valueOf(jsonObject.get("chatListenerMode").getAsString()));
        } catch (Exception e) {
            LOGGER.forceError("Failed to load config file! ", e);
            LOGGER.forceWarn("Loading defaults for null values..");
            loadMissingValuesAsDefaults();
        }
    }

    public void loadConfigEntry(String key, Supplier<Object> entryValueSupplier) {
        try {
            configValues.put(key, entryValueSupplier.get());
        } catch (Exception exception) {
            configValues.put(key, defaults.get(key));
        }
    }

    public void createDefaults() {
        defaults.put("hudEnabled", true);
        defaults.put("textColor", DebugScreen.DEFAULT_TEXT_COLOR);
        defaults.put("drawShadows", true);
        defaults.put("hideOnList", true);
        defaults.put("hudX", 0);
        defaults.put("hudY", 0);
        defaults.put("chatListenerMode", ChatListenerMode.FABRIC_EVENTS);
    }

    private JsonObject serializeColor(Color color) {
        JsonObject object = new JsonObject();
        object.addProperty("red", color.getRed());
        object.addProperty("blue", color.getBlue());
        object.addProperty("green", color.getGreen());
        return object;
    }

    private void loadDefaults() {
       configValues.clear();
       configValues.putAll(defaults);
    }

    private void loadMissingValuesAsDefaults() {
        for(var entry : defaults.entrySet()) {
            configValues.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    private static Color deserializeColor(JsonObject serializedColor) {
        return new Color(
                serializedColor.get("red").getAsInt(),
                serializedColor.get("green").getAsInt(),
                serializedColor.get("blue").getAsInt()
        );
    }

    public StatsHUD getStatsHUD() {
        return statsHUD;
    }

    public DebugScreen getDebugScreen() {
        return debugScreen;
    }
}
