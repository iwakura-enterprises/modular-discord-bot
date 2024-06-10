package dev.mayuna.modularbot.managers;

import dev.mayuna.modularbot.config.StorageSettings;
import dev.mayuna.modularbot.util.logging.ModularBotLogger;
import dev.mayuna.pumpk1n.Pumpk1n;
import dev.mayuna.pumpk1n.objects.DataHolder;
import dev.mayuna.pumpk1n.util.BaseLogger;
import lombok.NonNull;

import java.util.UUID;

/**
 * Extension to the {@link Pumpk1n}
 */
public final class ModularBotDataManager extends Pumpk1n {

    public static final UUID GLOBAL_DATA_HOLDER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final ModularBotLogger LOGGER = ModularBotLogger.create("DataManager");
    private final StorageSettings storageSettings;

    /**
     * Creates new {@link ModularBotDataManager}
     *
     * @param storageSettings {@link StorageSettings} to use
     */
    public ModularBotDataManager(StorageSettings storageSettings) {
        super(storageSettings.createStorageHandler());
        this.storageSettings = storageSettings;

        createLogger();
        getLogger().log("Using StorageHandler: " + storageHandler.getName());
    }

    /**
     * Creates logger
     */
    private void createLogger() {
        this.setLogger(new BaseLogger() {
            @Override
            public void log(@NonNull String message, Throwable throwable) {
                if (throwable == null) {
                    LOGGER.log(storageSettings.getLogLevel().getLog4jLevel(), message);
                } else {
                    LOGGER.log(storageSettings.getLogLevel().getLog4jLevel(), message, throwable);
                }
            }
        });
    }

    /**
     * Gets or loads {@link DataHolder} by its id. If it did not load, it creates new {@link DataHolder} and loads it.
     *
     * @param uuid Non-null {@link UUID}
     *
     * @return Non-null {@link DataHolder}
     *
     * @throws IllegalArgumentException If the argument is {@link #GLOBAL_DATA_HOLDER_UUID}
     */
    @Override
    public @NonNull DataHolder getOrCreateDataHolder(@NonNull UUID uuid) {
        if (uuid == GLOBAL_DATA_HOLDER_UUID) {
            throw new IllegalArgumentException("This UUID is reserved for Global Data Holder!");
        }

        return super.getOrCreateDataHolder(uuid);
    }

    /**
     * Gets the global {@link DataHolder} with {@link #GLOBAL_DATA_HOLDER_UUID}
     *
     * @return Non-null {@link DataHolder}
     */
    public @NonNull DataHolder getGlobalDataHolder() {
        return super.getOrCreateDataHolder(GLOBAL_DATA_HOLDER_UUID);
    }
}
