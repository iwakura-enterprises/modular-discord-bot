package dev.mayuna.modularbot.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zaxxer.hikari.HikariConfig;
import dev.mayuna.modularbot.ModularBot;
import dev.mayuna.modularbot.base.ModuleManager;
import dev.mayuna.modularbot.logging.Logger;
import dev.mayuna.modularbot.managers.ModuleManagerImpl;
import dev.mayuna.pumpk1n.api.StorageHandler;
import dev.mayuna.pumpk1n.impl.FolderStorageHandler;
import dev.mayuna.pumpk1n.impl.SQLStorageHandler;
import dev.mayuna.pumpk1n.impl.SQLiteStorageHandler;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ModularBotConfig {

    private static @Getter @Setter ModularBotConfig instance = new ModularBotConfig();

    private @Getter Bot bot = new Bot();
    private @Getter Data data = new Data();

    public static boolean load() {
        try {
            if (!Files.exists(getPath())) {
                if (!save()) {
                    Logger.fatal("Could not save config!");
                    return false;
                }
            }

            instance = getGson().fromJson(String.join("", Files.readAllLines(getPath())), ModularBotConfig.class);
            save();
            return true;
        } catch (Exception exception) {
            Logger.get().fatal("Could not load config (" + getPath() + ")!", exception);
            return false;
        }
    }

    private static Path getPath() {
        return Path.of(ModularBot.Values.getFileNameConfig());
    }

    private static Gson getGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    /**
     * Saves the config
     *
     * @return True if saved, false if there was some exception
     */
    public static boolean save() {
        try {
            Files.writeString(getPath(), getGson().toJson(instance), StandardOpenOption.CREATE);
            return true;
        } catch (Exception exception) {
            Logger.get().fatal("Could not save config (" + getPath() + ")!", exception);
            return false;
        }
    }

    public static class Bot {

        protected @Getter String token = "### YOUR TOKEN HERE ###";
        protected @Getter long ownerId = 0;
        protected @Getter long botId = 0;

        protected @Getter int totalShards = 1;
        protected @Getter boolean waitOnAllShards = true;
        protected @Getter boolean showEtaWhenWaitingOnShards = true;
        protected @Getter long restartShardEveryIfNecessary = 60000;

        protected @Getter GlobalRateLimiter globalRateLimiter = new GlobalRateLimiter();
        protected @Getter PresenceActivity presenceActivity = new PresenceActivity();

        public static class GlobalRateLimiter {

            protected @Getter boolean enabled = false;
            protected @Getter long resetRequestsCountAfter = 1000;
            protected @Getter int maxRequestCount = 50;
            protected @Getter String[] ignoredEndpoints = new String[]{
                "POST/interactions/{interaction_id}/{interaction_token}/callback",
                "GET/webhooks/{application_id}/{interaction_token}/messages/{message_id}",
                "PATCH/webhooks/{application_id}/{interaction_token}/messages/{message_id}",
                "DELETE/webhooks/{application_id}/{interaction_token}/messages/{message_id}",
                "POST/webhooks/{application_id}/{interaction_token}",
                "GET/webhooks/{application_id}/{interaction_token}/messages/{message_id}",
                "PATCH/webhooks/{application_id}/{interaction_token}/messages/{message_id}",
                "DELETE/webhooks/{application_id}/{interaction_token}/messages/{message_id}"
            };
        }

        public static class PresenceActivity {

            protected @Getter boolean enabled = true;
            protected @Getter long cycleInterval = 10000;
        }
    }

    public static class Data {

        protected @Getter boolean enabled = true;
        protected @Getter String format;
        protected @Getter SQL sql = new SQL();

        public StorageHandler getStorageHandler() {
            if (!enabled) {
                return null;
            }

            StaticFormat staticFormat = StaticFormat.safeValueOf(format);

            if (staticFormat != null) {
                switch (staticFormat) {
                    case SQL -> {
                        HikariConfig hikariConfig = new HikariConfig();
                        String fullHostname = sql.getHostname();

                        if (sql.getPort() != null && !sql.getPort().isEmpty()) {
                            fullHostname += ":" + sql.getPort();
                        }

                        // TODO: SSL
                        hikariConfig.setJdbcUrl("jdbc:mysql://" + fullHostname + "/" + sql.getDatabase());
                        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
                        hikariConfig.setUsername(sql.getUsername());
                        hikariConfig.setPassword(sql.getPassword());
                        hikariConfig.setMinimumIdle(sql.getSettings().getMinimumConnections());
                        hikariConfig.setMaximumPoolSize(sql.getSettings().getMaximumConnections());
                        hikariConfig.setConnectionTimeout(sql.getSettings().getTimeout());
                        hikariConfig.setPoolName(sql.getSettings().getPoolName());
                        return new SQLStorageHandler(hikariConfig, sql.getTables().getDataHolders());
                    }
                    case SQLITE -> {
                        return new SQLiteStorageHandler(SQLiteStorageHandler.Settings.Builder.create()
                                                                                             .setFileName(sql.getDatabase() + ".db")
                                                                                             .setTableName(sql.getTables().getDataHolders())
                                                                                             .build());
                    }
                    case JSON -> {
                        return new FolderStorageHandler(ModularBot.Values.getPathFolderJsonData());
                    }
                }
            } else {
                try {
                    Class<?> clazz = ModularBot.getModuleManager().getJarClassLoader().loadClass(format, true);
                    return (StorageHandler) clazz.getConstructor().newInstance();
                } catch (Exception exception) {
                    Logger.get().error("Failed to load custom storage format! Check if the storage handler has public non-args constructor, if it's in classpath and if the specified class is type of StorageHandler.", exception);
                    ModularBot.shutdownGracefully();
                }
            }

            return null;
        }

        public static class SQL {

            protected @Getter String hostname = "localhost";
            protected @Getter String port = "3306";
            protected @Getter String database = "modular_bot";
            protected @Getter String username = "root";
            protected @Getter String password = "password123";
            protected @Getter Settings settings = new Settings();
            protected @Getter Tables tables = new Tables();

            public static class Settings {

                protected @Getter boolean useSSL = false;
                protected @Getter int minimumConnections = 2;
                protected @Getter int maximumConnections = 5;
                protected @Getter long timeout = 30000;
                protected @Getter String poolName = "modular_bot";
            }

            public static class Tables {

                protected @Getter String dataHolders = "data_holders";
            }
        }

        public enum StaticFormat {
            SQL,
            SQLITE,
            JSON;

            public static StaticFormat safeValueOf(String name) {
                try {
                    return StaticFormat.valueOf(name);
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
    }
}
