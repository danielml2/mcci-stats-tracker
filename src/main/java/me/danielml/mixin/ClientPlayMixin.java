package me.danielml.mixin;

import me.danielml.MCCIStats;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayMixin {

    @Inject(method = {"onGameMessage"}, at = @At("TAIL"))
    public void injectOnGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        MCCIStats.onClientPlayHandlerInjectedGameMessage(packet.content());
    }
}
