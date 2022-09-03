package dev.mayuna.modularbot.managers;

import dev.mayuna.pumpk1n.Pumpk1n;
import dev.mayuna.pumpk1n.api.StorageHandler;
import dev.mayuna.pumpk1n.objects.DataHolder;
import lombok.NonNull;

import java.util.UUID;

public class DataManager extends Pumpk1n {

    public static final UUID GLOBAL_DATA_HOLDER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public DataManager(StorageHandler storageHandler) {
        super(storageHandler);
    }

    /**
     * {@inheritDoc}
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
