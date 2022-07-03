package dev.mayuna.modularbot.console.commands.basic;

import dev.mayuna.mayuslibrary.arguments.ArgumentParser;
import dev.mayuna.modularbot.ModularBot;
import dev.mayuna.modularbot.console.commands.generic.AbstractConsoleCommand;
import dev.mayuna.modularbot.console.commands.generic.CommandResult;
import dev.mayuna.modularbot.logging.Logger;
import dev.mayuna.modularbot.managers.ModuleManager;
import dev.mayuna.modularbot.managers.WrappedShardManager;
import dev.mayuna.modularbot.objects.ModuleInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.util.List;

public class ModularConsoleCommand extends AbstractConsoleCommand {

    public ModularConsoleCommand() {
        this.name = "modular";
        this.syntax = "<modules(m)[disable(d) <module>|enable(e) <module>]|shards(s)[verbose(v)]>";
    }

    @Override
    public CommandResult execute(String arguments) {
        ArgumentParser argumentParser = new ArgumentParser(arguments);

        if (!argumentParser.hasAnyArguments()) {
            return CommandResult.INCORRECT_SYNTAX;
        }

        switch (argumentParser.getArgumentAtIndex(0).getValue()) {
            case "modules", "m" -> {
                ModuleManager moduleManager = ModuleManager.getInstance();

                if (argumentParser.hasArgumentAtIndex(1)) {
                    switch (argumentParser.getArgumentAtIndex(1).getValue()) {
                        case "disable", "d" -> {
                            if (!argumentParser.hasArgumentAtIndex(2)) {
                                return CommandResult.INCORRECT_SYNTAX;
                            }

                            throw new RuntimeException("This feature is not implemented.");
                        }
                        case "enable", "e" -> {
                            if (!argumentParser.hasArgumentAtIndex(2)) {
                                return CommandResult.INCORRECT_SYNTAX;
                            }

                            throw new RuntimeException("This feature is not implemented.");
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
                                    Logger.warn("- [" + jda.getShardInfo().getShardId() + "] -> " + jda.getStatus() + " (" + jda.getGuildCache().size() + " guilds)");
                                } else {
                                    Logger.info("- [" + jda.getShardInfo().getShardId() + "] -> " + jda.getStatus() + " (" + jda.getGuildCache().size() + " guilds)");
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
            default -> {
                return CommandResult.INCORRECT_SYNTAX;
            }
        }

        return CommandResult.SUCCESS;
    }
}
