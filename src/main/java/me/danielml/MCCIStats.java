package me.danielml;

import me.danielml.games.Game;
import me.danielml.games.minigames.*;
import me.danielml.mixin.TitleSubtitleMixin;
import me.danielml.screen.DebugScreen;
import me.danielml.screen.StatsHUD;
import me.danielml.util.ScoreboardUtil;
import me.danielml.util.ToggleableLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;

import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;


public class MCCIStats implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final ToggleableLogger LOGGER = new ToggleableLogger("mcci-stats-tracker");

	private static final Game[] GAMES = new Game[]{
			new HoleInTheWall(),
			new ParkourWarriorSurvivor(),
			new SkyBattle(),
			new TGTTOS(),
			new BattleBox()
	};

	private final boolean DEBUG = false;
	private static final Game NONE = new None();

	private static Game currentGame = NONE;

	private String lastTitle;
	private String lastSubtitle;

	@Override
	public void onInitialize() {
		LOGGER.info("Logger enabled: " + DEBUG);
		LOGGER.setEnabled(DEBUG);

		if(DEBUG)
			HudRenderCallback.EVENT.register(new DebugScreen());
		else
			HudRenderCallback.EVENT.register(new StatsHUD());


		ClientSendMessageEvents.CHAT.register(message -> {
			LOGGER.info("Sent chat message!");
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

				StatsHUD.setStatsDisplay(currentGame.displayData());
				DebugScreen.logText(debugText.toString());
			});
		});

		ClientReceiveMessageEvents.GAME.register((text, b) -> {
			LOGGER.info("[GAME]" + text.getString() + "(" + b + ")");
			currentGame.onChatMessageInGame(text);
		});
	}

	public void detectMode(ScoreboardObjective objective) {

		var objectiveDisplayName = objective.getDisplayName();
		if(objectiveDisplayName.getSiblings().size() < 3) {
			if(!(currentGame instanceof None))
				LOGGER.info("Back to the lobby!");
			currentGame = NONE;
			return;
		}
		// For some reason, the title of the game while IN GAME shows only on the 3rd sibling of the text, in lobby's it shows on the 2nd one, no idea why
		var gameTitle = objectiveDisplayName.getSiblings().get(2).getString();
		if(currentGame != getGameFromIdentifier(gameTitle)) {
			currentGame = getGameFromIdentifier(gameTitle);
			LOGGER.info("Game Played Currently: " + gameTitle);
		}
	}


	public Game getGameFromIdentifier(String identifier) {
		var optional = Arrays.stream(GAMES)
				.filter(game -> game.getSidebarIdentifier().equalsIgnoreCase(identifier))
				.findFirst();

		return optional.orElse(NONE);
	}

	public static void onScoreboardUpdate() {

		MinecraftClient client = MinecraftClient.getInstance();
		if(client != null)
			ScoreboardUtil.getCurrentScoreboard(client).ifPresent(scoreboard -> {
				var rows = ScoreboardUtil.getSidebarRows(scoreboard);
				currentGame.onSidebarUpdate(rows);
			});
	}

}
