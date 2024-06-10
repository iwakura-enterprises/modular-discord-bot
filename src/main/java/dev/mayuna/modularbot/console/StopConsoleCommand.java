package dev.mayuna.modularbot.console;

import dev.mayuna.consoleparallax.BaseCommand;
import dev.mayuna.consoleparallax.CommandInvocationContext;
import dev.mayuna.modularbot.ModularBot;
import org.jetbrains.annotations.NotNull;

public class StopConsoleCommand implements BaseCommand {

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
        ModularBot.shutdown();
    }
}
