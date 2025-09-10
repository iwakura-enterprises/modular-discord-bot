package dev.mayuna.modularbot.sigewine;

import dev.mayuna.modularbot.config.ModularBotConfig;
import dev.mayuna.modularbot.irminsul.ModularBotIrminsul;
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
