package me.danielml.screen;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.*;

public class DebugScreen implements HudRenderCallback {

    private static String textToShow = "";
    private static double x, y;

    private static boolean hudEnabled = true;
    private static boolean drawWithShadows = true;
    private static int textColorHex = 0xEEEEEE;
    public static Color DEFAULT = new Color(238, 238, 238, 255);
    private static Color textColor = DEFAULT;

    @Override
    public void onHudRender(MatrixStack matrixStack, float v) {
        var textRenderer = (MinecraftClient.getInstance()).textRenderer;

        matrixStack.scale(1,1,1);
        if(hudEnabled) {
            MultilineText multilineText = MultilineText.create(textRenderer, Text.literal(textToShow), 0xEEEEEE);
            if(drawWithShadows)
                multilineText.drawWithShadow(matrixStack, (int)x, (int)y, textRenderer.fontHeight, textColorHex);
            else
                multilineText.draw(matrixStack, (int)x, (int)y, textRenderer.fontHeight, textColorHex);
        }
    }

    public static void logText(String newText) {
        textToShow = newText;
    }

    public static void setHudEnabled(boolean hudEnabled) {
        DebugScreen.hudEnabled = hudEnabled;
    }

    public static void setPosition(double newX, double newY) {
        x = newX;
        y = newY;
    }

    public static void setTextColor(Color color) {
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

    public static void setDrawWithShadows(boolean drawWithShadows) {
        DebugScreen.drawWithShadows = drawWithShadows;
    }
}
