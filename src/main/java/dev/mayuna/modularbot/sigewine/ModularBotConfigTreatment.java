package dev.mayuna.modularbot.sigewine;

import dev.mayuna.modularbot.config.ModularBotConfig;
import enterprises.iwakura.sigewine.annotations.RomaritimeBean;

public class ModularBotConfigTreatment {

    @RomaritimeBean
    public ModularBotConfig modularBotConfig() {
        return ModularBotConfig.load();
    }
}
