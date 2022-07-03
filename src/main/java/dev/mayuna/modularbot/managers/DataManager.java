package dev.mayuna.modularbot.managers;

import com.google.gson.JsonObject;
import dev.mayuna.mayusjsonutils.JsonUtil;
import dev.mayuna.modularbot.ModularBot;
import dev.mayuna.modularbot.logging.Logger;
import dev.mayuna.modularbot.objects.data.DataHolder;
import dev.mayuna.modularbot.utils.Config;
import dev.mayuna.modularbot.utils.SQLUtil;
import dev.mayuna.modularbot.utils.SQLiteUtil;
import lombok.Getter;
import lombok.NonNull;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DataManager {

    public static final long GLOBAL_DATA_HOLDER_ID = Long.MIN_VALUE;

    private final @Getter List<DataHolder> dataHolderList = Collections.synchronizedList(new LinkedList<>());

    private DataHolder globalDataHolder;

    /**
     * Gets or creates {@link DataHolder} by its id
     *
     * @param id Any long number (for example, Discord's Text Channel ID, etc.)
     *
     * @return Non-null {@link DataHolder}
     *
     * @throws IllegalArgumentException If the argument is {@link Long#MIN_VALUE} (this ID is reserved)
     */
    public @NonNull DataHolder getOrCreateDataHolder(long id) {
        if (id == GLOBAL_DATA_HOLDER_ID) {
            throw new IllegalArgumentException("ID -2^63 (Long#MIN_VALUE) is reserved for Global Data Holder!");
        }

        DataHolder dataHolder = dataHolderList.stream().filter(dataHolderFilter -> dataHolderFilter.getId() == id).findFirst().orElse(null);

        if (dataHolder == null) {
            dataHolder = loadById(id);

            if (dataHolder == null) {
                dataHolder = new DataHolder(id);
                dataHolderList.add(dataHolder);
                save(dataHolder);
            }
        }

        return dataHolder;
    }

    ////////////////
    // = Saving = //
    ////////////////

    /**
     * Saves all currently loaded data
     */
    public void saveAll() {
        Logger.info("Saving data...");

        long start = System.currentTimeMillis();
        dataHolderList.forEach(this::save);
        getGlobalDataHolder().save();

        Logger.success("Data was saved in " + (System.currentTimeMillis() - start) + " ms!");
    }

    /**
     * Saves specified {@link DataHolder}
     *
     * @param dataHolder Non-null {@link DataHolder}
     */
    public void save(@NonNull DataHolder dataHolder) {
        synchronized (dataHolder) {
            switch (Config.getInstance().getData().getFormat()) {
                case SQL -> {
                    saveSQL(dataHolder);
                }
                case SQLITE -> {
                    saveSQLite(dataHolder);
                }
                case JSON -> {
                    saveJson(dataHolder);
                }
            }
        }
    }

    /////////////
    // - SQL - //
    /////////////

    private void saveSQL(DataHolder dataHolder) {
        SQLUtil.insertOrReplace(dataHolder);
    }

    ////////////////
    // - Sqlite - //
    ////////////////

    private void saveSQLite(DataHolder dataHolder) {
        SQLiteUtil.insertOrReplace(dataHolder);
    }

    //////////////
    // - Json - //
    //////////////

    private void saveJson(DataHolder dataHolder) {
        File folder = new File(ModularBot.Values.getPathFolderJsonData());

        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                Logger.error("Failed to create " + ModularBot.Values.getPathFolderJsonData() + " folder!");
                return;
            }
        }

        File file = new File(ModularBot.Values.getPathFolderJsonData() + dataHolder.getId() + ".json");

        try {
            beforeSaving(dataHolder);
            JsonUtil.saveJson(dataHolder.getAsJsonObject(), file);
        } catch (Exception exception) {
            Logger.get().error("Failed to save data holder " + dataHolder.getId() + "!");
        }
    }

    ///////////////
    // - Other - //
    ///////////////

    private void beforeSaving(DataHolder dataHolder) {
        dataHolder.getDataElementMap().values().forEach(dataElement -> {
            try {
                dataElement.beforeSave();
            } catch (Exception exception) {
                Logger.get().error("Exception occurred while calling #beforeSave() on " + dataElement.getClass() + "!", exception);
            }
        });
    }

    /////////////////
    // = Loading = //
    /////////////////

    /**
     * Loads all data
     */
    public void loadAll() {
        dataHolderList.clear();
        long start = System.currentTimeMillis();

        switch (Config.getInstance().getData().getFormat()) {
            case SQL -> {
                loadAllSQL();
            }
            case SQLITE -> {
                loadAllSQLite();
            }
            case JSON -> {
                loadAllJson();
            }
        }

        Logger.success("Loaded " + dataHolderList.size() + " data in " + (System.currentTimeMillis() - start) + "ms!");
    }

    private DataHolder loadById(long id) {
        switch (Config.getInstance().getData().getFormat()) {
            case SQL -> {
                return loadSQLById(id);
            }
            case SQLITE -> {
                return loadSQLiteById(id);
            }
            case JSON -> {
                return loadJsonById(id);
            }
            default -> {
                throw new RuntimeException("Data format is not set in Config file!");
            }
        }
    }

    /////////////
    // - SQL - //
    /////////////

    private void loadAllSQL() {
        SQLUtil.fetchAllIds().forEach(id -> {
            if (id == GLOBAL_DATA_HOLDER_ID) {
                return;
            }

            DataHolder dataHolder = loadSQLById(id);

            if (dataHolder != null) {
                dataHolderList.add(dataHolder);
            }
        });
    }

    private DataHolder loadSQLById(long id) {
        return SQLUtil.loadById(id);
    }

    ////////////////
    // - Sqlite - //
    ////////////////

    private void loadAllSQLite() {
        SQLiteUtil.fetchAllIds().forEach(id -> {
            if (id == GLOBAL_DATA_HOLDER_ID) {
                return;
            }

            DataHolder dataHolder = loadSQLiteById(id);

            if (dataHolder != null) {
                dataHolderList.add(dataHolder);
            }
        });
    }

    private DataHolder loadSQLiteById(long id) {
        return SQLiteUtil.loadById(id);
    }

    //////////////
    // - Json - //
    //////////////

    private void loadAllJson() {
        File folder = new File(ModularBot.Values.getPathFolderJsonData());

        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                Logger.error("Failed to create " + ModularBot.Values.getPathFolderJsonData() + " folder!");
                return;
            }
        }

        File[] files = folder.listFiles();

        if (files == null) {
            Logger.error("Failed to list files in folder " + ModularBot.Values.getPathFolderJsonData() + "!");
            return;
        }

        for (File file : files) {
            DataHolder dataHolder = loadJsonByFile(file);

            if (dataHolder != null) {
                if (dataHolder.getId() == GLOBAL_DATA_HOLDER_ID) {
                    continue;
                }

                dataHolderList.add(dataHolder);
            }
        }
    }

    private DataHolder loadJsonById(long id) {
        File folder = new File(ModularBot.Values.getPathFolderJsonData());

        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                Logger.error("Failed to create " + ModularBot.Values.getPathFolderJsonData() + " folder!");
                return null;
            }
        }

        File file = new File(ModularBot.Values.getPathFolderJsonData() + id + ".json");

        return loadJsonByFile(file);
    }

    private DataHolder loadJsonByFile(File file) {
        if (!file.exists()) {
            return null;
        }

        try {
            JsonObject jsonObject = JsonUtil.createOrLoadJsonFromFile(file).getJsonObject();
            DataHolder dataHolder = DataHolder.loadFromJsonObject(jsonObject);
            onLoad(dataHolder);
            return dataHolder;
        } catch (Exception exception) {
            Logger.get().error("Failed to load file " + file.getPath() + "!", exception);
            return null;
        }
    }

    ///////////////
    // = Other = //
    ///////////////

    private void onLoad(DataHolder dataHolder) {
        dataHolder.getDataElementMap().values().forEach(dataElement -> {
            try {
                dataElement.onLoad();
            } catch (Exception exception) {
                Logger.get().error("Exception occurred while calling #onLoad() on " + dataElement.getClass() + "!", exception);
            }
        });
    }

    public void initDatabase() {
        switch (Config.getInstance().getData().getFormat()) {
            case SQL -> {
                SQLUtil.initPool();
                SQLUtil.createDataHolderDatabase();
            }
            case SQLITE -> {
                SQLiteUtil.createDataHolderDatabase();
            }
        }
    }

    /**
     * Gets the global {@link DataHolder}
     *
     * @return Non-null {@link DataHolder} (if Global Data Holder does not exist, it creates new one with ID {@link Long#MIN_VALUE}
     */
    public DataHolder getGlobalDataHolder() {
        if (globalDataHolder == null) {
            globalDataHolder = loadById(GLOBAL_DATA_HOLDER_ID);

            if (globalDataHolder == null) {
                globalDataHolder = new DataHolder(GLOBAL_DATA_HOLDER_ID);
                globalDataHolder.save();
            }
        }

        return globalDataHolder;
    }
}
