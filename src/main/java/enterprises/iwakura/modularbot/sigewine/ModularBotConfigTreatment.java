package enterprises.iwakura.modularbot.sigewine;

import enterprises.iwakura.modularbot.ModularBotConfig;
import enterprises.iwakura.sigewine.core.annotations.Bean;

public class ModularBotConfigTreatment {

    @Bean
    public ModularBotConfig modularBotConfig() {
        return ModularBotConfig.load();
    }
}
