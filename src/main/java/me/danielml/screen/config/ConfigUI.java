package me.danielml.screen.config;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.impl.controller.TickBoxControllerBuilderImpl;
import me.danielml.screen.DebugScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;

public class ConfigUI {

    private static HashMap<String, Object> configValues;

    public static void initAndLoad() {
        configValues = new HashMap<>();
        configValues.put("hudEnabled", true);
        configValues.put("textColor", Color.WHITE);
        configValues.put("drawShadows", true);
        configValues.put("hudX", 0);
        configValues.put("hudY", 0);
    }

    private static <T> T getConfigValue(String key, Class<T> type) {
        return type.cast(configValues.get(key));
    }

    public static void setConfigValue(String key, Object value) {
        configValues.put(key, value);
    }

    public static Screen getConfigUI() {
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
                                        .action((yaclScreen, buttonOption) -> {
                                             MinecraftClient.getInstance().setScreen(
                                                     new UIPlacementScreen(yaclScreen, getConfigValue("hudX", Integer.class), getConfigValue("hudY", Integer.class)));
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
                                        .build()

                        )).build())
                        .save(ConfigUI::applyAndSave)
                .build()
                .generateScreen(null);
    }


    private static void applyAndSave() {
        DebugScreen.setTextColorHex(getConfigValue("textColor", Color.class));
        DebugScreen.setPosition(getConfigValue("hudX", Integer.class), getConfigValue("hudY", Integer.class));
        serialize();
    }
    private static void serialize() {

    }
}
