package dev.mayuna.modularbot.config;

import com.google.gson.Gson;
import dev.mayuna.mayusjsonutils.MayuJson;
import dev.mayuna.mayusjsonutils.ObjectLoader;
import dev.mayuna.modularbot.ModularBotConstants;
import enterprises.iwakura.irminsul.DatabaseServiceConfiguration;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/**
 * Modular Bot Config
 */
@SuppressWarnings("FieldMayBeFinal")
@Getter
@Log4j2
public final class ModularBotConfig {

    private static final Gson GSON = MayuJson.DEFAULT_GSON;

    // Settings
    private Discord discord = new Discord();
    private StorageSettings storageSettings = new StorageSettings("modular-bot");
    private Irminsul irminsul = new Irminsul();

    /**
     * Loads the configuration from the config file
     *
     * @return The loaded configuration
     */
    public static ModularBotConfig load() {
        try {
            return ObjectLoader.loadOrCreateFrom(ModularBotConfig.class, ModularBotConstants.PATH_MODULAR_BOT_CONFIG, StandardCharsets.UTF_8, GSON);
        } catch (IOException exception) {
            log.error("Failed to load config!", exception);
            return null;
        }
    }

    /**
     * Saves the configuration to the config file
     */
    public void save() {
        try {
            ObjectLoader.saveTo(this, ModularBotConstants.PATH_MODULAR_BOT_CONFIG, StandardCharsets.UTF_8, GSON);
        } catch (IOException exception) {
            log.error("Failed to save config!", exception);
        }
    }

    /**
     * Discord related settings
     */
    @Getter
    public static final class Discord {

        private String token = "### YOUR TOKEN HERE ###";
        private long ownerId = 0L;

        private ShardManager shardManager = new ShardManager();
        private PresenceActivityCycle presenceActivityCycle = new PresenceActivityCycle();

        @Getter
        public static final class ShardManager {

            private boolean light = true;
            private List<GatewayIntent> gatewayIntents = new LinkedList<>();

            private int totalShards = 1;
            private List<Integer> shardIds = new LinkedList<>();
            private int minShardId = -1;
            private int maxShardId = -1;
        }

        @Getter
        public static final class PresenceActivityCycle {
            private boolean enabled = true;
            private long cycleIntervalMillis = 10000;
        }
    }

    @Getter
    public static final class Irminsul {

        private DatabaseServiceConfiguration database = new DatabaseServiceConfiguration();

    }
}
