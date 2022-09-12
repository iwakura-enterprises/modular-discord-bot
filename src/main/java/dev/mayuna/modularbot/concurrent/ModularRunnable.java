package dev.mayuna.modularbot.concurrent;

import dev.mayuna.modularbot.objects.Module;
import lombok.Getter;

import java.util.UUID;

public class ModularRunnable implements ModularTask {

    private final UUID uuid = UUID.randomUUID();
    private final Module owner;
    private final @Getter Runnable runnable;
    private final boolean sync;
    private boolean running;

    private ModularRunnable(Module owner, Runnable runnable, boolean sync) {
        this.owner = owner;
        this.runnable = runnable;
        this.sync = sync;
    }

    public static ModularRunnable createSync(Module owner, Runnable runnable) {
        return new ModularRunnable(owner, runnable, true);
    }

    public static ModularRunnable createAsync(Module owner, Runnable runnable) {
        return new ModularRunnable(owner, runnable, false);
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
    public boolean isSync() {
        return sync;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void cancel() {
        owner.getScheduler().removeTask(this);
    }

    @Override
    public void start() {
        running = true;
        runnable.run();
        running = false;
        cancel();
    }
}
