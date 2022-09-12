package dev.mayuna.modularbot.concurrent;

import dev.mayuna.modularbot.objects.Module;

import java.util.UUID;

public interface ModularTask {

    UUID getUUID();

    Module getOwner();

    boolean isSync();

    boolean isCancelled();

    boolean isRunning();

    void cancel();

    void start();
}
