package dev.mayuna.modularbot.amber;

import enterprises.iwakura.amber.Logger;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ModuleAmberLogger implements Logger {

    /**
     * @param message Message to log
     */
    @Override
    public void info(String message) {
        log.info(message);
    }

    /**
     * @param message Message to log
     */
    @Override
    public void debug(String message) {
        log.debug(message);
    }

    /**
     * @param message   Message to log
     * @param throwable Throwable to log
     */
    @Override
    public void error(String message, Throwable throwable) {
        log.error(message, throwable);
    }
}
