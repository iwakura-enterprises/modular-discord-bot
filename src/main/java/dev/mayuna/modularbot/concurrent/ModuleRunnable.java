package dev.mayuna.modularbot.concurrent;

import dev.mayuna.modularbot.base.Module;
import dev.mayuna.modularbot.base.ModuleTask;
import lombok.Getter;
import lombok.NonNull;

import java.util.UUID;

/**
 * {@link Module}'s async runnable task
 */
@Getter
public final class ModuleRunnable implements ModuleTask {

    private final UUID uuid = UUID.randomUUID();
    private final Module owner;
    private final Runnable runnable;
    private boolean running;

    private boolean ran;

    /**
     * Creates new {@link ModuleRunnable}
     *
     * @param owner    Non-null {@link Module}
     * @param runnable Non-null {@link Runnable}
     */
    private ModuleRunnable(@NonNull Module owner, @NonNull Runnable runnable) {
        this.owner = owner;
        this.runnable = runnable;
    }

    /**
     * Creates async {@link ModuleRunnable}
     *
     * @param owner    Non-null {@link Module}
     * @param runnable Non-null {@link Runnable}
     *
     * @return {@link ModuleRunnable}
     */
    static ModuleRunnable create(@NonNull Module owner, @NonNull Runnable runnable) {
        return new ModuleRunnable(owner, runnable);
    }

    /**
     * Task's UUID
     *
     * @return {@link UUID}
     */
    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    /**
     * Removes this task from the Module's scheduler, however, does not interrupt any running tasks
     */
    @Override
    public void cancel() {
        owner.getScheduler().removeTask(this);
    }

    @Override
    public void start() {
        running = true;
        runnable.run();
        running = false;
        ran = true;
        cancel();
    }
}
