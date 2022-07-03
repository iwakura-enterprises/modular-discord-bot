package dev.mayuna.modularbot.objects;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import dev.mayuna.modularbot.logging.MayuLogger;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public abstract class Module {

    private @Getter @Setter ModuleInfo moduleInfo;
    private @Getter @Setter ModuleStatus moduleStatus;
    private @Getter @Setter MayuLogger logger;

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
     * @param commandClientBuilder Non-null {@link CommandClientBuilder}
     */
    public void onCommandClientBuilderInitialization(@NonNull CommandClientBuilder commandClientBuilder) {
        // Empty
    }

    /**
     * This method is called when the JDA is initializing. You can register events here and more.
     * @param shardManagerBuilder Non-null {@link DefaultShardManagerBuilder}
     */
    public void onShardManagerBuilderInitialization(@NonNull DefaultShardManagerBuilder shardManagerBuilder) {
        // Empty
    }

    /**
     * This method is called when some JDA event occur
     * @param genericEvent Non-null {@link GenericEvent}
     */
    public void onGenericEvent(@NonNull GenericEvent genericEvent) {
        // Empty
    }

    /**
     * This method is called when some exception is uncaught
     * @param throwable Non-null {@link Throwable}
     */
    public void onUncaughtException(@NonNull Throwable throwable) {
        // Empty
    }
}
