package dev.mayuna.modularbot.config.storage;

import dev.mayuna.pumpk1n.impl.SQLiteStorageHandler;
import lombok.Getter;

/**
 * Settings for SQL Lite storage
 */
@SuppressWarnings("FieldMayBeFinal")
@Getter
public final class SqliteStorageSettings {

    private String path;
    private String tableName = "data";

    /**
     * Used for serialization
     */
    public SqliteStorageSettings() {
        this("default");
    }

    /**
     * Creates a new storage settings (used when creating new config file with default values)
     *
     * @param defaultDatabaseFileName The default database file name
     */
    public SqliteStorageSettings(String defaultDatabaseFileName) {
        this.path = "./" + defaultDatabaseFileName + ".db";
    }

    /**
     * Creates a storage handler from the settings
     *
     * @return The storage handler
     */
    public SQLiteStorageHandler createStorageHandler() {
        return new SQLiteStorageHandler(new SQLiteStorageHandler.Settings(null, path, tableName));
    }
}