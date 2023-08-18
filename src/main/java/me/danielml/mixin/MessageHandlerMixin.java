package me.danielml.mixin;

import me.danielml.MCCIStats;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MessageHandler.class)
public class MessageHandlerMixin {

    @Inject(method = {"onGameMessage"}, at = @At("TAIL"))
    public void injectOnGameMessage(Text message, boolean overlay, CallbackInfo ci) {
        MCCIStats.LOGGER.info("MCCI: New message coming from MessageHandler Injection");
        MCCIStats.injectOnGameMessage(message);
    }
}
