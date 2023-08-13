package me.danielml;

import com.mojang.authlib.GameProfile;
import me.danielml.config.ConfigManager;
import me.danielml.games.Game;
import me.danielml.games.minigames.*;
import me.danielml.mixin.TitleSubtitleMixin;
import me.danielml.screen.DebugScreen;
import me.danielml.screen.StatsHUD;
import me.danielml.util.ScoreboardUtil;
import me.danielml.util.ToggleableLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import javax.print.AttributeException;
import java.time.Instant;
import java.util.Arrays;


public class MCCIStats implements ModInitializer {

	public static final ToggleableLogger LOGGER = new ToggleableLogger("mcci-stats-tracker");

	private static final Game[] GAMES = new Game[]{
			new HoleInTheWall(),
			new ParkourWarriorSurvivor(),
			new SkyBattle(),
			new TGTTOS(),
			new BattleBox()
	};

	public static final boolean DEBUG = true;
	private static final Game NONE = new None();

	private String previousScreenData = "";
	private static Game currentGame = NONE;

	private String lastTitle;
	private String lastSubtitle;
	private KeyBinding configKeybinding;
	private KeyBinding hideHUDKeybinding;
	private static ConfigManager configManager;
	private StatsHUD statsHUD;
	private DebugScreen debugScreen;

	@Override
	public void onInitialize() {
		LOGGER.info("Logger enabled: " + DEBUG);
		LOGGER.setEnabled(DEBUG);


		configKeybinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"Open Configuration Screen",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_END,
				"MCCI Stats Tracker"
		));
		hideHUDKeybinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"Hide HUD (While Pressed)",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_R,
				"MCCI Stats Tracker"));

		statsHUD = new StatsHUD(this);
		debugScreen = new DebugScreen(this);

		// Loads config from constructor.
		configManager = new ConfigManager(statsHUD, debugScreen);

		if(DEBUG)
			HudRenderCallback.EVENT.register(debugScreen);
		else
			HudRenderCallback.EVENT.register(statsHUD);

		ClientPlayConnectionEvents.DISCONNECT.register((clientPlayNetworkHandler, minecraftClient) -> {
			LOGGER.info("Disconnected from the server!");
			currentGame.saveData();
		});



		ClientSendMessageEvents.CHAT.register(message -> {
			LOGGER.info("Sent chat message!");
			if(DEBUG)
				ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance()).ifPresent((scoreboard -> {
				var sidebarRows = ScoreboardUtil.getSidebarRows(scoreboard);
				int rowIndex = 0;
				for(String row : sidebarRows) {
					LOGGER.info(rowIndex + ": " + row);
					rowIndex++;
				}
				}));
		});

		ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
			if(configKeybinding.wasPressed())
				MinecraftClient.getInstance().setScreen(getConfigScreen());

			String currentServer = minecraftClient.getCurrentServerEntry() != null ? minecraftClient.getCurrentServerEntry().address : "";
			if(!currentServer.endsWith("mccisland.net"))
				return;

			var scoreboardOptional = ScoreboardUtil.getCurrentScoreboard(minecraftClient);
			scoreboardOptional.ifPresent(scoreboard -> {

				ScoreboardObjective objective = scoreboard.getObjectiveForSlot(Scoreboard.getDisplaySlotId("sidebar"));

				if(objective == null)
					return;

				detectMode(objective);

				Text title = ((TitleSubtitleMixin)minecraftClient.inGameHud).getTitle();
				Text subtitle = ((TitleSubtitleMixin)minecraftClient.inGameHud).getSubtitle();

				String titleString = title != null ? title.getString() : "";
				String subtitleString = subtitle != null ? subtitle.getString() : "";
				if(!titleString.equals(lastTitle))
				{
					LOGGER.info("New title: '" + lastTitle + "' -> '" + titleString + "'");
					lastTitle = titleString;
					currentGame.onTitleChange(lastTitle);
				}
				if(!subtitleString.equals(lastSubtitle)) {
					LOGGER.info("New subtitle: '" + lastSubtitle + "' -> '" + subtitleString + "'");
					lastSubtitle = subtitleString;
					currentGame.onSubtitleChange(subtitleString);
				}


				StringBuilder debugText = new StringBuilder("Currently playing: " + currentGame.getSidebarIdentifier() + "\n");
				debugText.append(currentGame.displayData()).append(" \n");
				if(!debugText.toString().equalsIgnoreCase(previousScreenData)) {
					LOGGER.info("MCCI: New display data: " + debugText);
					previousScreenData = debugText.toString();
				}
				statsHUD.setStatsDisplay(currentGame.displayData());
				debugScreen.logText(debugText.toString());
			});
		});

		ClientReceiveMessageEvents.GAME.register((text, b) -> {
			LOGGER.info("MCCI: [GAME]" + text.getString() + "(" + b + ")");
			currentGame.onChatMessageInGame(text);
		});
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
			LOGGER.info("MCCI: new CHAT message: " + message.getString());
			currentGame.onChatMessageInGame(message);
		});
	}

	public void detectMode(ScoreboardObjective objective) {
		FabricLoader.getInstance().getGameDir();
		var objectiveDisplayName = objective.getDisplayName();
		if(objectiveDisplayName.getSiblings().size() < 3) {
			if(!(currentGame instanceof None))
				LOGGER.info("Back to the lobby!");
			currentGame.saveData();
			currentGame = NONE;
			return;
		}
		// For some reason, the title of the game while IN GAME shows only on the 3rd sibling of the text, in lobby's it shows on the 2nd one, no idea why
		var gameTitle = objectiveDisplayName.getSiblings().get(2).getString();
		// In case for some reason the scoreboard doesn't load in time or something weird happens
		ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance()).ifPresentOrElse(scoreboard -> {
			boolean isDojo = false;
			var sidebarRows = ScoreboardUtil.getSidebarRows(scoreboard);
			for(String row : sidebarRows) {
				if (row.contains("COURSE") || row.contains("medal")) {
					isDojo = true;
					break;
				}
			}
			if(currentGame != getGameFromIdentifier(gameTitle) && !isDojo) {
				loadNewMinigame(getGameFromIdentifier(gameTitle));
			}
		}, () -> {
			if(currentGame != getGameFromIdentifier(gameTitle)) {
				loadNewMinigame(getGameFromIdentifier(gameTitle));
			}
		});
	}

	public void loadNewMinigame(Game newGame) {
		currentGame.saveData();
		currentGame = newGame;
		currentGame.loadData();
		LOGGER.info("Game Played Currently: " + newGame.getSidebarIdentifier());
	}


	public Game getGameFromIdentifier(String identifier) {
		var optional = Arrays.stream(GAMES)
				.filter(game -> game.getSidebarIdentifier().equalsIgnoreCase(identifier))
				.findFirst();

		return optional.orElse(NONE);
	}

	public static Game getGameByIndex(int index) {
		return GAMES[index];
	}
	public static int gameCount() {
		return GAMES.length;
	}
	
	public static void resetToNone() {
		currentGame = NONE;
	}

	public static void onScoreboardUpdate() {

		MinecraftClient client = MinecraftClient.getInstance();
		if(client != null)
			ScoreboardUtil.getCurrentScoreboard(client).ifPresent(scoreboard -> {
				var rows = ScoreboardUtil.getSidebarRows(scoreboard);
				currentGame.onSidebarUpdate(rows);
			});
	}

	public static Screen getConfigScreen() {
		return configManager.getConfigUI();
	}

	public KeyBinding getHideHUDKeybinding() {
		return hideHUDKeybinding;
	}
}
