package enterprises.iwakura.modularbot.console;

import enterprises.iwakura.modularbot.ModularBot;
import enterprises.iwakura.ganyu.Output;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * {@link Output} implementation for {@link ModularBot}'s {@link enterprises.iwakura.ganyu.Ganyu}
 */
@Slf4j
public class ModularBotGanyuOutput implements Output {

    /**
     * @param message Message to print
     */
    @Override
    public void info(@NotNull String message) {
        log.info(message);
    }

    /**
     * @param message   Message to print
     * @param throwable Throwable to print
     */
    @Override
    public void error(String message, Throwable throwable) {
        log.error(message, throwable);
    }
}
