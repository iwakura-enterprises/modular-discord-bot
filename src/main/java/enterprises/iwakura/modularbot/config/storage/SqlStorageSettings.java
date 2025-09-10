package enterprises.iwakura.modularbot.config.storage;

import com.zaxxer.hikari.HikariConfig;
import dev.mayuna.pumpk1n.impl.SQLStorageHandler;
import lombok.Getter;

/**
 * Settings for SQL storage
 */
@SuppressWarnings("FieldMayBeFinal")
@Getter
public final class SqlStorageSettings {

    private String url = "jdbc:mysql://localhost:3306/sakuyabridge";
    private String username = "sakuyabridge";
    private String password = "password";
    private long connectionTimeout = 30000;
    private int minActiveConnections = 1;
    private int maxPoolSize = 5;
    private String tableName;

    /**
     * Used for serialization
     */
    public SqlStorageSettings() {
        this("default");
    }

    /**
     * Creates a new SQL storage settings (used when creating new config file with default values)
     *
     * @param defaultTableName The default table name
     */
    public SqlStorageSettings(String defaultTableName) {
        this.tableName = defaultTableName;
    }

    /**
     * Creates a storage handler from the settings
     *
     * @return The storage handler
     */
    public SQLStorageHandler createStorageHandler() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setConnectionTimeout(connectionTimeout);
        config.setMinimumIdle(minActiveConnections);
        config.setMaximumPoolSize(maxPoolSize);

        return new SQLStorageHandler(config, tableName);
    }
}