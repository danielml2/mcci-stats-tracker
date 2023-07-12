package me.danielml.mixin;

import me.danielml.MCCIStats;
import static me.danielml.MCCIStats.LOGGER;
import net.minecraft.scoreboard.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Scoreboard.class)
public class ScoreboardMixin {


    @Inject(method = {"updateScoreboardTeam"}, at = @At("TAIL"))
    public void injectUpdateScoreboardTeam(CallbackInfo callbackInfo) {
        try {
            MCCIStats.onScoreboardUpdate();
        } catch (Exception e) {
            LOGGER.error("Failed: ", e);
        }

    }
}
