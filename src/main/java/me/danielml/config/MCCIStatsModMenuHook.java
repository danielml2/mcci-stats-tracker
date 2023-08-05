package me.danielml.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.danielml.MCCIStats;
import net.minecraft.client.gui.screen.Screen;

public class MCCIStatsModMenuHook implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (ConfigScreenFactory<Screen>) parent -> MCCIStats.getConfigScreen();
    }
}
