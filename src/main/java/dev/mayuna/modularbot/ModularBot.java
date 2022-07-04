package dev.mayuna.modularbot;

import com.google.common.eventbus.EventBus;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import dev.mayuna.mayusjdautils.data.MayuCoreListener;
import dev.mayuna.mayuslibrary.exceptionreporting.ExceptionListener;
import dev.mayuna.mayuslibrary.exceptionreporting.ExceptionReporter;
import dev.mayuna.modularbot.console.ConsoleCommandManager;
import dev.mayuna.modularbot.console.commands.generic.AbstractConsoleCommand;
import dev.mayuna.modularbot.listeners.GlobalListener;
import dev.mayuna.modularbot.logging.Logger;
import dev.mayuna.modularbot.managers.DataManager;
import dev.mayuna.modularbot.managers.ModuleManager;
import dev.mayuna.modularbot.managers.WrappedShardManager;
import dev.mayuna.modularbot.utils.Config;
import dev.mayuna.modularbot.utils.SQLUtil;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;

public class ModularBot {

    private static final @Getter DataManager dataManager = new DataManager();
    private static final @Getter EventBus globalEventBus = new EventBus("modular_bot-global");
    private static @Getter @Setter boolean stopping;
    private static @Getter @Setter boolean dontSaveData = false;
    private static @Getter WrappedShardManager wrappedShardManager;
    private static CommandClientBuilder commandClientBuilder;

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
                              __  __         _      _            ___  _                   _   ___      _  \s
                             |  \\/  |___  __| |_  _| |__ _ _ _  |   \\(_)___ __ ___ _ _ __| | | _ ) ___| |_\s
                             | |\\/| / _ \\/ _` | || | / _` | '_| | |) | (_-</ _/ _ \\ '_/ _` | | _ \\/ _ \\  _|
                             |_|  |_\\___/\\__,_|\\_,_|_\\__,_|_|   |___/|_/__/\\__\\___/_| \\__,_| |___/\\___/\\__|
                            """);

        Logger.info("@ Version: " + Values.APP_VERSION);
        Logger.info("Made by Mayuna\n");
        Logger.info("Loading...");

        long start = System.currentTimeMillis();

        Logger.debug("Loading config...");
        if (!Config.load()) {
            shutdownGracefully();
        }

        Logger.info("Initializing console commands...");
        ConsoleCommandManager.init();

        Logger.debug("Registering shutdown hook...");
        registerShutdownHook();

        Logger.debug("Registering Exception reporter...");
        registerExceptionReporter();

        try {
            ModuleManager.getInstance().loadModules();
        } catch (Exception exception) {
            Logger.get().fatal("Exception occurred while loading modules! Cannot proceed.", exception);
            shutdownGracefully();
        }

        dataManager.initDatabase();

        if (!Config.getInstance().getData().isLazyLoading()) {
            Logger.debug("Loading all data...");
            dataManager.loadAll();
        }

        try {
            ModuleManager.getInstance().enableModules();
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
                .setOwnerId(Config.getInstance().getBot().getOwnerId());

        ModuleManager.getInstance().processCommandClientBuilder(commandClientBuilder);
    }

    /**
     * Initializes JDA Shard Manager
     */
    private static void initJDA() {
        Logger.info("Initializing JDA Shard Manager with " + Config.getInstance().getBot().getTotalShards() + " shards...");

        var shardManagerBuilder = DefaultShardManagerBuilder.createLight(Config.getInstance().getBot().getToken())
                                                            .setShardsTotal(Config.getInstance().getBot().getTotalShards())
                                                            .addEventListeners(commandClientBuilder.build())
                                                            .addEventListeners(new MayuCoreListener())
                                                            .addEventListeners(new GlobalListener());

        ModuleManager.getInstance().processShardBuilder(shardManagerBuilder);

        try {
            wrappedShardManager = new WrappedShardManager(shardManagerBuilder.build());
        } catch (Exception exception) {
            Logger.get().fatal("Exception occurred while build Shard Manager! Cannot proceed.", exception);
            shutdownGracefully();
        }

        if (Config.getInstance().getBot().isWaitOnAllShards()) {
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
            ModuleManager.getInstance().processException(throwable);
        }));
    }

    ////////////////////
    // Public methods //
    ////////////////////

    /**
     * Shutdowns Modular Discord Bot gracefully. Run this if you want to stop this application!
     */
    public static void shutdownGracefully() {
        ModularBot.setStopping(true);

        Logger.info("Shutting down Modular Discord Bot gracefully...");

        doAllShutdownProcedures();

        System.exit(0);
    }

    private static void doAllShutdownProcedures() {
        if (!dontSaveData) {
            dataManager.saveAll();
        }

        ModuleManager.getInstance().unloadModules();

        if (wrappedShardManager != null) {
            wrappedShardManager.getInstance().shutdown();
        }

        SQLUtil.closePool();
    }

    /**
     * Registers console command
     *
     * @param abstractConsoleCommands {@link AbstractConsoleCommand}s
     */
    public static void registerConsoleCommands(AbstractConsoleCommand... abstractConsoleCommands) {
        ConsoleCommandManager.registerCommands(abstractConsoleCommands);
    }

    public static class Values {

        public static final String APP_VERSION = "b1.0";
        private static @Getter @Setter String pathFolderModules = "./modules/";
        private static @Getter @Setter String pathFolderJsonData = "./json_data/";
        private static @Getter @Setter String fileNameConfig = "./modular_bot.json";
        private static @Getter @Setter String fileNameModuleInfo = "module_info.json";
    }
}
