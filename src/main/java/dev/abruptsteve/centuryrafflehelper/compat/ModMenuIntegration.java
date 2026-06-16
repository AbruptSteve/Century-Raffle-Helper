package dev.abruptsteve.centuryrafflehelper.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.abruptsteve.centuryrafflehelper.CenturyRaffleHelperMod;
import net.minecraft.client.gui.screens.Screen;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> CenturyRaffleHelperMod.CONFIG_MANAGER.createConfigScreen(parent instanceof Screen ? (Screen) parent : null, null);
    }
}
