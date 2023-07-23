package me.danielml.screen;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.*;

public class DebugScreen implements HudRenderCallback {

    private static String textToShow = "Debug \n".repeat(5);
    private static double x, y;

    private static int textColorHex = 0xEEEEEE;
    private static Color textColor = new Color(238, 238, 238, 255);

    @Override
    public void onHudRender(MatrixStack matrixStack, float v) {
        var textRenderer = (MinecraftClient.getInstance()).textRenderer;

        matrixStack.scale(1,1,1);
        MultilineText multilineText = MultilineText.create(textRenderer, Text.literal(textToShow), 0xEEEEEE);
        multilineText.drawWithShadow(matrixStack, (int)x, (int)y, textRenderer.fontHeight, textColorHex);
    }

    public static void logText(String newText) {
        textToShow = newText;
    }

    public static void setPosition(double newX, double newY) {
        x = newX;
        y = newY;
    }

    public static void setTextColorHex(Color color) {
        var hexStr = Integer.toHexString(color.getRGB());
        textColor = color;
        textColorHex = Integer.valueOf(hexStr.substring(2), 16);
    }

    public static int getTextColorHex() {
        return textColorHex;
    }

    public static Color getTextColor() {
        return textColor;
    }
}
