package dev.mayuna.modularbot.managers;

import dev.mayuna.pumpk1n.Pumpk1n;
import dev.mayuna.pumpk1n.api.StorageHandler;
import dev.mayuna.pumpk1n.objects.DataHolder;
import lombok.NonNull;

import java.util.UUID;

public class DataManager extends Pumpk1n {

    public static final UUID GLOBAL_DATA_HOLDER_UUID = UUID.nameUUIDFromBytes(new byte[]{0});

    public DataManager(StorageHandler storageHandler) {
        super(storageHandler);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException If the argument is 93b885ad-fe0d-3089-8df6-34904fd59f71 (this UUID is reserved)
     */
    @Override
    public @NonNull DataHolder getOrCreateDataHolder(@NonNull UUID uuid) {
        if (uuid == GLOBAL_DATA_HOLDER_UUID) {
            throw new IllegalArgumentException("This UUID is reserved for Global Data Holder!");
        }

        return super.getOrCreateDataHolder(uuid);
    }

    /**
     * Gets the global {@link DataHolder}
     *
     * @return Non-null {@link DataHolder} (if Global Data Holder does not exist, it creates new one with ID {@link Long#MIN_VALUE}
     */
    public @NonNull DataHolder getGlobalDataHolder() {
        return super.getOrCreateDataHolder(GLOBAL_DATA_HOLDER_UUID);
    }
}
