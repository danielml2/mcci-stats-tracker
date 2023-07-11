package me.danielml.util;


import static me.danielml.MCCIStats.LOGGER;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ScoreboardUtil {

    public static Optional<Scoreboard> getCurrentScoreboard(MinecraftClient client) {
        return client.player != null && client.player.getScoreboard() != null ? Optional.of(client.player.getScoreboard()) : Optional.empty();
    }

    public static List<String> getSidebarRows(Scoreboard scoreboard) {
        var list = scoreboard.getKnownPlayers()
                .stream()
                .map((playerName) -> scoreboard.getPlayerTeam(playerName).getPrefix().getString())
                .collect(Collectors.toList());
        Collections.reverse(list);
        return list;
    }

}
