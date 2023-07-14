package dev.mayuna.modularbot.events;

import dev.mayuna.modularbot.managers.WrappedShardManager;
import lombok.Getter;

public class WrappedShardManagerInitializedEvent implements GenericModularEvent {

    private final @Getter WrappedShardManager wrappedShardManager;

    public WrappedShardManagerInitializedEvent(WrappedShardManager wrappedShardManager) {
        this.wrappedShardManager = wrappedShardManager;
    }
}
