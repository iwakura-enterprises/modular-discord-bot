package enterprises.iwakura.modularbot.console;

import enterprises.iwakura.modularbot.ModularBot;
import enterprises.iwakura.ganyu.Output;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

/**
 * {@link Output} implementation for {@link ModularBot}'s {@link enterprises.iwakura.ganyu.Ganyu}
 */
@Log4j2
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
