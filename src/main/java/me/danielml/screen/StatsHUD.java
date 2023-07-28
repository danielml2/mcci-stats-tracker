package me.danielml.screen;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.*;

public class StatsHUD implements HudRenderCallback {

    private static String statsDisplay = "";
    private static Color textColor = DebugScreen.DEFAULT;
    private static int textColorHex = 0xEEEEEE;

    private static int x = 0, y = 0;
    private static boolean hudEnabled = true;
    private static boolean drawWithShadows = true;

    private static MultilineText hudMultiline;
    private static TextRenderer clientTextRenderer;
    @Override
    public void onHudRender(MatrixStack matrixStack, float v) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();

        String currentServer = minecraftClient.getCurrentServerEntry() != null ? minecraftClient.getCurrentServerEntry().address : "";
        if(!currentServer.endsWith("mccisland.net"))
            return;

        if(clientTextRenderer == null || hudMultiline == null)
        {
            clientTextRenderer = minecraftClient.textRenderer;
            hudMultiline = MultilineText.create(clientTextRenderer, Text.literal(statsDisplay), 0xEEEEEE);
        }

        matrixStack.scale(1,1,1);
        if(hudEnabled)
        {
            if(drawWithShadows)
                hudMultiline.drawWithShadow(matrixStack, x, y, clientTextRenderer.fontHeight, textColorHex);
            else
                hudMultiline.draw(matrixStack, x, y, clientTextRenderer.fontHeight, textColorHex);
        }
    }

    public static void setStatsDisplay(String newStatsDisplay) {
        statsDisplay = newStatsDisplay;
        clientTextRenderer = clientTextRenderer == null ? MinecraftClient.getInstance().textRenderer : clientTextRenderer;
        hudMultiline = MultilineText.create(clientTextRenderer, Text.literal(statsDisplay), 0xEEEEEE);
    }

    public static void setTextColor(Color color) {
        var hexStr = Integer.toHexString(color.getRGB());
        textColor = color;
        textColorHex = Integer.valueOf(hexStr.substring(2), 16);
    }

    public static Color getTextColor() {
        return textColor;
    }
    public static int getTextColorHex() {
        return textColorHex;
    }

    public static void setHudEnabled(boolean hudEnabled) {
        StatsHUD.hudEnabled = hudEnabled;
    }

    public static void setPosition(int newX, int newY) {
        x = newX;
        y = newY;
    }

    public static void setDrawWithShadows(boolean drawWithShadows) {
        StatsHUD.drawWithShadows = drawWithShadows;
    }
    public static boolean isDrawingWithShadows() {
        return drawWithShadows;
    }
}
