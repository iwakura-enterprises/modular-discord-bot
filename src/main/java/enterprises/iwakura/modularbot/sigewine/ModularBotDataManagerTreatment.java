package enterprises.iwakura.modularbot.sigewine;

import enterprises.iwakura.modularbot.config.ModularBotConfig;
import enterprises.iwakura.modularbot.managers.ModularBotDataManager;
import enterprises.iwakura.sigewine.core.annotations.RomaritimeBean;
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
