package dev.mayuna.modularbot.sigewine;

import dev.mayuna.modularbot.config.ModularBotConfig;
import dev.mayuna.modularbot.managers.ModularBotDataManager;
import enterprises.iwakura.sigewine.annotations.RomaritimeBean;
import lombok.RequiredArgsConstructor;

@RomaritimeBean
@RequiredArgsConstructor
public class ModularBotDataManagerTreatment {

    private final ModularBotConfig config;

    @RomaritimeBean
    public ModularBotDataManager modularBotDataManager() {
        return new ModularBotDataManager(config.getStorageSettings());
    }
}
