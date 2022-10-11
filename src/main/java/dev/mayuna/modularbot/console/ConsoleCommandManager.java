package dev.mayuna.modularbot.console;

import dev.mayuna.mayuslibrary.arguments.ArgumentParser;
import dev.mayuna.modularbot.console.commands.basic.HelpConsoleCommand;
import dev.mayuna.modularbot.console.commands.basic.ModularConsoleCommand;
import dev.mayuna.modularbot.console.commands.basic.StopConsoleCommand;
import dev.mayuna.modularbot.console.commands.generic.AbstractConsoleCommand;
import dev.mayuna.modularbot.console.commands.generic.CommandResult;
import dev.mayuna.modularbot.logging.Logger;
import lombok.Getter;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class ConsoleCommandManager {

    // Data
    private static @Getter final List<AbstractConsoleCommand> consoleCommands = new LinkedList<>();

    // Runtime
    private static @Getter Thread commandThread;

    public static void init() {
        registerCommands(new HelpConsoleCommand(),
                         new StopConsoleCommand(),
                         new ModularConsoleCommand()
        );

        if (System.console() == null) {
            Logger.info("Console command thread will not be started - System.console() returned null");
            return;
        }

        startCommandThread();
    }

    public static void registerCommands(AbstractConsoleCommand... consoleCommand) {
        consoleCommands.addAll(List.of(consoleCommand));
    }

    private static void processCommand(String command) {
        if (command == null) {
            return;
        }

        ArgumentParser argumentParser = new ArgumentParser(command);

        if (!argumentParser.hasAnyArguments()) {
            Logger.error("Unknown command '" + command + "'!");
            return;
        }

        String name = argumentParser.getArgumentAtIndex(0).getValue();
        String arguments = "";

        if (argumentParser.hasArgumentAtIndex(1)) {
            arguments = argumentParser.getAllArgumentsAfterIndex(1).getValue();
        }

        for (AbstractConsoleCommand abstractConsoleCommand : consoleCommands) {
            if (abstractConsoleCommand.name.equalsIgnoreCase(name)) {

                String finalArguments = arguments;
                new Thread(() -> {
                    try {
                        CommandResult commandResult = abstractConsoleCommand.execute(finalArguments);

                        if (commandResult == CommandResult.INCORRECT_SYNTAX) {
                            Logger.error("Invalid syntax! Syntax: " + abstractConsoleCommand.name + " " + abstractConsoleCommand.syntax);
                        }
                    } catch (Exception exception) {
                        Logger.throwing(exception);
                        Logger.error("Exception occurred while executing command '" + command + "'!");
                    }
                }, "#CommandThread_" + abstractConsoleCommand.name).start();
                return;
            }
        }

        Logger.error("Unknown command '" + command + "'!");
    }

    private static void startCommandThread() {
        commandThread = new Thread(() -> {
            while (true) {
                String command = System.console().readLine();
                processCommand(command);
            }
        }, "COMMAND-READER");
        commandThread.start();
    }
}
