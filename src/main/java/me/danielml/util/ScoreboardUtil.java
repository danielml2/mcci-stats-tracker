package me.danielml.util;


import static me.danielml.MCCIStats.LOGGER;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.*;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ScoreboardUtil {

    public static Optional<Scoreboard> getCurrentScoreboard(MinecraftClient client) {
        return client.player != null && client.player.getScoreboard() != null ? Optional.of(client.player.getScoreboard()) : Optional.empty();
    }

    public static List<String> getSidebarRows(Scoreboard scoreboard) {
        return scoreboard.getTeams().stream()
                .filter(team -> !team.getPrefix().getString().equals(""))
                .map(team -> team.getPrefix().getString())
                .toList();
    }

}
