package dev.mayuna.modularbot.concurrent;

import dev.mayuna.modularbot.base.Module;
import dev.mayuna.modularbot.base.ModuleTask;
import lombok.Getter;
import lombok.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Scheduler for Module's tasks
 */
public final class ModuleScheduler {

    public static final String THREAD_NAME_FORMAT = ModuleScheduler.class.getSimpleName() + "-Task-%s";

    private final Executor taskExecutor = Executors.newCachedThreadPool();
    private final @Getter Module module;
    private final Map<ModuleTask, Thread> tasks = Collections.synchronizedMap(new HashMap<>());
    private final ModuleTaskTimer taskTimer = createTimer();

    /**
     * Creates new {@link ModuleScheduler}
     *
     * @param module Non-null {@link Module}
     */
    public ModuleScheduler(@NonNull Module module) {
        this.module = module;
    }

    /**
     * Creates asynchronous task
     *
     * @param runnable Non-null {@link Runnable}
     */
    public void runAsync(Runnable runnable) {
        runTask(ModuleRunnable.create(module, runnable));
    }

    /**
     * Schedules periodical task
     *
     * @param runnable Non-null {@link Runnable}
     * @param delay    Delay before the first execution
     * @param period   Period between executions
     */
    public void schedule(Runnable runnable, long delay, long period) {
        taskTimer.getInstance().schedule(new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        }, delay, period);
    }

    /**
     * Schedules periodical task
     *
     * @param runnable Non-null {@link Runnable}
     * @param delay    Delay before the first execution
     */
    public void schedule(Runnable runnable, long delay) {
        taskTimer.getInstance().schedule(new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        }, delay);
    }

    /**
     * Schedules fixed periodical task
     *
     * @param runnable Non-null {@link Runnable}
     * @param delay    Delay before the first execution
     * @param period   Fixed period between executions
     */
    public void scheduleFixed(Runnable runnable, long delay, long period) {
        taskTimer.getInstance().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        }, delay, period);
    }

    /**
     * Creates {@link ModuleTaskTimer}
     *
     * @return {@link ModuleTaskTimer}
     */
    private ModuleTaskTimer createTimer() {
        ModuleTaskTimer moduleTaskTimer = ModuleTaskTimer.create(module);
        runTask(moduleTaskTimer);
        return moduleTaskTimer;
    }

    /**
     * Runs specified {@link ModuleTask}
     *
     * @param moduleTask Non-null {@link ModuleTask}
     */
    private void runTask(ModuleTask moduleTask) {
        if (moduleTask instanceof ModuleRunnable) {
            taskExecutor.execute(() -> {
                Thread.currentThread().setName(THREAD_NAME_FORMAT.formatted(moduleTask.getUUID()));
                tasks.put(moduleTask, Thread.currentThread());
                moduleTask.start();
                tasks.remove(moduleTask);
            });
            return;
        }

        if (moduleTask instanceof ModuleTaskTimer) {
            tasks.put(moduleTask, null);
        }
    }

    /**
     * Cancels all {@link ModuleTask}s
     */
    public void cancelTasks() {
        Map<ModuleTask, Thread> tasksCopy = new HashMap<>(this.tasks);

        tasksCopy.forEach((moduleTask, thread) -> {
            try {
                moduleTask.cancel();
            } catch (Exception ignored) {
            }
        });

        tasks.clear();
    }

    /**
     * Removes {@link ModuleTask}
     *
     * @param moduleTask Non-null {@link ModuleTask}
     */
    public void removeTask(ModuleTask moduleTask) {
        tasks.remove(moduleTask);
    }
}
