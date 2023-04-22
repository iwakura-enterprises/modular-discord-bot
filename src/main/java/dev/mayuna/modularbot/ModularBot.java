package dev.mayuna.modularbot;

import com.google.common.eventbus.EventBus;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import dev.mayuna.mayusjdautils.interactive.InteractiveListener;
import dev.mayuna.mayuslibrary.exceptionreporting.ExceptionListener;
import dev.mayuna.mayuslibrary.exceptionreporting.ExceptionReporter;
import dev.mayuna.modularbot.console.ConsoleCommandManager;
import dev.mayuna.modularbot.console.commands.generic.AbstractConsoleCommand;
import dev.mayuna.modularbot.listeners.GlobalListener;
import dev.mayuna.modularbot.logging.Logger;
import dev.mayuna.modularbot.managers.DataManager;
import dev.mayuna.modularbot.managers.ModuleManagerImpl;
import dev.mayuna.modularbot.managers.WrappedShardManager;
import dev.mayuna.modularbot.utils.ModularBotConfig;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;

public class ModularBot {

    private static final @Getter EventBus globalEventBus = new EventBus("modular_bot-global");
    private static @Getter DataManager dataManager;
    private static @Getter @Setter boolean stopping;
    private static @Getter @Setter boolean dontSaveData = false;
    private static @Getter WrappedShardManager wrappedShardManager;
    private static @Getter CommandClientBuilder commandClientBuilder;

