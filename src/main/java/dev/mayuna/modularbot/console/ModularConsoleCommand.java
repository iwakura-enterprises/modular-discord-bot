package dev.mayuna.modularbot.console;

import dev.mayuna.consoleparallax.BaseCommand;
import dev.mayuna.consoleparallax.CommandInvocationContext;
import dev.mayuna.modularbot.ModularBot;
import dev.mayuna.modularbot.base.Module;
import dev.mayuna.modularbot.base.ModuleManager;
import dev.mayuna.modularbot.util.logging.ModularBotLogger;
import dev.mayuna.modularbot.objects.ModuleInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ModularConsoleCommand implements BaseCommand {

    private static final ModularBotLogger LOGGER = ModularBotLogger.create(ModularConsoleCommand.class);

    /*
    public ModularConsoleCommand() {
        this.name = "modular";
        this.syntax = "<modules(m)[unload(u) <module>|load(l) <module>|reload(r)]|shards(s)[verbose(v)]|restart(r)>";
    }

    @Override
    public CommandResult execute(String arguments) {
        ArgumentParser argumentParser = new ArgumentParser(arguments);

        if (!argumentParser.hasAnyArguments()) {
            return CommandResult.INCORRECT_SYNTAX;
        }

        switch (argumentParser.getArgumentAtIndex(0).getValue()) {
            case "modules", "m" -> {
                ModuleManagerImpl moduleManager = ModuleManagerImpl.getInstance();

                if (argumentParser.hasArgumentAtIndex(1)) {
                    switch (argumentParser.getArgumentAtIndex(1).getValue()) {
                        case "unload", "u" -> {
                            if (!argumentParser.hasArgumentAtIndex(2)) {
                                return CommandResult.INCORRECT_SYNTAX;
                            }

                            return processModuleDisable(argumentParser);
                        }
                        case "load", "l" -> {
                            if (!argumentParser.hasArgumentAtIndex(2)) {
                                return CommandResult.INCORRECT_SYNTAX;
                            }

                            return processModuleEnable(argumentParser);
                        }
                        case "reload", "r" -> {
                            return processModuleReload(argumentParser);
                        }
                    }
                }

                Logger.info("== Enabled modules - " + moduleManager.getModules().size() + " ==");
                moduleManager.getModules().forEach(module -> {
                    ModuleInfo moduleInfo = module.getModuleInfo();

                    Logger.info("- " + moduleInfo.name() + " @ " + moduleInfo.version() + " (by " + moduleInfo.author() + ")");
                });
                Logger.info("Listing modules done.");
            }
            case "shards", "s" -> {
                WrappedShardManager wrappedShardManager = ModularBot.getWrappedShardManager();
                ShardManager shardManager = wrappedShardManager.getInstance();

                Logger.info("=== Shards info ===");
                Logger.info("Total shards: " + shardManager.getShardsTotal());

                if (argumentParser.hasArgumentAtIndex(1)) {
                    switch (argumentParser.getArgumentAtIndex(1).getValue()) {
                        case "verbose", "v" -> {
                            Logger.info("- [ID] -> Status (x guilds)");
                            Logger.info("");
                            wrappedShardManager.getInstance().getShardCache().forEach(jda -> {
                                if (jda.getStatus() != JDA.Status.CONNECTED) {
                                    Logger.warn("- [" + jda.getShardInfo().getShardId() + "] -> " + jda.getStatus() + " (" + jda.getGuildCache()
                                                                                                                                .size() + " guilds)");
                                } else {
                                    Logger.info("- [" + jda.getShardInfo().getShardId() + "] -> " + jda.getStatus() + " (" + jda.getGuildCache()
                                                                                                                                .size() + " guilds)");
                                }
                            });

                            return CommandResult.SUCCESS;
                        }
                    }
                }

                List<JDA> nonConnectedShards = wrappedShardManager.getShardsWithoutStatus(JDA.Status.CONNECTED);

                if (!nonConnectedShards.isEmpty()) {
                    Logger.warn("Some shards are not connected!");
                    nonConnectedShards.forEach(jda -> {
                        Logger.warn("Shard " + jda.getShardInfo().getShardId() + " has status " + jda.getStatus());
                    });
                } else {
                    Logger.success("All shards are connected.");
                }
            }
            case "restart", "r" -> {
                ModularBot.restartBot(false);
            }
            default -> {
                return CommandResult.INCORRECT_SYNTAX;
            }
        }

        return CommandResult.SUCCESS;
    }

    private CommandResult processModuleDisable(ArgumentParser argumentParser) {
        String moduleName = argumentParser.getAllArgumentsAfterIndex(2).getValue();
        Module module = ModularBot.getModuleManager().getModuleByName(moduleName).orElse(null);

        if (module == null) {
            Logger.error("Module " + moduleName + " is not loaded or could not be found.");
            return CommandResult.SUCCESS;
        }

        ModularBot.getModuleManager().unloadModule(module);
        return CommandResult.SUCCESS;
    }

    private CommandResult processModuleEnable(ArgumentParser argumentParser) {
        String moduleName = argumentParser.getAllArgumentsAfterIndex(2).getValue();
        Module module = ModularBot.getModuleManager().getModuleByName(moduleName).orElse(null);

        if (module == null) {
            Logger.error("Module " + moduleName + " is not loaded or could not be found.");
            Logger.warn("Loading specific modules from jars is not currently supported. Use must use reload subcommand.");
            return CommandResult.SUCCESS;
        }

        ModularBot.getModuleManager().loadModule(module);
        ModularBot.getModuleManager().enableModule(module);
        return CommandResult.SUCCESS;
    }

    private CommandResult processModuleReload(ArgumentParser argumentParser) {
        if (!argumentParser.hasArgumentAtIndex(2)) {

            Logger.info("Unloading all modules...");
            ModularBot.getModuleManager().unloadModules();

            try {
                ModularBot.getModuleManager().loadModules();
            } catch (Exception exception) {
                Logger.throwing(exception);
                Logger.error("There was an exception while loading modules!");
            }

            return CommandResult.SUCCESS;
        }

        processModuleDisable(argumentParser);
        processModuleEnable(argumentParser);

        Logger.warn("If you want to (re)load commands or events, you must restart the bot.");
        return CommandResult.SUCCESS;
    }*/

    @Override
    public @NotNull String getName() {
        return "modular";
    }

    @Override
    public @NotNull String getUsage() {
        return "Manages ModularDiscordBot";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<modules(m)[unload(u) <module>|load(l) <module>|reload(r)]|shards(s)[verbose(v)]|restart(r)>";
    }

    @Override
    public @NotNull String getDescription() {
        return "Allows you to manage ModularDiscordBot's modules and see other various information.";
    }

    @Override
    public void execute(@NotNull CommandInvocationContext commandInvocationContext) {
        final String[] args = commandInvocationContext.getArguments();

        if (args.length == 0) {
            LOGGER.error("No arguments specified. Syntax: {}", getSyntax());
            return;
        }

        switch (args[0]) {
            case "modules", "m" -> {
                if (args.length == 1) {
                    showAllModules();
                    return;
                }

                if (args.length != 3) {
                    LOGGER.error("Invalid modules arguments. Syntax: {}", getSyntax());
                    return;
                }

                processModuleSubCommand(args[1], args[2]);
            }
            case "shards", "s" -> {
                // TODO
            }
        }
    }

    private void processModuleSubCommand(String action, String moduleName) {
        // TODO
    }

    /**
     * Shows all modules
     */
    private void showAllModules() {
        ModuleManager moduleManager = ModularBot.getModuleManager();
        List<Module> modules = moduleManager.getModules();

        LOGGER.info("== Modules - {} ==", modules.size());
        modules.forEach(module -> {
            ModuleInfo moduleInfo;

            // TODO:
        });

        LOGGER.info("Listing modules done.");
    }
}
