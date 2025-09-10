package enterprises.iwakura.modularbot.sigewine;

import dev.mayuna.mayusjdautils.MayusJDAUtilities;
import enterprises.iwakura.modularbot.ModularBotStyles;
import enterprises.iwakura.sigewine.core.annotations.RomaritimeBean;

public class MayusJDAUtilitiesTreatment {

    @RomaritimeBean(name = "modularBotMayusJDAUtilities")
    public MayusJDAUtilities mayusJDAUtilities() {
        final var baseMayusJDAUtilities = new MayusJDAUtilities();
        baseMayusJDAUtilities.setMessageInfoStyles(new ModularBotStyles(baseMayusJDAUtilities));
        return baseMayusJDAUtilities;
    }
}
