package enterprises.iwakura.modularbot;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import enterprises.iwakura.jdainteractables.InteractableListener;
import enterprises.iwakura.modularbot.config.ModularBotConfig;
import enterprises.iwakura.modularbot.managers.ModuleManager;
import enterprises.iwakura.modularbot.objects.ModuleStatus;
import enterprises.iwakura.modularbot.objects.activity.ModuleActivity;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
@Bean
@RequiredArgsConstructor
public final class ModularBotShardManager {

    public static final int INVALID_SHARD_ID = -1;

    private final ModularBotConfig modularBotConfig;
    private final ModuleManager moduleManager;

    private final Timer presenceActivityUpdaterTimer = new Timer();
    private @Getter CommandClientBuilder commandClientBuilder;
    private @Getter DefaultShardManagerBuilder shardManagerBuilder;
    private ShardManager shardManager;

    private boolean connected = false;
    private int lastActivityIndex = 0;

    /**
     * Initializes ShardManager
     */
    public boolean init() {
        var discordSettings = modularBotConfig.getDiscord();

        log.info("Creating CommandClientBuilder...");
        commandClientBuilder = new CommandClientBuilder()
                .setOwnerId(discordSettings.getOwnerId())
                .setActivity(null);

        try {
            ModularBotConfig.Discord.ShardManager shardManagerSettings = discordSettings.getShardManager();
            if (discordSettings.getShardManager().isLight()) {
                log.info("Creating light DefaultShardManagerBuilder...");
                shardManagerBuilder = DefaultShardManagerBuilder.createLight(discordSettings.getToken(), shardManagerSettings.getGatewayIntents());
            } else {
                log.info("Creating default DefaultShardManagerBuilder...");
                shardManagerBuilder = DefaultShardManagerBuilder.createDefault(discordSettings.getToken(), shardManagerSettings.getGatewayIntents());
            }

            log.info("Modular Bot's config Gateway intents: {}", discordSettings.getShardManager().getGatewayIntents().toString());

            if (!shardManagerSettings.getShardIds().isEmpty()) {
                log.info("Using these shard IDs: {}", shardManagerSettings.getShardIds().toString());
                shardManagerBuilder.setShards(shardManagerSettings.getShardIds());
                return true;
            }

            if (shardManagerSettings.getMinShardId() != INVALID_SHARD_ID && shardManagerSettings.getMaxShardId() != INVALID_SHARD_ID) {
                log.info("Using shard IDs from {} to {}", shardManagerSettings.getMinShardId(), shardManagerSettings.getMaxShardId());
                shardManagerBuilder.setShards(shardManagerSettings.getMinShardId(), shardManagerSettings.getMaxShardId());
                return true;
            }

            int totalShards = shardManagerSettings.getTotalShards();

            if (totalShards == INVALID_SHARD_ID) {
                log.info("Using recommended amount of shards by Discord");
                shardManagerBuilder.setShardsTotal(INVALID_SHARD_ID);
                return true;
            }

            log.info("Using total of {} shard(s)", shardManagerSettings.getTotalShards());
            shardManagerBuilder.setShardsTotal(shardManagerSettings.getTotalShards());

            return true;
        } catch (Exception exception) {
            log.error("Failed to create Shard Manager!", exception);
            return false;
        }
    }

    /**
     * Finishes and builds {@link ShardManager}
     *
     * @return True if successful, false otherwise
     */
    public boolean finish() {
        if (shardManagerBuilder == null) {
            log.warn("Cannot build shard manager twice.");
            return false;
        }

        log.info("Registering CommandClientBuilder...");
        shardManagerBuilder.addEventListeners(commandClientBuilder.build());

        log.info("Registering JDA-Interactable's InteractableListener...");
        shardManagerBuilder.addEventListeners(new InteractableListener());

        log.info("Building ShardManager...");

        try {
            this.shardManager = shardManagerBuilder.build(false);
            this.shardManagerBuilder = null;
            return true;
        } catch (Exception exception) {
            log.error("Could not build ShardManager!", exception);
            return false;
        }
    }

    /**
     * Connects to Discord<br>
     * Should not be called more than once.
     */
    public boolean connect() {
        if (connected) {
            log.warn("Cannot connect twice.");
            return false;
        }

        connected = true;

        try {
            this.shardManager.login();
            log.info("Connected to Discord!");
            return true;
        } catch (Exception exception) {
            log.error("Could not connect to Discord!", exception);
            return false;
        }
    }

    /**
     * Initializes Presence Activity Cycle
     */
    public void initPresenceActivityCycle() {
        ModularBotConfig.Discord.PresenceActivityCycle presenceActivityCycle = modularBotConfig.getDiscord().getPresenceActivityCycle();

        if (!presenceActivityCycle.isEnabled()) {
            log.warn("Presence Activity Cycle is disabled, skipping.");
            return;
        }

        long intervalMillis = presenceActivityCycle.getCycleIntervalMillis();

        if (intervalMillis < 10000) {
            log.error("Presence Activity Cycle interval must be higher than or equal to 10000ms!");
            return;
        }

        //@formatter:off
        presenceActivityUpdaterTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                List<ModuleActivity> allActivities = new LinkedList<>();

                moduleManager.getModules().forEach(module -> {
                    if (module.getModuleStatus() == ModuleStatus.ENABLED) {
                        List<ModuleActivity> activities = module.getModuleActivities().getActivities();

                        synchronized (activities) {
                            allActivities.addAll(activities);
                        }
                    }
                });

                // No activities registered
                if (allActivities.isEmpty()) {
                    return;
                }

                if (allActivities.size() <= lastActivityIndex + 1) {
                    lastActivityIndex = 0;
                } else {
                    lastActivityIndex++;
                }

                ModuleActivity moduleActivity = allActivities.get(lastActivityIndex);

                shardManager.getShardCache().forEach(jda -> {
                    try {
                        jda.getPresence().setActivity(moduleActivity.getOnActivityRefresh().apply(jda));
                    } catch (Exception exception) {
                        log.error("Failed to set activity from module {} with activity name of {} on shard ID {}",
                            moduleActivity.getModule().getModuleInfo().getName(),
                            moduleActivity.getName(),
                            jda.getShardInfo().getShardId()
                        );
                    }
                });
            }
        }, 0, intervalMillis);
        //@formatter:on
    }

    /**
     * Shutdowns {@link ShardManager}
     */
    public void shutdown() {
        presenceActivityUpdaterTimer.cancel();

        if (shardManager != null) {
            shardManager.shutdown();
        }
    }

    /**
     * Returns {@link ShardManager}
     *
     * @return {@link ShardManager}
     */
    public ShardManager get() {
        return shardManager;
    }
}
