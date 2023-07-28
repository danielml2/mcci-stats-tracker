package me.danielml.screen;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.*;

public class DebugScreen implements HudRenderCallback {

    public static Color DEFAULT_TEXT_COLOR = new Color(238, 238, 238, 255);

    private Color textColor = DEFAULT_TEXT_COLOR;
    private String textToShow = "";
    private double x, y;
    private boolean hudEnabled = true;
    private boolean drawWithShadows = true;
    private int textColorHex = 0xEEEEEE;

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

    public void logText(String newText) {
        this.textToShow = newText;
    }

    public void setHudEnabled(boolean hudEnabled) {
        this.hudEnabled = hudEnabled;
    }

    public void setPosition(double newX, double newY) {
        x = newX;
        y = newY;
    }

    public void setTextColor(Color color) {
        var hexStr = Integer.toHexString(color.getRGB());
        textColor = color;
        textColorHex = Integer.valueOf(hexStr.substring(2), 16);
    }

    public int getTextColorHex() {
        return textColorHex;
    }

    public Color getTextColor() {
        return textColor;
    }

    public void setDrawWithShadows(boolean drawWithShadows) {
        this.drawWithShadows = drawWithShadows;
    }
}
