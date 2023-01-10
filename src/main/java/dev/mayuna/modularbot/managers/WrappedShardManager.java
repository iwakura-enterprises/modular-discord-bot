package dev.mayuna.modularbot.managers;

import com.google.common.eventbus.Subscribe;
import dev.mayuna.modularbot.ModularBot;
import dev.mayuna.modularbot.events.AllShardsStartedEvent;
import dev.mayuna.modularbot.events.ShardRebootedEvent;
import dev.mayuna.modularbot.events.ShardStartedEvent;
import dev.mayuna.modularbot.logging.Logger;
import dev.mayuna.modularbot.objects.ModuleStatus;
import dev.mayuna.modularbot.objects.activity.ModularActivity;
import dev.mayuna.modularbot.utils.GeneralUtil;
import dev.mayuna.modularbot.utils.GlobalRateLimiter;
import dev.mayuna.modularbot.utils.ModularBotConfig;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.GatewayPingEvent;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class WrappedShardManager {

    private final @Getter ShardManager instance;
    private final @Getter GlobalRateLimiter globalRateLimiter;
    private final @Getter Timer shardRestartTimer = new Timer("SHARD-REBOOTER");
    private final @Getter Timer presenceActivityUpdaterTimer = new Timer("PRESENCE-UPDATER");
    private int lastActivityIndex = 0;

    public WrappedShardManager(ShardManager instance) {
        this.instance = instance;
        ModularBot.getGlobalEventBus().register(new ShardListener());

        if (ModularBotConfig.getInstance().getBot().getGlobalRateLimiter().isEnabled()) {
            Logger.info("Enabling custom Global Rate Limiter...");
            globalRateLimiter = new GlobalRateLimiter();
        } else {
            globalRateLimiter = null;
        }

        if (ModularBotConfig.getInstance().getBot().getPresenceActivity().isEnabled()) {
            Logger.info("Enabling Modular Presence Activity...");
            initPresenceActivityUpdater();
        }
    }

    /**
     * Initializes timer which restarts disconnected shards if the config value of "restartShardEveryIfNecessary" is larger than zero
     */
    private void initShardRestartSchedule() {
        long restartShardEveryIfNecessary = ModularBotConfig.getInstance().getBot().getRestartShardEveryIfNecessary();

        if (restartShardEveryIfNecessary <= 0) {
            return;
        }

        shardRestartTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Logger.flow("Checking for offline shards...");
                restartShardsIfNotConnected();
                Logger.flow("Checking for offline shards done.");
            }
        }, restartShardEveryIfNecessary, restartShardEveryIfNecessary);
    }

    /**
     * > This function initializes the presence activity updater
     */
    private void initPresenceActivityUpdater() {
        long interval = ModularBotConfig.getInstance().getBot().getPresenceActivity().getCycleInterval();

        if (interval < 1000) {
            Logger.error("Presence Activity cycle interval must be higher than or equal to 1000ms!");
            return;
        }

        presenceActivityUpdaterTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                WrappedShardManager shardManager = ModularBot.getWrappedShardManager();

                if (shardManager == null) {
                    return;
                }

                List<ModularActivity> allActivities = new LinkedList<>();

                ModularBot.getModuleManager().getModules().forEach(module -> {
                    if (module.getModuleStatus() == ModuleStatus.ENABLED) {
                        List<ModularActivity> activities = module.getModuleActivities().getActivities();

                        synchronized (activities) {
                            allActivities.addAll(activities);
                        }
                    }
                });

                if (allActivities.size() <= lastActivityIndex + 1) {
                    lastActivityIndex = 0;
                } else {
                    lastActivityIndex++;
                }

                ModularActivity modularActivity = allActivities.get(lastActivityIndex);

                shardManager.executeForEachShardWithStatus(JDA.Status.CONNECTED, jda -> {
                    try {
                        Activity activity = modularActivity.getOnActivityRefresh().apply(jda);
                        jda.getPresence().setActivity(activity);
                    } catch (Exception exception) {
                        Logger.get()
                              .error("While refreshing activities, modular activity " + modularActivity.getName() +
                                             " from Module " + modularActivity.getModule()
                                                                              .getModuleInfo()
                                                                              .name() +
                                             " has resulted in exception on shard ID " + jda.getShardInfo()
                                                                                            .getShardId() + "!", exception);
                    }
                });
            }
        }, 0, interval);
    }

    /**
     * Waits on all shards before all of them are connected to Discord
     */
    public void waitOnAllShards() {
        Logger.info("Waiting till all shards are connected to Discord...");

        int counter = 0;
        long startTime = System.currentTimeMillis();

        do {
            if (ModularBotConfig.getInstance().getBot().isShowEtaWhenWaitingOnShards()) {
                long currentTime = System.currentTimeMillis();

                if (counter == 10) {
                    int connectedShards = getShardsWithStatus(JDA.Status.CONNECTED).size();
                    int notConnectedShards = getShardsWithoutStatus(JDA.Status.CONNECTED).size();
                    double timePerShard = -1;

                    if (connectedShards != 0) {
                        timePerShard = (currentTime - startTime) / (double) connectedShards;
                    }

                    double timeRemaining = notConnectedShards * timePerShard;

                    Logger.info("Waiting on " + getShardsWithoutStatus(JDA.Status.CONNECTED).size() + " shards... ETA: " + GeneralUtil.getTimerWithoutMillis((long) Math.ceil(timeRemaining)));

                    counter = -1;
                }
                counter++;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException interruptedException) {
                Logger.get().warn("Interrupted exception occurred while waiting on all shards to be connected!", interruptedException);
            }
        } while (!areAllShardsConnected());
    }

    /**
     * Determines if the specified shard has {@link JDA.Status#CONNECTED} status
     *
     * @param shardId Shard ID
     *
     * @return True if yes, false otherwise
     */
    public boolean isShardConnected(int shardId) {
        JDA shard = instance.getShardById(shardId);

        if (shard == null) {
            return false;
        }

        return shard.getStatus() == JDA.Status.CONNECTED;
    }

    /**
     * Determines if all shards have {@link JDA.Status#CONNECTED} status
     *
     * @return True if yes, false otherwise
     */
    public boolean areAllShardsConnected() {
        for (JDA shard : instance.getShardCache()) {
            if (shard.getStatus() != JDA.Status.CONNECTED) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets all shards with specified status
     *
     * @param status {@link JDA.Status}
     *
     * @return List of shards (JDA)
     */
    public List<JDA> getShardsWithStatus(JDA.Status status) {
        return instance.getShardCache().stream().filter(jda -> jda.getStatus() == status).collect(Collectors.toList());
    }

    /**
     * Gets all shards which do not have specified {@link JDA.Status}
     *
     * @param status {@link JDA.Status}
     *
     * @return List of shards (JDA)
     */
    public List<JDA> getShardsWithoutStatus(JDA.Status status) {
        return instance.getShardCache().stream().filter(jda -> jda.getStatus() != status).collect(Collectors.toList());
    }

    /**
     * Executes specified {@link Consumer} on every shard
     *
     * @param consumer Non-null {@link Consumer}
     */
    public void executeForEachShard(@NonNull Consumer<JDA> consumer) {
        instance.getShardCache().forEach(consumer);
    }

    /**
     * Executes specified {@link Consumer} on every shard which has specified {@link JDA.Status}
     *
     * @param status   {@link JDA.Status}
     * @param consumer Non-null {@link Consumer}
     */
    public void executeForEachShardWithStatus(JDA.Status status, @NonNull Consumer<JDA> consumer) {
        instance.getShardCache().forEach(jda -> {
            if (jda.getStatus() == status) {
                consumer.accept(jda);
            }
        });
    }

    /**
     * Executes specified {@link Consumer} on every shard which has not specified {@link JDA.Status}
     *
     * @param status   {@link JDA.Status}
     * @param consumer Non-null {@link Consumer}
     */
    public void executeForEachShardWithoutStatus(JDA.Status status, @NonNull Consumer<JDA> consumer) {
        instance.getShardCache().forEach(jda -> {
            if (jda.getStatus() != status) {
                consumer.accept(jda);
            }
        });
    }

    /**
     * Restarts all shards which have status of {@link JDA.Status#DISCONNECTED}
     */
    public void restartShardsIfNotConnected() {
        for (JDA shard : instance.getShardCache()) {
            if (shard.getStatus() == JDA.Status.DISCONNECTED) {
                int shardId = shard.getShardInfo().getShardId();

                Logger.warn("Restarting disconnected Shard " + shardId + "...");
                instance.restart(shard.getShardInfo().getShardId());
                ModularBot.getGlobalEventBus().post(new ShardRebootedEvent(shard));
            }
        }
    }

    protected class ShardListener {

        private List<Integer> startedShards = new LinkedList<>();
        private boolean shouldCheckForNewShards = true;

        @Subscribe
        public void onGatewayPing(GatewayPingEvent event) {
            if (!shouldCheckForNewShards) {
                return;
            }

            JDA shard = event.getEntity();
            int shardId = shard.getShardInfo().getShardId();

            if (!startedShards.contains(shardId)) {
                startedShards.add(shardId);

                if (globalRateLimiter != null) {
                    globalRateLimiter.processJda(shard);
                }

                ModularBot.getGlobalEventBus().post(new ShardStartedEvent(shard));

                if (didAllShardsStart(ModularBotConfig.getInstance().getBot().getTotalShards())) {
                    shouldCheckForNewShards = false;
                    startedShards = null;
                    ModularBot.getGlobalEventBus().post(new AllShardsStartedEvent());
                }
            }
        }

        @Subscribe
        public void onAllShardsStarted(AllShardsStartedEvent event) {
            ModularBot.getWrappedShardManager().initShardRestartSchedule();
        }

        protected boolean didAllShardsStart(int totalShards) {
            return startedShards.size() == totalShards;
        }
    }
}
