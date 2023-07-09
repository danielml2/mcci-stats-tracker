package me.danielml;

import me.danielml.util.ScoreboardUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import net.minecraft.scoreboard.Scoreboard;

import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;


public class MCCIStats implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("mcci-stats-tracker");
	private String currentGame = "";

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.



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
			});
		});

		// Add scraping kills of our own using a regex or something based on the game
		// Also scrap whenever the game ends? either that or through title screens
		ClientReceiveMessageEvents.GAME.register((text, b) -> {
			LOGGER.info("[GAME]" + text.getString() + "(" + b + ")");
		});
	}

	public void detectMode(ScoreboardObjective objective) {

		var objectiveDisplayName = objective.getDisplayName();
		if(objectiveDisplayName.getSiblings().size() < 3) {
			if(!currentGame.equalsIgnoreCase("None"))
				LOGGER.info("Back to the lobby!");
			currentGame = "None";
			return;
		}
		// For some reason, the title of the game while IN GAME shows only on the 3rd sibling of the text, in lobby's it shows on the 2nd one, no idea why
		var gameTitle = objectiveDisplayName.getSiblings().get(2).getString();
		if(!currentGame.equalsIgnoreCase(gameTitle)) {
			currentGame = gameTitle;
			LOGGER.info("Game Played Currently: " + gameTitle);
		}
	}
}
