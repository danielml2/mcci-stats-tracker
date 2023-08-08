package me.danielml.screen;

import me.danielml.MCCIStats;
import me.danielml.config.ConfigManager;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.*;

import static me.danielml.MCCIStats.*;

public class UIPlacementScreen extends Screen {

    private final Screen previousScreen;

    private final ConfigManager configManager;
    private ButtonWidget previewTextSwap;
    private String previewText;
    private MultilineText previewMultiline;
    private MultilineText mousePosition;
    private MultilineText instructions;
    private int previewX;
    private int previewY;

    private int gameIndex = 0;
    private int mouseColor = 0;

    private int textColorHex;
    private StatsHUD statsHUD;
    private DebugScreen debugScreen;

    public UIPlacementScreen(Screen previousScreen, int startX, int startY, ConfigManager configManager) {
        super(Text.literal(""));
        previewX = startX;
        previewY = startY;
        this.previousScreen = previousScreen;
        this.configManager = configManager;
        this.debugScreen = configManager.getDebugScreen();
        this.statsHUD = configManager.getStatsHUD();
    }

    @Override
    protected void init() {
        textColorHex = DEBUG ? statsHUD.getTextColorHex() : debugScreen.getTextColorHex();

        previewText = getGameByIndex(gameIndex).previewUI();
        previewMultiline = MultilineText.create(textRenderer, Text.literal(previewText), 200);
        mousePosition = MultilineText.create(textRenderer, Text.literal(previewText), 0xEEEEEE);
        mouseColor = DEBUG ? generateHalfTransparentVersionInHex(debugScreen.getTextColor()) : generateHalfTransparentVersionInHex(statsHUD.getTextColor());

        instructions = MultilineText.create(textRenderer,
                Text.literal(" Left Click to choose a position. \n " +
                        "Right Click/Button to switch the preview text\n" +
                        " To save and/or go back, press ESC."), 2000);


        previewTextSwap = new MultilineTextButton(width / 2 - 37, height / 2 - 25, 75, 50,
                Text.literal("Preview Text:").append("\n").append(getGameByIndex(gameIndex).getSidebarIdentifier()), button -> changePreviewText());


        addDrawableChild(previewTextSwap);

        super.init();
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);

        if(statsHUD.isDrawingWithShadows()) {
            previewMultiline.drawWithShadow(drawContext, previewX, previewY, textRenderer.fontHeight, textColorHex);
            mousePosition.drawWithShadow(drawContext, mouseX, mouseY, textRenderer.fontHeight, mouseColor);
        } else {
            previewMultiline.draw(drawContext, previewX, previewY, textRenderer.fontHeight, textColorHex);
            mousePosition.draw(drawContext, mouseX, mouseY, textRenderer.fontHeight, mouseColor);
        }
        instructions.drawWithShadow(drawContext, width / 2 - 75, previewTextSwap.getY() - 50, textRenderer.fontHeight, 0xEEEEEE);
    }

    public int generateHalfTransparentVersionInHex(Color color) {
        Color newColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 127);
         return  (newColor.getAlpha() << 24) | (newColor.getRed() << 16) | (newColor.getGreen() << 8) | newColor.getBlue();
    }

    public void changePreviewText() {
        gameIndex += 1;
        gameIndex = gameIndex % gameCount();
        previewText = getGameByIndex(gameIndex).previewUI();
        previewMultiline = MultilineText.create(textRenderer, Text.literal(previewText), 0xEEEEEE);
        mousePosition = MultilineText.create(textRenderer, Text.literal(previewText), 0xEEEEEE);
        previewTextSwap.setMessage(Text.literal("Preview Text:").append("\n").append(getGameByIndex(gameIndex).getSidebarIdentifier()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!previewTextSwap.isMouseOver(mouseX, mouseY) && button == 0)
        {
            LOGGER.info("Mouse not over button!");
            previewX = (int) mouseX;
            previewY = (int) mouseY;
        } else if(button == 1) {
            changePreviewText();
        }
        LOGGER.info("Button: " + button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        configManager.requestSetPosition(previewX, previewY);
        LOGGER.info("HUD X: " + previewX + " HUD Y: " + previewY);
        client.setScreen(previousScreen);
    }

    class MultilineTextButton extends ButtonWidget {

        private MultilineText multilineMessage;

        public MultilineTextButton(int x, int y, int width, int height, Text message, PressAction onPress) {
            super(x, y, width, height, message, onPress, textSupplier -> Text.empty());
            setMessage(message);
        }

        @Override
        public void drawMessage(DrawContext drawContext, TextRenderer textRenderer, int color) {
            multilineMessage.drawCenterWithShadow(drawContext, getX() + (width / 2), getY() + (height / 2) - textRenderer.fontHeight, textRenderer.fontHeight, 0xEEEEEE);
        }

        @Override
        public void setMessage(Text message) {
            multilineMessage = MultilineText.create(textRenderer, getMessage(), width);
            super.setMessage(message);
        }
    }
}
