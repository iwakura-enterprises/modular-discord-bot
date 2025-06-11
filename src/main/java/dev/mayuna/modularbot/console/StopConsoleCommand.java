package dev.mayuna.modularbot.console;

import dev.mayuna.consoleparallax.BaseCommand;
import dev.mayuna.consoleparallax.CommandInvocationContext;
import dev.mayuna.modularbot.ModularBot;
import enterprises.iwakura.sigewine.core.annotations.RomaritimeBean;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@RomaritimeBean
@RequiredArgsConstructor
public final class StopConsoleCommand implements BaseCommand {

    private final ModularBot modularBot;

    @Override
    public @NotNull String getName() {
        return "stop";
    }

    @Override
    public @NotNull String getUsage() {
        return "Stops the ModularDiscordBot";
    }

    @Override
    public @NotNull String getSyntax() {
        return "N/A";
    }

    @Override
    public @NotNull String getDescription() {
        return "Stops the ModularDiscordBot";
    }

    @Override
    public void execute(@NotNull CommandInvocationContext context) {
        modularBot.shutdown();
    }
}
