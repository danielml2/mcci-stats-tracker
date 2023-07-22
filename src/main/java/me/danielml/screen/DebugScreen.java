package me.danielml.screen;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class DebugScreen implements HudRenderCallback {

    private static String textToShow = "Debug \n".repeat(5);
    private static double x, y;

    @Override
    public void onHudRender(MatrixStack matrixStack, float v) {
        var textRenderer = (MinecraftClient.getInstance()).textRenderer;

        matrixStack.scale(1,1,1);
        MultilineText multilineText = MultilineText.create(textRenderer, Text.literal(textToShow), 0xEEEEEE);
        multilineText.drawWithShadow(matrixStack, (int)x, (int)y, textRenderer.fontHeight, 0xEEEEEE);
    }

    public static void logText(String newText) {
        textToShow = newText;
    }

    public static void setPosition(double newX, double newY) {
        x = newX;
        y = newY;
    }
}
