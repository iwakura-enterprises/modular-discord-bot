package dev.mayuna.modularbot.console;

import dev.mayuna.modularbot.ModularBot;
import enterprises.iwakura.ganyu.CommandInvocationContext;
import enterprises.iwakura.ganyu.GanyuCommand;
import enterprises.iwakura.ganyu.annotation.Command;
import enterprises.iwakura.ganyu.annotation.DefaultCommand;
import enterprises.iwakura.ganyu.annotation.Description;
import enterprises.iwakura.ganyu.annotation.Syntax;
import enterprises.iwakura.sigewine.core.annotations.RomaritimeBean;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RomaritimeBean
@RequiredArgsConstructor
@Command("stop")
@Description("Stops the ModularDiscordBot")
@Syntax("")
public final class StopConsoleCommand implements GanyuCommand {

    @DefaultCommand
    public void execute(@NotNull CommandInvocationContext context) {
        var modularBot = ModularBot.getSigewine().syringe(ModularBot.class);
        modularBot.shutdown();
    }
}
