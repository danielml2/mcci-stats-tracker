package me.danielml.screen.config;

import me.danielml.screen.DebugScreen;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.*;

import static me.danielml.MCCIStats.*;

public class UIPlacementScreen extends Screen {

    private final Screen previousScreen;
    private ButtonWidget previewTextSwap;
    private String previewText;
    private MultilineText previewMultiline;
    private MultilineText mousePosition;
    private MultilineText instructions;
    private int previewX;
    private int previewY;

    private int gameIndex = 0;
    private int mouseColor = 0;

    public UIPlacementScreen(Screen previousScreen, int startX, int startY) {
        super(Text.literal(""));
        previewX = startX;
        previewY = startY;
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {


        previewText = getGameByIndex(gameIndex).previewUI();
        previewMultiline = MultilineText.create(textRenderer, Text.literal(previewText), 200);
        mousePosition = MultilineText.create(textRenderer, Text.literal(previewText), 0xEEEEEE);
        mouseColor = generateHalfTransparentVersionInHex(DebugScreen.getTextColor());

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
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        previewMultiline.drawWithShadow(matrices, previewX, previewY, textRenderer.fontHeight, DebugScreen.getTextColorHex());
        instructions.drawWithShadow(matrices, width / 2 - 75, previewTextSwap.getY() - 50, textRenderer.fontHeight, 0xEEEEEE);
        mousePosition.drawWithShadow(matrices, mouseX, mouseY, textRenderer.fontHeight, mouseColor);
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
//            ConfigUI.setConfigValue("hudX", mouseX);
//            ConfigUI.setConfigValue("hudY", mouseY);
        } else if(button == 1) {
            changePreviewText();
        }
        LOGGER.info("Button: " + button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        client.setScreen(previousScreen);
    }

    class MultilineTextButton extends ButtonWidget {

        private MultilineText multilineMessage;

        public MultilineTextButton(int x, int y, int width, int height, Text message, PressAction onPress) {
            super(x, y, width, height, message, onPress, textSupplier -> Text.empty());
            setMessage(message);
        }

        @Override
        public void drawMessage(MatrixStack matrices, TextRenderer textRenderer, int color) {
            multilineMessage.drawCenterWithShadow(matrices, getX() + (width / 2), getY() + (height / 2) - textRenderer.fontHeight, textRenderer.fontHeight, DebugScreen.getTextColorHex());
        }

        @Override
        public void setMessage(Text message) {
            multilineMessage = MultilineText.create(textRenderer, getMessage(), width);
            super.setMessage(message);
        }
    }
}
