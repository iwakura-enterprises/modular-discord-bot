package dev.mayuna.modularbot.base;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import dev.mayuna.consoleparallax.ConsoleParallax;
import dev.mayuna.modularbot.util.logging.ModularBotLogger;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Module manager
 */
public interface ModuleManager {

    /**
     * Returns ModuleManager's logger
     *
     * @return {@link ModularBotLogger}
     */
    ModularBotLogger getLogger();

    /**
     * Returns list of loaded modules in memory.
     *
     * @return List of modules
     */
    List<Module> getModules();

    /**
     * Adds internal module(s) to memory. If not loaded, they will be loaded and enabled.
     *
     * @param modules Module(s) to add
     */
    void addInternalModules(Module... modules);

    /**
     * Determines if specified module is loaded in memory
     *
     * @param moduleName Module name
     *
     * @return True if module is loaded in memory, false otherwise
     */
    default boolean isModuleLoaded(String moduleName) {
        return getModuleByName(moduleName).orElse(null) != null;
    }

    /**
     * Gets {@link Module} from loaded modules in memory by module name
     *
     * @param moduleName Module name
     *
     * @return Returns optional of {@link Module}
     */
    default Optional<Module> getModuleByName(String moduleName) {
        return getModules().stream().filter(module -> module.getModuleInfo().getName().equals(moduleName)).findAny();
    }

    /**
     * Loads modules from file system to memory. This method must not call {@link Module#onLoad()}<br> If called again, firstly all loaded modules are
     * disabled (if needed) and
     * unloaded, then it proceeds normally.
     *
     * @return Returns if the loading was successful
     */
    boolean loadModules();

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

    default void processCommandClientBuilder(CommandClientBuilder commandClientBuilder) {
        getModules().forEach(module -> module.onCommandClientBuilderInitialization(commandClientBuilder));
    }

    default void processConsoleParallax(ConsoleParallax consoleParallax) {
        getModules().forEach(module -> module.onConsoleCommandRegistration(consoleParallax));
    }

    default void processShardBuilder(DefaultShardManagerBuilder shardManagerBuilder) {
        getModules().forEach(module -> module.onShardManagerBuilderInitialization(shardManagerBuilder));
    }

    default void processException(Throwable throwable) {
        getModules().forEach(module -> {
            try {
                for (var stackTraceElement : throwable.getStackTrace()) {
                    for (String packageName : module.getModuleInfo().getExceptionHandlingPackages()) {
                        if (stackTraceElement.getClassName().contains(packageName)) {
                            module.onUncaughtException(throwable);
                            return;
                        }
                    }
                }
            } catch (Exception exception) {
                getLogger().error("Exception occurred while processing modules with uncaught exception!", exception);
            }
        });
    }
}
