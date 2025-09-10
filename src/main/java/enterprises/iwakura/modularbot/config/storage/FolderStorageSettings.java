package enterprises.iwakura.modularbot.config.storage;

import dev.mayuna.pumpk1n.impl.FolderStorageHandler;

@SuppressWarnings("FieldMayBeFinal")
public final class FolderStorageSettings {

    private String path;

    /**
     * Used for serialization
     */
    public FolderStorageSettings() {
        this("default");
    }

    /**
     * Creates a new folder storage settings (used when creating new config file with default values)
     *
     * @param defaultDirectoryName The default directory name
     */
    public FolderStorageSettings(String defaultDirectoryName) {
        this.path = "./" + defaultDirectoryName + "/";
    }

    /**
     * Creates a storage handler from the settings
     *
     * @return The storage handler
     */
    public FolderStorageHandler createStorageHandler() {
        return new FolderStorageHandler(path);
    }
}