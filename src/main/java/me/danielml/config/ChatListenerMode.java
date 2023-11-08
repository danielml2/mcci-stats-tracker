package me.danielml.config;

import dev.isxander.yacl3.api.NameableEnum;
import me.danielml.mixin.ClientPlayMixin;
import me.danielml.mixin.MessageHandlerMixin;
import net.minecraft.text.Text;

// Different ways the mod can listen for chat messages, in case the default way doesn't work due to interference with some mod.
public enum ChatListenerMode implements NameableEnum {
    /**
     * This mode just uses the built-in Fabric Events API to listen for game messages
     */
    FABRIC_EVENTS("Fabric Events"),
    /**
     *  This uses the mixin of the ClientPlayNetworkHandler class, injecting into the onGameMessage method to listen for game messages
     *  @see ClientPlayMixin
     */
    CLIENTPLAYNETWORKHANDLER_INJECTION("Client Network Injection"),
    /**
     *  This uses the mixin of the MessageHandler class, injecting into the onGameMessage method to listen for game messages
     *  @see MessageHandlerMixin
     */
    MESSAGEHANDLER_INJECTION("Message Handler Injection");

    private final String displayName;

    ChatListenerMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public Text getDisplayName() {
        return Text.literal(displayName);
    }
}