    /**
     * Main function of ModularBot
     *
     * @param args Arguments
     */
    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            Logger.warn("There are no args, yet.");
            return;
        }

        Logger.info("""
                            \s
                            \033[0;35m  __  __         _      _            ___  _                   _   ___      _  \s
                            \033[0;35m |  \\/  |___  __| |_  _| |__ _ _ _  |   \\(_)___ __ ___ _ _ __| | | _ ) ___| |_\s
                            \033[0;35m | |\\/| / _ \\/ _` | || | / _` | '_| | |) | (_-</ _/ _ \\ '_/ _` | | _ \\/ _ \\  _|
                            \033[0;35m |_|  |_\\___/\\__,_|\\_,_|_\\__,_|_|   |___/|_/__/\\__\\___/_| \\__,_| |___/\\___/\\__|
                            """);

        Logger.info("@ Version: " + Values.APP_VERSION);
        Logger.info("Made by Mayuna\n");
        Logger.info("Loading...");

        long start = System.currentTimeMillis();

        Logger.debug("Loading config...");
        if (!ModularBotConfig.load()) {
            shutdownGracefully();
        }

        Logger.info("Initializing console commands...");
        ConsoleCommandManager.init();

        Logger.debug("Registering shutdown hook...");
        registerShutdownHook();

        Logger.debug("Registering Exception reporter...");
        registerExceptionReporter();

        try {
            ModuleManagerImpl.getInstance().loadModules();
        } catch (Exception exception) {
            Logger.get().fatal("Exception occurred while loading modules! Cannot proceed.", exception);
            shutdownGracefully();
        }

        if (ModularBotConfig.getInstance().getData().isEnabled()) {
            Logger.info("Loading data manager...");
            dataManager = new DataManager(ModularBotConfig.getInstance().getData().getStorageHandler());
            Logger.info("Preparing storage...");
            dataManager.prepareStorage();

            Logger.info("Loading global data holder...");
            dataManager.getGlobalDataHolder();
        }

        try {
            ModuleManagerImpl.getInstance().enableModules();
        } catch (Exception exception) {
            Logger.get().fatal("Exception occurred while enabling modules! Cannot proceed.", exception);
            shutdownGracefully();
        }

        initJDAUtilities();
        initJDA();

        Logger.success("Modular Discord Bot has finished loading! (Took " + (System.currentTimeMillis() - start) + "ms)");
    }

    /////////////////////
    // Private methods //
    /////////////////////

    /**
     * Initializes JDA Chewtils
     */
    private static void initJDAUtilities() {
        Logger.info("Initializing JDA Utilities...");

        commandClientBuilder = new CommandClientBuilder()
                .setOwnerId(ModularBotConfig.getInstance().getBot().getOwnerId())
                .setActivity(null);

        ModuleManagerImpl.getInstance().processCommandClientBuilder(commandClientBuilder);
    }

    /**
     * Initializes JDA Shard Manager
     */
    private static void initJDA() {
        Logger.info("Initializing JDA Shard Manager with " + ModularBotConfig.getInstance().getBot().getTotalShards() + " shards...");

        var shardManagerBuilder = DefaultShardManagerBuilder.createLight(ModularBotConfig.getInstance().getBot().getToken())
                                                            .setShardsTotal(ModularBotConfig.getInstance().getBot().getTotalShards())
                                                            .addEventListeners(commandClientBuilder.build())
                                                            .addEventListeners(new InteractiveListener())
                                                            .addEventListeners(new GlobalListener());

        ModuleManagerImpl.getInstance().processShardBuilder(shardManagerBuilder);

        try {
            wrappedShardManager = new WrappedShardManager(shardManagerBuilder.build());
        } catch (Exception exception) {
            Logger.get().fatal("Exception occurred while build Shard Manager! Cannot proceed.", exception);
            shutdownGracefully();
        }

        if (ModularBotConfig.getInstance().getBot().isWaitOnAllShards()) {
            wrappedShardManager.waitOnAllShards();
        }
    }

    /**
     * Registers JVM Shutdown hook
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!stopping) {
                Logger.warn("Modular Discord Bot has not stopped gracefully! Please, use command 'stop' to stop the application. There is a chance that the modules won't be unloaded fully before JVM termination.");

                Logger.info("Shutting down Modular Discord Bot...");
                doAllShutdownProcedures();
            }
        }));
    }

    /**
     * Registers exception reporter
     */
    private static void registerExceptionReporter() {
        ExceptionReporter.registerExceptionReporter();
        ExceptionReporter.getInstance().addListener(new ExceptionListener("default", "", exceptionReport -> {
            // We'll be catching all exceptions - since the empty packageName argument

            Throwable throwable = exceptionReport.getThrowable();

            Logger.get().warn("Uncaught exception occurred! Sending to modules...", throwable);
            ModuleManagerImpl.getInstance().processException(throwable);
        }));
    }

    ////////////////////
    // Public methods //
    ////////////////////

    /**
     * Shutdowns Modular Discord Bot gracefully. Run this if you want to stop this application!
     */
    public static void shutdownGracefully() {
        restartBot(true);
    }

    public static void restartBot(boolean shutdown) {
        ModularBot.setStopping(true);

        Logger.info("Shutting down Modular Discord Bot gracefully...");

        doAllShutdownProcedures();

        if (shutdown) {
            System.exit(0);
        } else {
            Logger.warn("Restarting can cause issues. Use at your own risk!");
            new Thread(() -> {
                ModularBot.main(null);
            }).start();
        }
    }

    private static void doAllShutdownProcedures() {
        ModuleManagerImpl.getInstance().unloadModules();

        if (wrappedShardManager != null) {
            wrappedShardManager.getInstance().shutdown();
            wrappedShardManager.getShardRestartTimer().cancel();
        }
    }

    /**
     * Registers console command
     *
     * @param abstractConsoleCommands {@link AbstractConsoleCommand}s
     */
    public static void registerConsoleCommands(AbstractConsoleCommand... abstractConsoleCommands) {
        ConsoleCommandManager.registerCommands(abstractConsoleCommands);
    }

    /////////////
    // Getters //
    /////////////

    public static ModuleManagerImpl getModuleManager() {
        return ModuleManagerImpl.getInstance();
    }

    ///////////////////
    // Other Classes //
    ///////////////////

    public static class Values {

        public static final String APP_VERSION = "b1.5.2";
        private static @Getter @Setter String pathFolderModules = "./modules/";
        private static @Getter @Setter String pathFolderModuleConfigs = "./modules/%s/";
        private static @Getter @Setter String pathFolderJsonData = "./json_data/";
        private static @Getter @Setter String fileNameConfig = "./modular_bot.json";
        private static @Getter @Setter String fileNameModuleInfo = "module_info.json";
        private static @Getter @Setter String fileNameModuleConfig = "config.json";
    }
}
