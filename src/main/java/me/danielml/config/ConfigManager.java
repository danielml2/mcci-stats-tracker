package me.danielml.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.impl.controller.IntegerFieldControllerBuilderImpl;
import dev.isxander.yacl3.impl.controller.TickBoxControllerBuilderImpl;
import me.danielml.screen.DebugScreen;
import me.danielml.screen.UIPlacementScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import static me.danielml.MCCIStats.LOGGER;

public class ConfigManager {

    private static HashMap<String, Object> configValues;
    private Option<Integer> hudX, hudY;

    public ConfigManager() {
        configValues = new HashMap<>();
        loadConfigFromFile();
    }

    private <T> T getConfigValue(String key, Class<T> type) {
        return type.cast(configValues.get(key));
    }

    public void setConfigValue(String key, Object value) {
        configValues.put(key, value);
    }

    public Screen getConfigUI() {
        hudX = Option.<Integer>createBuilder()
                .name(Text.of("HUD X"))
                .available(false)
                .binding(0,
                        () -> getConfigValue("hudX", Integer.class),
                        (newValue) -> setConfigValue("hudX", newValue)
                )
                .controller(IntegerFieldControllerBuilderImpl::new)
                .build();
        hudY = Option.<Integer>createBuilder()
                .name(Text.of("HUD Y"))
                .available(false)
                .binding(0,
                        () -> getConfigValue("hudY", Integer.class),
                        (newValue) -> setConfigValue("hudY", newValue)
                )
                .controller(IntegerFieldControllerBuilderImpl::new)
                .build();

        return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("MCCI Stats Tracker"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("UI Settings"))
                        .options(Arrays.asList(
                                Option.<Boolean>createBuilder()
                                .name(Text.literal("Stats HUD on/off"))
                                .binding(
                                        true,
                                        () -> getConfigValue("hudEnabled", Boolean.class),
                                        (hudToggleState) -> configValues.put("hudEnabled", hudToggleState)
                                )
                                .controller(opt -> BooleanControllerBuilder.create(opt)
                                        .valueFormatter(val -> Text.of(val ? "ON" : "OFF")))
                                .build(),
                                ButtonOption.createBuilder()
                                        .name(Text.literal("Change HUD Placement"))
                                        .text(Text.literal("Set & Preview"))
                                        .available(true)
                                        .action((yaclScreen, buttonOption) -> {
                                             MinecraftClient.getInstance().setScreen(
                                                     new UIPlacementScreen(yaclScreen,
                                                             getConfigValue("hudX", Integer.class),
                                                             getConfigValue("hudY", Integer.class),
                                                             this));
                                        }).build(),
                                Option.<Color>createBuilder()
                                        .name(Text.literal("HUD Text Color"))
                                        .binding(
                                                Color.white,
                                                () -> getConfigValue("textColor", Color.class),
                                                (color) -> configValues.put("textColor", color)
                                        )
                                        .controller(ColorControllerBuilder::create)
                                        .build(),
                                Option.<Boolean>createBuilder()
                                        .name(Text.literal("Draw Text with Shadows"))
                                        .binding(true,
                                                () -> getConfigValue("drawShadows", Boolean.class),
                                                (shadowSetting) -> setConfigValue("drawShadows", shadowSetting))
                                        .controller(TickBoxControllerBuilderImpl::new)
                                        .build(),
                                hudX,
                                hudY
                        )).build())
                        .save(this::applyAndSave)
                .build()
                .generateScreen(null);
    }


    private void applyAndSave() {
        DebugScreen.setTextColor(getConfigValue("textColor", Color.class));
        DebugScreen.setPosition(getConfigValue("hudX", Integer.class), getConfigValue("hudY", Integer.class));
        serializeAndSave();
    }

    public void setHUDPositionValues(int x, int y) {
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
        configValues.put("textColor", new Color(238, 238, 238));
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
}
