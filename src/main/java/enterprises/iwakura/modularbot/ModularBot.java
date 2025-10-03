package enterprises.iwakura.modularbot;

import dev.mayuna.mayuslibrary.exceptionreporting.UncaughtExceptionReporter;
import enterprises.iwakura.modularbot.base.Module;
import enterprises.iwakura.modularbot.config.ModularBotConfig;
import enterprises.iwakura.modularbot.managers.DefaultModuleManager;
import enterprises.iwakura.ganyu.Ganyu;
import enterprises.iwakura.sigewine.core.Sigewine;
import enterprises.iwakura.sigewine.core.SigewineOptions;
import enterprises.iwakura.sigewine.core.annotations.RomaritimeBean;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Log4j2
@RomaritimeBean
public final class ModularBot {

    private static final @Getter Sigewine sigewine = new Sigewine(SigewineOptions.builder().build());
    private static ModularBot instance;

    private final List<Module> internalModules = new ArrayList<>();

    private final Ganyu ganyu;
    private final ModularBotShardManager modularBotShardManager;
    private final ModularBotConfig config;
    private final DefaultModuleManager moduleManager;

    private boolean running;
    private boolean stopping;

    public ModularBot(
            Ganyu ganyu,
            ModularBotShardManager modularBotShardManager,
            ModularBotConfig config,
            DefaultModuleManager moduleManager) {
        this.ganyu = ganyu;
        this.modularBotShardManager = modularBotShardManager;
        this.config = config;
        this.moduleManager = moduleManager;

        instance = this;
    }

    /**
     * Returns the singleton instance of ModularBot
     *
     * @return the singleton instance of ModularBot
     */
    public static ModularBot get() {
        return instance;
    }

    public void start(String[] args) {
        log.info("Starting ModularDiscordBot @ {}", ModularBotConstants.getVersion());
        log.info("Made by Mayuna");

        log.info("Java Runtime Information:");
        log.info("Java Version: {}", System.getProperty("java.version"));
        log.info("Java Vendor: {}", System.getProperty("java.vendor"));
        log.info("Java VM Name: {}", System.getProperty("java.vm.name"));
        log.info("Java VM Version: {}", System.getProperty("java.vm.version"));
        log.info("Java VM Vendor: {}", System.getProperty("java.vm.vendor"));

        log.info("""
                \s
                \s
                \033[0;35m 88b           d88  88888888ba,    88888888ba
                \033[0;35m 888b         d888  88      `"8b   88      "8b
                \033[0;35m 88`8b       d8'88  88        `8b  88      ,8P
                \033[0;35m 88 `8b     d8' 88  88         88  88aaaaaa8P'
                \033[0;35m 88  `8b   d8'  88  88         88  88\"\"\"\"""8b,
                \033[0;35m 88   `8b d8'   88  88         8P  88      `8b
                \033[0;35m 88    `888'    88  88      .a8P   88      a8P
                \033[0;35m 88     `8'     88  88888888Y"'    88888888P"
                \s
                \033[0;35M =========== Modular Discord Bot 3 ===========
                \033[0;35M ==     Created by: Iwakura Enterprises     ==
                \033[0;35M =============================================\033[0m
                """
        );

        log.info("Loading...");
        final long startMillis = System.currentTimeMillis();

        log.info("Phase 1/5 - Loading core...");

        log.info("Checking configuration");
        checkConfiguration();

        log.info("Registering Shutdown hook");
        registerShutdownHook();

        log.info("Registering UncaughtExceptionReporter");
        registerUncaughtExceptionReporter();

        log.info("Preparing ModuleManager");
        prepareModuleManager();

        log.info("Phase 2/5 - Loading modules...");
        loadModules();

        log.info("Phase 3/5 - Enabling modules...");
        enableModules();

        log.info("Phase 4/5 - Preparing JDA...");

        log.info("Creating ModularBotShardManager...");
        createModularBotShardManager();

        log.info("Initializing modules...");
        initializeModules();

        log.info("Finishing ModularBotShardManager...");
        finishModularBotShardManager();

        log.info("Phase 5/5 - Connecting to Discord...");
        connectToDiscord();

        log.info("Successfully started ModularDiscordBot (took {}ms)", (System.currentTimeMillis() - startMillis));
        running = true;

        log.info("Initializing Presence Activity Cycle...");
        initializePresenceActivityCycle();
    }

