package enterprises.iwakura.modularbot.base;

import java.util.UUID;

/**
 * Base for the Module's tasks
 */
public interface ModuleTask {

    /**
     * Returns task's UUID
     *
     * @return {@link UUID}
     */
    UUID getUUID();

    /**
     * Returns task's owner
     *
     * @return {@link Module}
     */
    Module getOwner();

    /**
     * Determines if the task was cancelled
     *
     * @return True if yes, false otherwise
     */
    boolean isCancelled();

    /**
     * Determines if the task is running
     *
     * @return True if yes, false otherwise
     */
    boolean isRunning();

    /**
     * Cancels the task
     */
    void cancel();

    /**
     * Starts the task
     */
    void start();
}
