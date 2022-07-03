package dev.mayuna.modularbot.console.commands.basic;

import dev.mayuna.mayuslibrary.logging.Logger;
import dev.mayuna.modularbot.console.ConsoleCommandManager;
import dev.mayuna.modularbot.console.commands.generic.AbstractConsoleCommand;
import dev.mayuna.modularbot.console.commands.generic.CommandResult;

public class HelpConsoleCommand extends AbstractConsoleCommand {

    public HelpConsoleCommand() {
        this.name = "help";
        this.syntax = "";
    }

    @Override
    public CommandResult execute(String arguments) {
        Logger.info("=== Loaded Commands (" + ConsoleCommandManager.getConsoleCommands().size() + ") ===");
        for (var consoleCommand : ConsoleCommandManager.getConsoleCommands()) {
            Logger.info("> " + consoleCommand.name + " " + consoleCommand.syntax);
        }

        return CommandResult.SUCCESS;
    }
}
