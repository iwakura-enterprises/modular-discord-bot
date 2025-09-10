package dev.mayuna.modularbot.console;

import dev.mayuna.modularbot.ModularBot;
import dev.mayuna.modularbot.base.Module;
import dev.mayuna.modularbot.base.ModuleManager;
import dev.mayuna.modularbot.objects.ModuleInfo;
import enterprises.iwakura.ganyu.GanyuCommand;
import enterprises.iwakura.ganyu.annotation.*;
import enterprises.iwakura.sigewine.core.annotations.RomaritimeBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.logging.log4j.Level;

import java.util.List;

@RomaritimeBean
@RequiredArgsConstructor
@Log4j2
@Command("modular")
@Description("Allows you to manage ModularDiscordBot's modules and see other various information")
@Syntax("")
public final class ModularConsoleCommand implements GanyuCommand {

    @SubCommand("modules")
    @Description("Shows all modules")
    public void showAllModules() {
        var modularBot = ModularBot.getSigewine().syringe(ModularBot.class);
        ModuleManager moduleManager = modularBot.getModuleManager();
        List<Module> modules = moduleManager.getModules();

        log.info("== Modules - {} ==", modules.size());
        modules.forEach(module -> {
            ModuleInfo moduleInfo = module.getModuleInfo();

            log.info("- {} @ {} (by {}) [{}]", moduleInfo.getName(), moduleInfo.getVersion(), moduleInfo.getAuthor(), module.getModuleStatus());
        });

        log.info("Listing modules done.");
    }

    @SubCommand("shards")
    @Description("Shows information about shards")
    @Syntax("[verbose]")
    public void showShards(
            @OptionalArg Boolean verbose
    ) {
        var modularBot = ModularBot.getSigewine().syringe(ModularBot.class);
        ShardManager shardManager = modularBot.getModularBotShardManager().get();

        log.info("== Shard Info ==");
        log.info("Total shards: {}", shardManager.getShardsTotal());

        if (!verbose) {
            return;
        }

        log.info("Running shards: {}", shardManager.getShardsRunning());

        log.info("");
        log.info("! [ID] -> Status (x guilds, y users)");
        log.info("");
        shardManager.getShardCache().forEach(shard -> {
            Level logLevel;

            if (shard.getStatus() == JDA.Status.CONNECTED) {
                logLevel = Level.INFO;
            } else {
                logLevel = Level.WARN;
            }

            log.log(logLevel, "[{}] -> {} ({} guilds, {} users)",
                    shard.getShardInfo().getShardId(),
                    shard.getStatus(),
                    shard.getGuildCache().size(),
                    shard.getUserCache().size()
            );
        });
    }
}
