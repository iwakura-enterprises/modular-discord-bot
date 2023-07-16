package dev.mayuna.modularbot.objects;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import dev.mayuna.mayusjdautils.MayusJDAUtilities;
import dev.mayuna.modularbot.concurrent.ModularScheduler;
import dev.mayuna.modularbot.logging.MayuLogger;
import dev.mayuna.modularbot.objects.activity.ModuleActivities;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;

public abstract class Module {

    private final @Getter ModuleActivities moduleActivities = new ModuleActivities(this);

    private @Getter @Setter ModuleInfo moduleInfo;
    private @Getter @Setter ModuleStatus moduleStatus;
    private @Getter @Setter ModuleConfig moduleConfig;
    private @Getter @Setter MayuLogger logger;
    private @Getter @Setter MayusJDAUtilities mayusJDAUtilities;

    private @Getter @Setter ModularScheduler scheduler;

    /**
     * This method is called when the module is loaded
     */
    public void onLoad() {
        // Empty
    }

    /**
     * This method is called when the module is enabling
     */
    public abstract void onEnable();

    /**
     * This method is called when the module is disabling
     */
    public abstract void onDisable();

    /**
     * This method is called when the module is unloaded
     */
    public void onUnload() {
        // Empty
    }

    /**
     * This method is called when the JDA Utilities' {@link CommandClientBuilder} is initializing. You cna register commands here and more.
     *
     * @param commandClientBuilder Non-null {@link CommandClientBuilder}
     */
    public void onCommandClientBuilderInitialization(@NonNull CommandClientBuilder commandClientBuilder) {
        // Empty
    }

    /**
     * This method is called when the JDA is initializing. You can register events here and more.
     *
     * @param shardManagerBuilder Non-null {@link DefaultShardManagerBuilder}
     */
    public void onShardManagerBuilderInitialization(@NonNull DefaultShardManagerBuilder shardManagerBuilder) {
        // Empty
    }

    /**
     * This method is called when some exception is uncaught
     *
     * @param throwable Non-null {@link Throwable}
     */
    public void onUncaughtException(@NonNull Throwable throwable) {
        // Empty
    }
}
