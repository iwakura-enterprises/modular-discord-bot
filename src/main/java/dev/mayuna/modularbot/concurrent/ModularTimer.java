package dev.mayuna.modularbot.concurrent;

import dev.mayuna.modularbot.objects.Module;
import lombok.Getter;

import java.util.Timer;
import java.util.UUID;

public class ModularTimer implements ModularTask {

    private final UUID uuid = UUID.randomUUID();
    private final Module owner;
    private final @Getter Timer instance = new Timer("ModularScheduler-Timer-" + uuid);
    private boolean running;
    private boolean cancelled;

    private ModularTimer(Module owner) {
        this.owner = owner;
    }

    public static ModularTimer create(Module owner) {
        return new ModularTimer(owner);
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
        return false;
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
    }

    @Override
    public void start() {
        running = true;
    }
}
