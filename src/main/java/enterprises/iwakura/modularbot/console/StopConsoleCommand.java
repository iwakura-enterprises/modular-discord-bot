package enterprises.iwakura.modularbot.console;

import enterprises.iwakura.modularbot.ModularBot;
import enterprises.iwakura.ganyu.CommandInvocationContext;
import enterprises.iwakura.ganyu.GanyuCommand;
import enterprises.iwakura.ganyu.annotation.Command;
import enterprises.iwakura.ganyu.annotation.DefaultCommand;
import enterprises.iwakura.ganyu.annotation.Description;
import enterprises.iwakura.ganyu.annotation.Syntax;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import enterprises.iwakura.sigewine.core.utils.BeanAccessor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Bean
@RequiredArgsConstructor
@Command("stop")
@Description("Stops the ModularDiscordBot")
@Syntax("")
public final class StopConsoleCommand implements GanyuCommand {

    @Bean
    private final BeanAccessor<ModularBot> modularBotAccessor = new BeanAccessor<>(ModularBot.class);

    @DefaultCommand
    public void execute(@NotNull CommandInvocationContext context) {
        modularBotAccessor.getBeanInstance().shutdown();
    }
}
