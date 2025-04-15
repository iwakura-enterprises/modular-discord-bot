package dev.mayuna.modularbot.console;

import dev.mayuna.consoleparallax.BaseCommand;
import dev.mayuna.consoleparallax.CommandInvocationContext;
import dev.mayuna.modularbot.ModularBot;
import dev.mayuna.modularbot.base.Module;
import dev.mayuna.modularbot.base.ModuleManager;
import dev.mayuna.modularbot.objects.ModuleInfo;
import dev.mayuna.modularbot.util.logging.ModularBotLogger;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RequiredArgsConstructor
public final class ModularConsoleCommand implements BaseCommand {

    private static final ModularBotLogger LOGGER = ModularBotLogger.create(ModularConsoleCommand.class);

    private final ModularBot modularBot;

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
        return "<modules(m)|shards(s) [verbose(v)]>";
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

                /*
                if (args.length != 3) {
                    LOGGER.error("Invalid modules arguments. Syntax: {}", getSyntax());
                    return;
                }

                processModuleSubCommand(args[1], args[2]);*/
            }
            case "shards", "s" -> {
                boolean verbose = args.length == 2 && (args[1].equals("verbose") || args[1].equals("v"));
                showShards(verbose);
            }
        }
    }

    /**
     * Shows all modules
     */
    private void showAllModules() {
        ModuleManager moduleManager = modularBot.getModuleManager();
        List<Module> modules = moduleManager.getModules();

        LOGGER.info("== Modules - {} ==", modules.size());
        modules.forEach(module -> {
            ModuleInfo moduleInfo = module.getModuleInfo();

            LOGGER.info("- {} @ {} (by {}) [{}]", moduleInfo.getName(), moduleInfo.getVersion(), moduleInfo.getAuthor(), module.getModuleStatus());
        });

        LOGGER.info("Listing modules done.");
    }

    /**
     * Show shards
     *
     * @param verbose Determines if the list of shards should be printed as well
     */
    private void showShards(boolean verbose) {
        ShardManager shardManager = modularBot.getModularBotShardManager().get();

        LOGGER.info("== Shard Info ==");
        LOGGER.info("Total shards: {}", shardManager.getShardsTotal());

        if (!verbose) {
            return;
        }

        LOGGER.info("Running shards: {}", shardManager.getShardsRunning());

        LOGGER.info("");
        LOGGER.info("! [ID] -> Status (x guilds, y users)");
        LOGGER.info("");
        shardManager.getShardCache().forEach(shard -> {
            Level logLevel;

            if (shard.getStatus() == JDA.Status.CONNECTED) {
                logLevel = ModularBotLogger.SUCCESS;
            } else {
                logLevel = Level.WARN;
            }

            LOGGER.log(logLevel, "[{}] -> {} ({} guilds, {} users)",
                       shard.getShardInfo().getShardId(),
                       shard.getStatus(),
                       shard.getGuildCache().size(),
                       shard.getUserCache().size()
            );
        });
    }
}
