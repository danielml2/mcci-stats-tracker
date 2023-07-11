package me.danielml.mixin;

import me.danielml.MCCIStats;
import static me.danielml.MCCIStats.LOGGER;
import net.minecraft.scoreboard.Scoreboard;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(Scoreboard.class)
public class ScoreboardMixin {


    @Inject(method = {"updateScoreboardTeam"}, at = @At("TAIL"))
    public void injectUpdateScoreboardTeam(CallbackInfo callbackInfo) {
        try {
            Field field = CallbackInfo.class.getDeclaredField("name");
            field.setAccessible(true);
            MCCIStats.onScoreboardUpdate();

        } catch (Exception e) {
            LOGGER.error("Failed: ", e);
        }

    }
}
