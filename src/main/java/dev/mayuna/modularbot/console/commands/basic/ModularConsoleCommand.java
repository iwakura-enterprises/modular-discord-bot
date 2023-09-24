package dev.mayuna.modularbot.console.commands.basic;

import dev.mayuna.mayuslibrary.arguments.ArgumentParser;
import dev.mayuna.modularbot.ModularBot;
import dev.mayuna.modularbot.base.ModuleManager;
import dev.mayuna.modularbot.console.commands.generic.AbstractConsoleCommand;
import dev.mayuna.modularbot.console.commands.generic.CommandResult;
import dev.mayuna.modularbot.logging.Logger;
import dev.mayuna.modularbot.managers.ModuleManagerImpl;
import dev.mayuna.modularbot.managers.WrappedShardManager;
import dev.mayuna.modularbot.objects.Module;
import dev.mayuna.modularbot.objects.ModuleInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.util.List;

public class ModularConsoleCommand extends AbstractConsoleCommand {

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
                ModuleManager moduleManager = ModularBot.getModuleManager();

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
    }
}
