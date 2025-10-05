package enterprises.iwakura.modularbot.base;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import enterprises.iwakura.ganyu.Ganyu;
import enterprises.iwakura.modularbot.config.ModuleConfig;
import enterprises.iwakura.modularbot.objects.ModuleInfo;
import enterprises.iwakura.modularbot.objects.ModuleStatus;
import enterprises.iwakura.modularbot.objects.activity.ModuleActivities;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;

import java.util.List;

@Getter
@Setter
public abstract class Module<TConfig extends ModuleConfig> {

    private final ModuleActivities moduleActivities = new ModuleActivities(this);

    private ModuleInfo moduleInfo;
    private TConfig moduleConfig;
    private ModuleStatus moduleStatus;

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
     * This method is called when Modular Bot is registering console commands
     *
     * @param ganyu Non-null {@link Ganyu}
     */
    public void onConsoleCommandRegistration(@NonNull Ganyu ganyu) {
    }

    /**
     * This method is called when some exception is uncaught
     *
     * @param throwable Non-null {@link Throwable}
     */
    public void onUncaughtException(@NonNull Throwable throwable) {
        // Empty
    }

    /**
     * Gets a list of all Irminsul entities (JPA-like entities) used by this module. These will be registered automatically.
     *
     * @return Non-null list of JPA entities
     */
    public List<Class<?>> getIrminsulEntities() {
        return List.of();
    }

    /**
     * Sets the module configuration. This is called by Modular Bot itself.
     *
     * @param moduleConfig Non-null {@link ModuleConfig}
     */
    public void setModuleConfig(ModuleConfig moduleConfig) {
        this.moduleConfig = (TConfig) moduleConfig;
    }
}
