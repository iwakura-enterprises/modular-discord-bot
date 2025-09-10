package enterprises.iwakura.modularbot.config.storage;

import enterprises.iwakura.modularbot.config.StorageSettings;
import lombok.Getter;
import org.apache.logging.log4j.Level;

/**
 * Configuration for the log levels (used in {@link StorageSettings}
 */
@Getter
public enum StorageLogLevelSettings {
    INFO(Level.INFO),
    DEBUG(Level.DEBUG),
    TRACE(Level.TRACE),
    WARN(Level.WARN),
    ERROR(Level.ERROR),
    FATAL(Level.FATAL);

    private final Level log4jLevel;

    StorageLogLevelSettings(Level log4jLevel) {
        this.log4jLevel = log4jLevel;
    }
}