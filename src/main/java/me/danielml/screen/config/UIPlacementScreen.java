package me.danielml.screen.config;

import me.danielml.screen.DebugScreen;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import static me.danielml.MCCIStats.LOGGER;

public class UIPlacementScreen extends Screen {

    private Screen previousScreen;

    public UIPlacementScreen(Screen previousScreen) {
        super(Text.literal(""));
        this.previousScreen = previousScreen;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        MultilineText multilineText = MultilineText.create(textRenderer, Text.literal("blah blah blah blah blah \n" .repeat(5)), 0xEEEEEE);
        multilineText.drawWithShadow(matrices, mouseX, mouseY, textRenderer.fontHeight, 0xEEEEEE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        LOGGER.info("Clicked! X: " + mouseX + " Y: " + mouseY);
        DebugScreen.setPosition(mouseX, mouseY);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        client.setScreen(previousScreen);
    }
}
