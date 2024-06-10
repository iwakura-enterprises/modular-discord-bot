package dev.mayuna.modularbot.concurrent;

import dev.mayuna.modularbot.base.Module;
import dev.mayuna.modularbot.base.ModuleTask;
import lombok.Getter;
import lombok.NonNull;

import java.util.Timer;
import java.util.UUID;

/**
 * Holds task which is run on a {@link Timer}
 */
public class ModuleTaskTimer implements ModuleTask {

    private final UUID uuid = UUID.randomUUID();
    private final Module owner;
    private final @Getter Timer instance = new Timer(ModuleScheduler.THREAD_NAME_FORMAT.formatted(uuid));
    private boolean running;
    private boolean cancelled;

    /**
     * Creates new {@link ModuleTaskTimer}
     *
     * @param owner Non-null {@link Module}
     */
    private ModuleTaskTimer(@NonNull Module owner) {
        this.owner = owner;
    }

    /**
     * Creates new {@link ModuleTaskTimer}
     *
     * @param owner Non-null {@link Module}
     *
     * @return {@link ModuleTaskTimer}
     */
    static ModuleTaskTimer create(Module owner) {
        return new ModuleTaskTimer(owner);
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public Module getOwner() {
        return owner;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void cancel() {
        instance.cancel();
        instance.purge();
        running = false;
        cancelled = true;
        owner.getScheduler().removeTask(this);
    }

    @Override
    public void start() {
        running = true;
    }
}
