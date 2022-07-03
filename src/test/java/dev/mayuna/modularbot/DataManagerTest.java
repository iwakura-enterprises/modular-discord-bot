package dev.mayuna.modularbot;

import dev.mayuna.modularbot.data.TestData;
import dev.mayuna.modularbot.managers.DataManager;
import dev.mayuna.modularbot.objects.data.DataHolder;
import dev.mayuna.modularbot.utils.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;

public class DataManagerTest {

    @TempDir
    public static File folder;
    private static DataManager dataManager;

    @BeforeAll
    public static void prepare() throws Exception {
        ModularBot.Values.setFileNameConfig(new File(folder, ModularBot.Values.getFileNameConfig()).getAbsolutePath());
        ModularBot.Values.setPathFolderJsonData(new File(folder, "/json_data").getAbsolutePath());
        setReflectionDataSQLDatabase(new File(folder, "modular_bot.db").getAbsolutePath());

        Config.load();
        dataManager = new DataManager();
    }

    private static void setReflectionDataFormat(Config.Data.Format format) {
        try {
            Field field = Config.Data.class.getDeclaredField("format");
            field.setAccessible(true);
            field.set(Config.getInstance().getData(), format);

        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void setReflectionDataSQLDatabase(String database) {
        try {
            Field field = Config.Data.SQL.class.getDeclaredField("database");
            field.setAccessible(true);
            field.set(Config.getInstance().getData().getSql(), database);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    public void testJSONDatabase() {
        setReflectionDataFormat(Config.Data.Format.JSON);
        testData();
        testGlobalData();
    }

    @Test
    public void testSQLiteDatabase() {
        setReflectionDataFormat(Config.Data.Format.SQLITE);
        dataManager.initDatabase();
        testData();
        testGlobalData();
    }

    private void testData() {
        DataHolder dataHolder = dataManager.getOrCreateDataHolder(123);
        TestData testData = dataHolder.getOrCreateDataElement(TestData.class);
        testData.processNumber();

        int currentNumber = testData.getNumber();

        dataHolder.save();
        dataManager = new DataManager();
        dataHolder = dataManager.getOrCreateDataHolder(123);
        testData = dataHolder.getOrCreateDataElement(TestData.class);

        Assertions.assertEquals(currentNumber, testData.getNumber());
    }

    private void testGlobalData() {
        DataHolder dataHolder = dataManager.getGlobalDataHolder();
        TestData testData = dataHolder.getOrCreateDataElement(TestData.class);
        testData.processNumber();

        int currentNumber = testData.getNumber();

        dataHolder.save();
        dataManager = new DataManager();
        dataHolder = dataManager.getGlobalDataHolder();
        testData = dataHolder.getOrCreateDataElement(TestData.class);

        Assertions.assertEquals(currentNumber, testData.getNumber());
    }
}
