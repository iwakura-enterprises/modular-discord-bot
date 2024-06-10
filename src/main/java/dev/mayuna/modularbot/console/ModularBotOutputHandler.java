package dev.mayuna.modularbot.console;

import dev.mayuna.consoleparallax.OutputHandler;
import dev.mayuna.modularbot.ModularBot;
import dev.mayuna.modularbot.util.logging.ModularBotLogger;
import org.jetbrains.annotations.NotNull;

/**
 * {@link OutputHandler} implementation for {@link ModularBot}'s {@link dev.mayuna.consoleparallax.ConsoleParallax}
 */
public class ModularBotOutputHandler implements OutputHandler {

    private static final ModularBotLogger LOGGER = ModularBotLogger.create("Console");

    /**
     * @param message Message to print
     */
    @Override
    public void info(@NotNull String message) {
        LOGGER.info(message);
    }

    /**
     * @param message Message to print
     */
    @Override
    public void error(@NotNull String message) {
        LOGGER.error(message);
    }
}
