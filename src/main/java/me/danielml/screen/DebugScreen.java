package me.danielml.screen;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class DebugScreen implements HudRenderCallback {

    private static String textToShow = "";

    @Override
    public void onHudRender(DrawContext drawContext, float v) {
        var textRenderer = (MinecraftClient.getInstance()).textRenderer;

        MultilineText multilineText = MultilineText.create(textRenderer, Text.literal(textToShow), 0xEEEEEE);

        multilineText.drawWithShadow(drawContext, 0, 0, textRenderer.fontHeight, 0xEEEEEE);
    }


    public static void logText(String newText) {
        textToShow = newText;
    }
}
