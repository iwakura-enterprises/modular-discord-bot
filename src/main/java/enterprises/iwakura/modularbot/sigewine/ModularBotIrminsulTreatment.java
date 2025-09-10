package enterprises.iwakura.modularbot.sigewine;

import enterprises.iwakura.modularbot.config.ModularBotConfig;
import enterprises.iwakura.modularbot.irminsul.ModularBotIrminsul;
import enterprises.iwakura.sigewine.core.annotations.RomaritimeBean;
import lombok.RequiredArgsConstructor;

@RomaritimeBean
@RequiredArgsConstructor
public class ModularBotIrminsulTreatment {

    private final ModularBotConfig config;

    @RomaritimeBean
    public ModularBotIrminsul modularBotIrminsul() {
        return new ModularBotIrminsul(config.getIrminsul().getDatabase());
    }
}
