package dev.mayuna.modularbot.concurrent;

import dev.mayuna.modularbot.base.ModularTask;
import dev.mayuna.modularbot.base.Module;
import lombok.Getter;

import java.util.*;

public class ModularScheduler {

    private final @Getter Module module;
    private final @Getter Map<ModularTask, Thread> tasks = Collections.synchronizedMap(new HashMap<>());

    public ModularScheduler(Module module) {
        this.module = module;
    }

    public void createRunnableSync(Runnable runnable) {
        runTask(ModularRunnable.createSync(module, runnable));
    }

    public void createRunnableAsync(Runnable runnable) {
        runTask(ModularRunnable.createAsync(module, runnable));
    }

    public ModularTimer createTimer() {
        ModularTimer modularTimer = ModularTimer.create(module);
        runTask(modularTimer);
        return modularTimer;
    }

    private void runTask(ModularTask modularTask) {
        if (modularTask instanceof ModularRunnable) {
            Thread taskThread = new Thread(() -> {
                tasks.put(modularTask, Thread.currentThread());
                modularTask.start();
                tasks.remove(modularTask);
            });
            taskThread.setName("ModularScheduler-Task-" + modularTask.getUUID());
            taskThread.start();

            if (modularTask.isSync()) {
                try {
                    taskThread.wait();
                } catch (InterruptedException exception) {
                    throw new RuntimeException("Interrupted while waiting before sync task " + modularTask.getUUID() + " finishes!", exception);
                }
            }
        } else if (modularTask instanceof ModularTimer) {
            tasks.put(modularTask, null);
        }
    }

    public void cancelTasks() {
        Map<ModularTask, Thread> tasksCopy = new HashMap<>(this.tasks);

        tasksCopy.forEach((modularTask, thread) -> {
            try {
                modularTask.cancel();
            } catch (Exception ignored) {
            }
        });

        tasks.clear();
    }

    public void removeTask(ModularTask modularTask) {
        tasks.remove(modularTask);
    }
}
