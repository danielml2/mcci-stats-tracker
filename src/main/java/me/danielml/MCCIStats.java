package me.danielml;

import me.danielml.games.Game;
import me.danielml.games.minigames.*;
import me.danielml.mixin.TitleSubtitleMixin;
import me.danielml.screen.DebugScreen;
import me.danielml.util.ScoreboardUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

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
	public static final Logger LOGGER = LoggerFactory.getLogger("mcci-stats-tracker");

	private static final Game[] GAMES = new Game[]{
			new HoleInTheWall(),
			new ParkourWarriorSurvivor(),
			new SkyBattle(),
			new TGTTOS(),
			new BattleBox()
	};
	private static final Game NONE = new None();

	private static Game currentGame = NONE;

	private String lastTitle;
	private String lastSubtitle;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		HudRenderCallback.EVENT.register(new DebugScreen());

		ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {

			String currentServer = minecraftClient.getCurrentServerEntry() != null ? minecraftClient.getCurrentServerEntry().address : "";
			if(!currentServer.endsWith("mccisland.net"))
				return;

			var scoreboardOptional = ScoreboardUtil.getCurrentScoreboard(minecraftClient);
			scoreboardOptional.ifPresent(scoreboard -> {
				// Add check for what game we're on, and if we're in game at all, or just at the lobby of said game

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


				StringBuilder debugText = new StringBuilder("Currently playing: " + currentGame.getSidebarIdentifier() + "\n ");
				debugText.append(currentGame.displayData()).append(" \n");

				debugText.append("\n Title: ").append(titleString);
				debugText.append(" \n Subtitle:").append(subtitleString);
				debugText.append("\n ");
				var sidebarRows = ScoreboardUtil.getSidebarRows(scoreboard);
				int rowIndex = 0;
				for(String row : sidebarRows) {
					debugText.append(rowIndex).append(": ").append(row).append(" \n ");
					rowIndex++;
				}


				// DONE Hole in the Wall: Placement, Top wall speed? (Row:), Average placement,  (Placement shows in subtitles, also players remaining on the sidebar)

				// Battle Box: Eliminations (Chat + Title), Personal Placement (Sidebar/Endgame chat), Team Placement (Endgame Chat/Sidebar), Game Over (Title & Chat), Team (Sidebar)
				// Sky Battle: Personal Placement (Sidebar), Survivor Placement (Chat / Title), Eliminations (Chat & Title), Avg Team Placement (Chat / Title), Game over: chat
				// TGTTOS: Avg Placement per Map (Chat/Subtitle), Avg Game Placement, Avg/Time per Map (Chat) Avg Placement in the current game (Sidebar, Chat)
				// DONE PKWS: Avg Time for Leap / Map (Chat), Avg Placement (Title + Chat), Avg Placement per Leap (Sidebar)
				DebugScreen.logText(debugText.toString());
			});
		});

		// Add scraping kills of our own using a regex or something based on the game
		// Also scrap whenever the game ends? either that or through title screens
		ClientReceiveMessageEvents.GAME.register((text, b) -> {
			LOGGER.info("[GAME]" + text.getString() + "(" + b + ")");


			// Death messages work both for HOTW & PKWS but still needs to split later for other stuff
			// TODO: Get death/placement detection for the rest of the games, which probably won't be from chat
			currentGame.onChatMessageInGame(text);

			MinecraftClient client = MinecraftClient.getInstance();

			Text title = ((TitleSubtitleMixin)client.inGameHud).getTitle();
			Text subtitle = ((TitleSubtitleMixin)client.inGameHud).getSubtitle();

			String titleString = title != null ? title.getString() : "None";
			String subtitleString = subtitle != null ? subtitle.getString() : "None";

//			LOGGER.info("Title: " + titleString);
//			LOGGER.info("Subtitle: " + subtitleString);
//
//			// 
//			ScoreboardUtil.getCurrentScoreboard(MinecraftClient.getInstance()).ifPresent((scoreboard -> {
//				var sidebarRows = ScoreboardUtil.getSidebarRows(scoreboard);
//				int rowIndex = 0;
//				for(String row : sidebarRows) {
//					LOGGER.info(rowIndex + ": " + row);
//					rowIndex++;
//				}
//			}));
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
				LOGGER.info("Sidebar update!");
				currentGame.onSidebarUpdate(rows);
			});
	}

}
