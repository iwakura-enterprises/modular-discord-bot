package dev.mayuna.modularbot.managers;

import com.google.common.eventbus.Subscribe;
import dev.mayuna.modularbot.ModularBot;
import dev.mayuna.modularbot.events.AllShardsStartedEvent;
import dev.mayuna.modularbot.events.ShardRebootedEvent;
import dev.mayuna.modularbot.events.ShardStartedEvent;
import dev.mayuna.modularbot.logging.Logger;
import dev.mayuna.modularbot.utils.Config;
import dev.mayuna.modularbot.utils.GeneralUtil;
import dev.mayuna.modularbot.utils.GlobalRateLimiter;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.JDA;
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

    public WrappedShardManager(ShardManager instance) {
        this.instance = instance;
        ModularBot.getGlobalEventBus().register(new ShardListener());

        if (Config.getInstance().getBot().getGlobalRateLimiter().isEnabled()) {
            Logger.info("Enabling custom Global Rate Limiter...");
            globalRateLimiter = new GlobalRateLimiter();
        } else {
            globalRateLimiter = null;
        }
    }

    /**
     * Initializes timer which restarts disconnected shards if the config value of "restartShardEveryIfNecessary" is larger than zero
     */
    private void initShardRestartSchedule() {
        long restartShardEveryIfNecessary = Config.getInstance().getBot().getRestartShardEveryIfNecessary();

        if (restartShardEveryIfNecessary <= 0) {
            return;
        }

        shardRestartTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Logger.debug("Checking for offline shards...");
                restartShardsIfNotConnected();
                Logger.debug("Checking for offline shards done.");
            }
        }, restartShardEveryIfNecessary, restartShardEveryIfNecessary);
    }

    /**
     * Waits on all shards before all of them are connected to Discord
     */
    public void waitOnAllShards() {
        Logger.info("Waiting till all shards are connected to Discord...");

        int counter = 0;
        long startTime = System.currentTimeMillis();

        do {
            if (Config.getInstance().getBot().isShowEtaWhenWaitingOnShards()) {
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

                if (didAllShardsStart(Config.getInstance().getBot().getTotalShards())) {
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
