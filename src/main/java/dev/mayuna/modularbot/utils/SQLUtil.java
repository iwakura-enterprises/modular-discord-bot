package dev.mayuna.modularbot.utils;

import com.google.gson.JsonParser;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.mayuna.modularbot.objects.data.DataHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class SQLUtil {

    private static ConnectionPoolManager pool;

    public static void initPool() {
        if (pool != null) {
            return;
        }

        pool = new ConnectionPoolManager();
    }

    public static void closePool() {
        if (pool != null) {
            pool.closePool();
        }
    }

    public static void createDataHolderDatabase() {
        Connection connection = null;
        PreparedStatement statement = null;

        try {
            connection = pool.getConnection();
            statement = connection.prepareStatement(
                    GeneralUtil.replaceAllSQLPlaceholders("""
                                                                  CREATE TABLE IF NOT EXISTS {data_holder_table}
                                                                  (
                                                                  id BIGINT NOT NULL,
                                                                  data JSON NOT NULL,
                                                                  PRIMARY KEY (id)
                                                                  );
                                                                       """
                    )
            );

            statement.execute();
        } catch (Exception exception) {
            throw new RuntimeException("Exception occurred while creating DataHolders table in SQL database!", exception);
        } finally {
            pool.close(connection, statement, null);
        }
    }

    public static DataHolder loadById(long id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = pool.getConnection();
            statement = connection.prepareStatement(GeneralUtil.replaceAllSQLPlaceholders("SELECT data FROM {data_holder_table} WHERE id = ?;"));
            statement.setLong(1, id);
            statement.executeQuery();

            resultSet = statement.getResultSet();

            if (resultSet.next()) {
                return DataHolder.loadFromJsonObject(JsonParser.parseString(resultSet.getString("data")).getAsJsonObject());
            }

            return null;
        } catch (Exception exception) {
            throw new RuntimeException("Exception occurred while loading DataHolder with ID " + id + " from SQL database!", exception);
        } finally {
            pool.close(connection, statement, resultSet);
        }
    }

    public static List<Long> fetchAllIds() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = pool.getConnection();
            statement = connection.prepareStatement(GeneralUtil.replaceAllSQLPlaceholders("SELECT id FROM {data_holder_table};"));
            statement.executeQuery();

            resultSet = statement.getResultSet();
            List<Long> ids = new LinkedList<>();

            while (resultSet.next()) {
                ids.add(resultSet.getLong("id"));
            }

            return ids;
        } catch (Exception exception) {
            throw new RuntimeException("Exception occurred while fetching all IDs from SQL database!", exception);
        } finally {
            pool.close(connection, statement, resultSet);
        }
    }

    public static void insertOrReplace(DataHolder dataHolder) {
        Connection connection = null;
        PreparedStatement statement = null;

        try {
            connection = pool.getConnection();
            statement = connection.prepareStatement(GeneralUtil.replaceAllSQLPlaceholders("REPLACE INTO {data_holder_table} (id, data) VALUES (?, ?)"));
            statement.setLong(1, dataHolder.getId());
            statement.setString(2, dataHolder.getAsJsonObject().toString());
            statement.executeUpdate();
        } catch (Exception exception) {
            throw new RuntimeException("Exception occurred while saving DataHolder with ID " + dataHolder.getId() + " to SQL database!", exception);
        } finally {
            pool.close(connection, statement, null);
        }
    }

    public static class ConnectionPoolManager {

        private HikariDataSource dataSource;
        private String host;
        private String port;
        private String database;
        private String username;
        private String password;
        private String poolName;
        private int minimumConnections;
        private int maximumConnections;
        private long connectionTimeout;
        private boolean useSSL;

        public ConnectionPoolManager() {
            init();
            setupPool();
        }

        private void init() {
            host = Config.getInstance().getData().getSql().getHostname();
            port = Config.getInstance().getData().getSql().getPort();
            database = Config.getInstance().getData().getSql().getDatabase();
            username = Config.getInstance().getData().getSql().getUsername();
            password = Config.getInstance().getData().getSql().getPassword();
            minimumConnections = Config.getInstance().getData().getSql().getSettings().getMinimumConnections();
            maximumConnections = Config.getInstance().getData().getSql().getSettings().getMaximumConnections();
            connectionTimeout = Config.getInstance().getData().getSql().getSettings().getTimeout();
            useSSL = Config.getInstance().getData().getSql().getSettings().isUseSSL();
            poolName = Config.getInstance().getData().getSql().getSettings().getPoolName();
        }

        private void setupPool() {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?characterEncoding=UTF-8&autoReconnect=true&useSSL=" + useSSL);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setUsername(username);
            config.setPassword(password);
            config.setMinimumIdle(minimumConnections);
            config.setMaximumPoolSize(maximumConnections);
            config.setConnectionTimeout(connectionTimeout);
            config.setPoolName(poolName);
            dataSource = new HikariDataSource(config);
        }

        public Connection getConnection() throws SQLException {
            return dataSource.getConnection();
        }

        public void closePool() {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
        }

        public void close(Connection conn, PreparedStatement ps, ResultSet res) {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ignored) {
                }
            }
            if (res != null) {
                try {
                    res.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

}