    /**
     * Loads configuration
     */
    private void checkConfiguration() {
        // Failed to load
        if (config == null) {
            shutdown();
        }
    }

    /**
     * Registers JVM Shutdown hook
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!stopping) {
                log.warn("Modular Discord Bot has not stopped gracefully! Please, use command 'stop' to stop the application. There is a chance that the modules won't be unloaded fully before JVM termination.");

                log.info("Shutting down Modular Discord Bot...");
                shutdown();
            }
        }));
    }

    /**
     * Registers exception reporter
     */
    private void registerUncaughtExceptionReporter() {
        UncaughtExceptionReporter.register();
        UncaughtExceptionReporter.addExceptionReportConsumer(exceptionReport -> {
            log.warn("Uncaught exception occurred! Sending to modules...", exceptionReport.getThrowable());
            moduleManager.processException(exceptionReport.getThrowable());
        });
    }

    /**
     * Prepares module manager
     */
    private void prepareModuleManager() {
        if (!internalModules.isEmpty()) {
            log.info("Adding {} internal modules...", internalModules.size());
            moduleManager.addInternalModules(internalModules.toArray(new Module[0]));
        }
    }

    /**
     * Loads modules
     */
    private void loadModules() {
        if (!moduleManager.loadModules()) {
            shutdown();
        }
    }

    /**
     * Enables modules
     */
    private void enableModules() {
        moduleManager.enableModules();
    }

    /**
     * Initializes Discord stuff such as CommandClientBuilder, etc.
     */
    private void initializeModules() {
        log.info("Processing Ganyu...");
        moduleManager.processGanyu(ganyu);

        log.info("Processing CommandClientBuilder...");
        moduleManager.processCommandClientBuilder(modularBotShardManager.getCommandClientBuilder());

        log.info("Processing ShardManagerBuilder...");
        moduleManager.processShardBuilder(modularBotShardManager.getShardManagerBuilder());
    }

    /**
     * Creates ShardManager
     */
    private void createModularBotShardManager() {
        log.info("Initializing ModularBotShardManager...");
        if (!modularBotShardManager.init()) {
            shutdown();
        }
    }

    /**
     * Builds shard manager
     */
    private void finishModularBotShardManager() {
        if (!modularBotShardManager.finish()) {
            shutdown();
        }
    }

    /**
     * Connects to Discord
     */
    private void connectToDiscord() {
        if (!modularBotShardManager.connect()) {
            shutdown();
        }
    }

    /**
     * Initializes Presence Activity Cycle
     */
    private void initializePresenceActivityCycle() {
        modularBotShardManager.initPresenceActivityCycle();
    }

    /**
     * Shutdowns ModularDiscordBot
     */
    public void shutdown() {
        stopping = true;

        log.info("Shutting down ModularDiscordBot @ {}", ModularBotConstants.getVersion());

        internalModules.clear();

        log.info("Shutting down Ganyu...");
        ganyu.stop();

        log.info("Unloading modules...");
        moduleManager.unloadModules();

        log.info("Disconnecting from Discord...");
        if (modularBotShardManager != null) {
            modularBotShardManager.shutdown();
        }

        log.info("Shutdown completed");

        log.info("Halting JVM...");
        Runtime.getRuntime().halt(0);
    }

    /**
     * Adds internal module. Added modules will be loaded upon starting the ModularBot. If it's already started, it will be loaded immediately.
     *
     * @param modules Modules to add
     */
    public void addInternalModules(@NonNull Module... modules) {
        if (running) {
            moduleManager.addInternalModules(modules);
            return;
        }

        var listModules = List.of(modules);
        log.info("Adding {} internal modules", listModules.size());
        internalModules.addAll(listModules);
    }
}
