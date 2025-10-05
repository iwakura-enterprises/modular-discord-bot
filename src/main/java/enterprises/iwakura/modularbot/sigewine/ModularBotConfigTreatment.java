package enterprises.iwakura.modularbot.sigewine;

import enterprises.iwakura.modularbot.ModularBotConfig;
import enterprises.iwakura.sigewine.core.annotations.RomaritimeBean;

public class ModularBotConfigTreatment {

    @RomaritimeBean
    public ModularBotConfig modularBotConfig() {
        return ModularBotConfig.load();
    }
}
