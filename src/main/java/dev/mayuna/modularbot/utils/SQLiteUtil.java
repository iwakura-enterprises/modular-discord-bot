package dev.mayuna.modularbot.utils;

import com.google.gson.JsonParser;
import dev.mayuna.modularbot.objects.data.DataHolder;
import lombok.NonNull;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

public class SQLiteUtil {

    // We want everything synchronized, because SQLite does not like accessing the database with more than one thread
    private static final Object mutex = new Object();

    private static Connection connectToDatabase() {
        synchronized (mutex) {
            try {
                return DriverManager.getConnection("jdbc:sqlite:" + Config.getInstance().getData().getSql().getDatabase() + ".db");
            } catch (Exception exception) {
                throw new RuntimeException(new SQLException("Could not create connection to sqlite database!", exception));
            }
        }
    }

    /**
     * Creates a Data Holder database if there is not one
     */
    public static void createDataHolderDatabase() {
        synchronized (mutex) {
            try (Connection connection = connectToDatabase()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(GeneralUtil.replaceAllSQLPlaceholders("""
                                                                                    CREATE TABLE IF NOT EXISTS {data_holder_table}
                                                                                    (
                                                                                    id BIGINT PRIMARY KEY NOT NULL,
                                                                                    data JSON NOT NULL
                                                                                    );
                                                                                    """));
                }
            } catch (SQLException exception) {
                throw new RuntimeException("Exception occurred while creating DataHolders table in SQLite database!", exception);
            }
        }
    }

    /**
     * Loads {@link DataHolder} by its id from the database
     *
     * @param id id
     *
     * @return Nullable {@link DataHolder} (null if it does not exist)
     */
    public static DataHolder loadById(long id) {
        synchronized (mutex) {
            try (Connection connection = connectToDatabase()) {
                String sql = GeneralUtil.replaceAllSQLPlaceholders("SELECT data FROM {data_holder_table} WHERE id = ?;");

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, id);

                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return DataHolder.loadFromJsonObject(JsonParser.parseString(resultSet.getString("data")).getAsJsonObject());
                        }
                    }
                }
            } catch (SQLException exception) {
                throw new RuntimeException("Exception occurred while loading DataHolder with ID " + id + "  from SQLite database!", exception);
            }
        }

        return null;
    }

    /**
     * Fetches all IDs
     *
     * @return Non-null {@link LinkedList} with IDs
     */
    public static @NonNull List<Long> fetchAllIds() {
        synchronized (mutex) {
            try (Connection connection = connectToDatabase()) {
                try (Statement statement = connection.createStatement()) {
                    String sql = GeneralUtil.replaceAllSQLPlaceholders("SELECT id FROM {data_holder_table};");

                    try (ResultSet resultSet = statement.executeQuery(sql)) {
                        List<Long> ids = new LinkedList<>();

                        while (resultSet.next()) {
                            ids.add(resultSet.getLong("id"));
                        }

                        return ids;
                    }
                }
            } catch (SQLException exception) {
                throw new RuntimeException("Exception occurred while fetching all IDs from from SQLite database!", exception);
            }
        }
    }

    /**
     * Inserts or replaces specified {@link DataHolder} in the database
     *
     * @param dataHolder Non-null {@link DataHolder}
     */
    public static void insertOrReplace(@NonNull DataHolder dataHolder) {
        synchronized (mutex) {
            try (Connection connection = connectToDatabase()) {
                String sql = GeneralUtil.replaceAllSQLPlaceholders("REPLACE INTO {data_holder_table} (id, data) VALUES (?, ?)");

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, dataHolder.getId());
                    statement.setString(2, dataHolder.getAsJsonObject().toString());

                    statement.executeUpdate();
                }
            } catch (SQLException exception) {
                throw new RuntimeException("Exception occurred while saving DataHolder with ID " + dataHolder.getId() + " to SQLite database!", exception);
            }
        }
    }
}
