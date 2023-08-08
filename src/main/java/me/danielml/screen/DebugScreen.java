package me.danielml.screen;

import me.danielml.MCCIStats;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.*;

public class DebugScreen implements HudRenderCallback {

    public static Color DEFAULT_TEXT_COLOR = new Color(238, 238, 238, 255);

    private Color textColor = DEFAULT_TEXT_COLOR;
    private String textToShow = "";
    private int x, y;
    private boolean hudEnabled = true;
    private boolean drawWithShadows = true;

    private boolean hideOnPlayerList = true;
    private int textColorHex = 0xEEEEEE;

    private MCCIStats mod;
    public DebugScreen(MCCIStats mod) {
        this.mod = mod;
    }

    @Override
    public void onHudRender(DrawContext drawContext, float v) {
        var textRenderer = (MinecraftClient.getInstance()).textRenderer;

        MinecraftClient minecraftClient = MinecraftClient.getInstance();

        String currentServer = minecraftClient.getCurrentServerEntry() != null ? minecraftClient.getCurrentServerEntry().address : "";
        boolean shouldHideFromPlayerList = minecraftClient.options.playerListKey.isPressed() && hideOnPlayerList;
        boolean shouldHideFromKeybinding = mod.getHideHUDKeybinding().isPressed();

        if(!currentServer.endsWith("mccisland.net") || shouldHideFromPlayerList || shouldHideFromKeybinding)
            return;

        if(hudEnabled) {
            MultilineText multilineText = MultilineText.create(textRenderer, Text.literal(textToShow), 0xEEEEEE);
            if(drawWithShadows)
                multilineText.drawWithShadow(drawContext, x, y, textRenderer.fontHeight, textColorHex);
            else
                multilineText.draw(drawContext, x, y, textRenderer.fontHeight, textColorHex);
        }
    }

    public void logText(String newText) {
        this.textToShow = newText;
    }

    public void setHudEnabled(boolean hudEnabled) {
        this.hudEnabled = hudEnabled;
    }

    public void setPosition(int newX, int newY) {
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


    public void setHideOnPlayerList(boolean hideOnPlayerList) {
        this.hideOnPlayerList = hideOnPlayerList;
    }

    public boolean isHidingOnPlayerList() {
        return hideOnPlayerList;
    }
}
