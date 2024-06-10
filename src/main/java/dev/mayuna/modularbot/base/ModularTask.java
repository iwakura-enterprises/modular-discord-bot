package dev.mayuna.modularbot.base;

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
