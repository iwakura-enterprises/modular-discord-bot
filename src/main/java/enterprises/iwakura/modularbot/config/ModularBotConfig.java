package enterprises.iwakura.modularbot.config;

import enterprises.iwakura.jean.Jean;
import enterprises.iwakura.jean.LoadOptions;
import enterprises.iwakura.jean.serializer.GsonSerializer;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Modular Bot Config, extends {@link Jean} to provide configuration management
 */
@Bean
@Slf4j
public class ModularBotConfig extends Jean {

    public ModularBotConfig() {
        super(
                Path.of("."),
                new GsonSerializer(),
                LoadOptions.builder().saveOnLoad(true).build()
        );
    }

    /**
     * Returns the Discord related settings
     *
     * @return the Discord settings
     */
    public Discord getDiscord() {
        return this.getOrLoad("discord", Discord.class);
    }

    /**
     * Returns the Modules related settings
     *
     * @return the Modules settings
     */
    public Modules getModules() {
        return this.getOrLoad("modules", Modules.class);
    }

    /**
     * Discord related settings
     */
    @Data
    public static final class Discord {

        private String token = "### YOUR TOKEN HERE ###";
        private long ownerId = 0L;

        private ShardManager shardManager = new ShardManager();
        private PresenceActivityCycle presenceActivityCycle = new PresenceActivityCycle();

        @Data
        public static final class ShardManager {

            private boolean light = true;
            private List<GatewayIntent> gatewayIntents = new LinkedList<>();

            private int totalShards = 1;
            private List<Integer> shardIds = new LinkedList<>();
            private int minShardId = -1;
            private int maxShardId = -1;
        }

        @Data
        public static final class PresenceActivityCycle {

            private boolean enabled = true;
            private long cycleIntervalMillis = 10000;
        }
    }

    @Data
    public static final class Modules {

        private List<String> moduleDirectories = new ArrayList<>(List.of("./modules"));
        private int amberDownloaderThreads = 64;
        private boolean overrideModuleDependenciesLibraryDirectory = true;
        private boolean crashOnModuleLoadFailure = true;
    }
}
