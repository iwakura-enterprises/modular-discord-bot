package dev.mayuna.modularbot.config;

import dev.mayuna.modularbot.config.storage.*;
import dev.mayuna.pumpk1n.api.StorageHandler;
import lombok.Getter;

/**
 * Settings for storage
 */
@SuppressWarnings("FieldMayBeFinal")
@Getter
public final class StorageSettings {

    private StorageLogLevelSettings logLevel = StorageLogLevelSettings.DEBUG;
    private boolean logOperations = false;
    private StorageTypeSettings storageType = StorageTypeSettings.SQL_LITE;
    private FolderStorageSettings folderStorageSettings;
    private SqliteStorageSettings sqliteStorageSettings;
    private SqlStorageSettings sqlStorageSettings;

    /**
     * Used for serialization
     *
     * @deprecated Used for serialization, please use {@link #StorageSettings(String)}
     */
    @Deprecated
    public StorageSettings() {
        this("default");
    }

    /**
     * Creates a new storage settings (used when creating new config file with default values)
     *
     * @param defaultStorageName The default storage name (for folder storage and SQL Lite storage)
     */
    public StorageSettings(String defaultStorageName) {
        folderStorageSettings = new FolderStorageSettings(defaultStorageName);
        sqliteStorageSettings = new SqliteStorageSettings(defaultStorageName);
        sqlStorageSettings = new SqlStorageSettings(defaultStorageName);
    }

    /**
     * Creates a storage handler from the settings
     *
     * @return The storage handler
     */
    public StorageHandler createStorageHandler() {
        return switch (storageType) {
            case StorageTypeSettings.FOLDER -> folderStorageSettings.createStorageHandler();
            case StorageTypeSettings.SQL_LITE -> sqliteStorageSettings.createStorageHandler();
            case StorageTypeSettings.SQL -> sqlStorageSettings.createStorageHandler();
        };
    }
}
