package dev.mayuna.modularbot.sigewine;

import dev.mayuna.modularbot.config.ModularBotConfig;
import enterprises.iwakura.sigewine.core.annotations.RomaritimeBean;

public class ModularBotConfigTreatment {

    @RomaritimeBean
    public ModularBotConfig modularBotConfig() {
        return ModularBotConfig.load();
    }
}
