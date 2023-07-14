package me.danielml.screen;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class StatsHUD implements HudRenderCallback {

    private static String statsDisplay = "";
    @Override
    public void onHudRender(MatrixStack matrixStack, float v) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();

        String currentServer = minecraftClient.getCurrentServerEntry() != null ? minecraftClient.getCurrentServerEntry().address : "";
        if(!currentServer.endsWith("mccisland.net"))
            return;

        var textRenderer = (MinecraftClient.getInstance()).textRenderer;

        matrixStack.scale(1,1,1);
        MultilineText multilineText = MultilineText.create(textRenderer, Text.literal(statsDisplay), 0xEEEEEE);
        multilineText.drawWithShadow(matrixStack, 0, 0, textRenderer.fontHeight, 0xEEEEEE);
    }

    public static void setStatsDisplay(String newStatsDisplay) {
        statsDisplay = newStatsDisplay;
    }
}
