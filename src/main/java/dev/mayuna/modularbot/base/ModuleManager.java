package dev.mayuna.modularbot.base;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import dev.mayuna.modularbot.logging.Logger;
import dev.mayuna.modularbot.objects.Module;
import dev.mayuna.modularbot.utils.CustomJarClassLoader;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;

import java.util.List;
import java.util.Optional;

public interface ModuleManager {

    /**
     * Returns list of loaded modules in memory.
     * @return List of modules
     */
    List<Module> getModules();

    /**
     * Determines if specified module is loaded in memory
     *
     * @param moduleName Module name
     *
     * @return True if module is loaded in memory, false otherwise
     */
    boolean isModuleLoaded(String moduleName);

    /**
     * Gets {@link Module} from loaded modules in memory by module name
     *
     * @param moduleName Module name
     *
     * @return Returns optional of {@link Module}
     */
    Optional<Module> getModuleByName(String moduleName);

    /**
     * Loads modules from file system to memory. This method must not call {@link Module#onLoad()}<br> If called again, firstly all loaded modules are
     * disabled (if needed) and unloaded, then it proceeds normally.
     *
     * @return Returns list of loaded modules
     */
    List<Module> loadModules();

    /**
     * Loads module (must call {@link Module#onLoad()}
     *
     * @param module Module object
     */
    void loadModule(Module module);

    /**
     * Enables all modules. Should just iterate through all loaded modules and call {@link #enableModule(Module)}
     */
    void enableModules();

    /**
     * Enables module. Must respect module's dependencies, etc.
     *
     * @param module Module object
     */
    void enableModule(Module module);

    /**
     * Unloads all modules. Should just iterate through all loaded modules and call {@link #unloadModule(Module)}
     */
    void unloadModules();

    /**
     * Disables (if needed) and unloads module
     *
     * @param module Module object
     */
    void unloadModule(Module module);

    CustomJarClassLoader getJarClassLoader();

    default void processCommandClientBuilder(CommandClientBuilder commandClientBuilder) {
        getModules().forEach(module -> module.onCommandClientBuilderInitialization(commandClientBuilder));
    }

    default void processShardBuilder(DefaultShardManagerBuilder shardManagerBuilder) {
        getModules().forEach(module -> module.onShardManagerBuilderInitialization(shardManagerBuilder));
    }

    default void processException(Throwable throwable) {
        getModules().forEach(module -> {
            try {
                for (var stackTraceElement : throwable.getStackTrace()) {
                    for (String packageName : module.getModuleInfo().exceptionHandlingPackages()) {
                        if (stackTraceElement.getClassName().contains(packageName)) {
                            module.onUncaughtException(throwable);
                            return;
                        }
                    }
                }
            } catch (Exception exception) {
                Logger.get().error("Exception occurred while processing modules with uncaught exception!", exception);
            }
        });
    }
}
