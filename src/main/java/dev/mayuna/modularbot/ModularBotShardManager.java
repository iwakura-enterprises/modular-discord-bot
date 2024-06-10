package dev.mayuna.modularbot;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import dev.mayuna.mayusjdautils.interactive.InteractiveListener;
import dev.mayuna.modularbot.config.ModularBotConfig;
import dev.mayuna.modularbot.objects.ModuleStatus;
import dev.mayuna.modularbot.objects.activity.ModuleActivity;
import dev.mayuna.modularbot.util.logging.ModularBotLogger;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public final class ModularBotShardManager {

    public static final int INVALID_SHARD_ID = -1;
    private final static ModularBotLogger LOGGER = ModularBotLogger.create(ModularBotShardManager.class);

    private final ModularBotConfig.Discord discordSettings;
    private final Timer presenceActivityUpdaterTimer = new Timer();
    private @Getter CommandClientBuilder commandClientBuilder;
    private @Getter DefaultShardManagerBuilder shardManagerBuilder;
    private ShardManager shardManager;

    private boolean connected = false;
    private int lastActivityIndex = 0;

    /**
     * Creates new instance of {@link ModularBotShardManager}
     *
     * @param discordSettings {@link ModularBotConfig.Discord}
     */
    public ModularBotShardManager(@NonNull ModularBotConfig.Discord discordSettings) {
        this.discordSettings = discordSettings;
    }

    /**
     * Initializes ShardManager
     */
    public boolean init() {
        LOGGER.mdebug("Creating CommandClientBuilder...");
        commandClientBuilder = new CommandClientBuilder()
                .setOwnerId(discordSettings.getOwnerId())
                .setActivity(null);

        try {
            final ModularBotConfig.Discord.ShardManager shardManagerSettings = discordSettings.getShardManager();
            if (discordSettings.getShardManager().isLight()) {
                LOGGER.mdebug("Creating light DefaultShardManagerBuilder...");
                shardManagerBuilder = DefaultShardManagerBuilder.createLight(discordSettings.getToken(), shardManagerSettings.getGatewayIntents());
            } else {
                LOGGER.mdebug("Creating default DefaultShardManagerBuilder...");
                shardManagerBuilder = DefaultShardManagerBuilder.createDefault(discordSettings.getToken(), shardManagerSettings.getGatewayIntents());
            }

            LOGGER.mdebug("Gateway intents: {}", discordSettings.getShardManager().getGatewayIntents().toString());

            if (!shardManagerSettings.getShardIds().isEmpty()) {
                LOGGER.mdebug("Using these shard IDs: {}", shardManagerSettings.getShardIds().toString());
                shardManagerBuilder.setShards(shardManagerSettings.getShardIds());
                return true;
            }

            if (shardManagerSettings.getMinShardId() != INVALID_SHARD_ID && shardManagerSettings.getMaxShardId() != INVALID_SHARD_ID) {
                LOGGER.mdebug("Using shard IDs from {} to {}", shardManagerSettings.getMinShardId(), shardManagerSettings.getMaxShardId());
                shardManagerBuilder.setShards(shardManagerSettings.getMinShardId(), shardManagerSettings.getMaxShardId());
                return true;
            }

            int totalShards = shardManagerSettings.getTotalShards();

            if (totalShards == INVALID_SHARD_ID) {
                LOGGER.mdebug("Using recommended amount of shards by Discord");
                shardManagerBuilder.setShardsTotal(INVALID_SHARD_ID);
                return true;
            }

            LOGGER.mdebug("Using total of {} shard(s)", shardManagerSettings.getTotalShards());
            shardManagerBuilder.setShardsTotal(shardManagerSettings.getTotalShards());

            return true;
        } catch (Exception exception) {
            LOGGER.error("Failed to create Shard Manager!", exception);
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
            LOGGER.warn("Cannot build twice.");
            return false;
        }

        LOGGER.mdebug("Building CommandClientBuilder...");
        shardManagerBuilder.addEventListeners(commandClientBuilder.build());

        LOGGER.mdebug("Registering InteractiveListener...");
        shardManagerBuilder.addEventListeners(new InteractiveListener());

        LOGGER.mdebug("Building ShardManager...");

        try {
            this.shardManager = shardManagerBuilder.build(false);
            this.shardManagerBuilder = null;
            return true;
        } catch (Exception exception) {
            LOGGER.error("Could not build ShardManager!", exception);
            return false;
        }
    }

    /**
     * Connects to Discord<br>
     * Should not be called more than once.
     */
    public boolean connect() {
        if (connected) {
            LOGGER.warn("Cannot connect twice.");
            return false;
        }

        connected = true;

        try {
            this.shardManager.login();
            LOGGER.success("Connected to Discord!");
            return true;
        } catch (Exception exception) {
            LOGGER.error("Could not connect to Discord!", exception);
            return false;
        }
    }

    /**
     * Initializes Presence Activity Cycle
     */
    public void initPresenceActivityCycle() {
        ModularBotConfig.Discord.PresenceActivityCycle presenceActivityCycle = discordSettings.getPresenceActivityCycle();

        if (!presenceActivityCycle.isEnabled()) {
            LOGGER.warn("Presence Activity Cycle is disabled, skipping.");
            return;
        }

        long intervalMillis = presenceActivityCycle.getCycleIntervalMillis();

        if (intervalMillis < 10000) {
            LOGGER.error("Presence Activity Cycle interval must be higher than or equal to 10000ms!");
            return;
        }

        presenceActivityUpdaterTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                List<ModuleActivity> allActivities = new LinkedList<>();

                ModularBot.getModuleManager().getModules().forEach(module -> {
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
                        LOGGER.error("Failed to set activity from module {} with activity name of {} on shard ID {}",
                                     moduleActivity.getModule().getModuleInfo().getName(),
                                     moduleActivity.getName(),
                                     jda.getShardInfo().getShardId()
                        );
                    }
                });
            }
        }, 0, intervalMillis);
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
