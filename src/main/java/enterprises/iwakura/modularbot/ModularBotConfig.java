package enterprises.iwakura.modularbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.mayuna.mayusjsonutils.MayuJson;
import dev.mayuna.mayusjsonutils.ObjectLoader;
import enterprises.iwakura.keqing.Keqing;
import enterprises.iwakura.keqing.impl.GsonSerializer;
import enterprises.iwakura.keqing.impl.SnakeYamlSerializer;
import enterprises.iwakura.irminsul.DatabaseServiceConfiguration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Modular Bot Config
 */
@SuppressWarnings("FieldMayBeFinal")
@Getter
@Slf4j
public final class ModularBotConfig {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String CONFIG_FILE_NAME_TEMPLATE = "modular_bot";

    // Settings
    private Discord discord = new Discord();
    private Modules modules = new Modules();

    /**
     * Loads the configuration from the config file
     *
     * @return The loaded configuration
     */
    public static ModularBotConfig load() {
        try {
            ObjectLoader.loadOrCreateFrom(ModularBotConfig.class, Path.of(CONFIG_FILE_NAME_TEMPLATE + ".json"), StandardCharsets.UTF_8, GSON);
            var keqing = Keqing.loadFromFileSystem(CONFIG_FILE_NAME_TEMPLATE, '-', new GsonSerializer(GSON));
            return keqing.readProperty("", ModularBotConfig.class);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load config!", exception);
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
    public static final class Modules {

        private List<String> moduleDirectories = new ArrayList<>(List.of("./modules"));
        private int amberDownloaderThreads = 64;
        private boolean overrideModuleDependenciesLibraryDirectory = true;
    }
}
