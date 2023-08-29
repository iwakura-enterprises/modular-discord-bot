package dev.mayuna.modularbot;

import dev.mayuna.mayusjdautils.MayusJDAUtilities;
import dev.mayuna.mayusjdautils.styles.MessageInfoStyles;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

public class ModularBotStyles extends MessageInfoStyles {

    public ModularBotStyles(MayusJDAUtilities mayusJDAUtilities) {
        super(mayusJDAUtilities);
    }

    @NotNull
    @Override
    public EmbedBuilder getDefaultEmbedStyle() {
        return super.getDefaultEmbedStyle().setFooter("Powered by Modular Discord Bot");
    }
}
