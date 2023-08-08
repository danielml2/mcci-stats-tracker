package me.danielml.screen;

import me.danielml.MCCIStats;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.*;

public class StatsHUD implements HudRenderCallback {

    private String statsDisplay = "";
    private Color textColor = DebugScreen.DEFAULT_TEXT_COLOR;
    private int textColorHex = 0xEEEEEE;

    private int x = 0, y = 0;
    private boolean hudEnabled = true;
    private boolean drawWithShadows = true;

    private boolean hideOnPlayerList = true;

    private MultilineText hudMultiline;
    private TextRenderer clientTextRenderer;

    private MCCIStats mod;
    public StatsHUD(MCCIStats mod) {
        this.mod = mod;
    }

    @Override
    public void onHudRender(MatrixStack matrixStack, float v) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();

        String currentServer = minecraftClient.getCurrentServerEntry() != null ? minecraftClient.getCurrentServerEntry().address : "";
        boolean shouldHideFromPlayerList = minecraftClient.options.playerListKey.isPressed() && hideOnPlayerList;
        boolean shouldHideFromKeybinding = mod.getHideHUDKeybinding().isPressed();

        if(!currentServer.endsWith("mccisland.net") || shouldHideFromPlayerList || shouldHideFromKeybinding)
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

    public void setStatsDisplay(String newStatsDisplay) {
        statsDisplay = newStatsDisplay;
        clientTextRenderer = clientTextRenderer == null ? MinecraftClient.getInstance().textRenderer : clientTextRenderer;
        hudMultiline = MultilineText.create(clientTextRenderer, Text.literal(statsDisplay), 0xEEEEEE);
    }

    public void setTextColor(Color color) {
        var hexStr = Integer.toHexString(color.getRGB());
        textColor = color;
        textColorHex = Integer.valueOf(hexStr.substring(2), 16);
    }

    public Color getTextColor() {
        return textColor;
    }
    public int getTextColorHex() {
        return textColorHex;
    }

    public void setHudEnabled(boolean hudEnabled) {
        this.hudEnabled = hudEnabled;
    }

    public void setPosition(int newX, int newY) {
        x = newX;
        y = newY;
    }

    public void setDrawWithShadows(boolean drawWithShadows) {
        this.drawWithShadows = drawWithShadows;
    }
    public boolean isDrawingWithShadows() {
        return drawWithShadows;
    }

    public void setHideOnPlayerList(boolean hideOnPlayerList) {
        this.hideOnPlayerList = hideOnPlayerList;
    }
    public boolean isHiddenOnPlayerList() {
        return hideOnPlayerList;
    }
}
