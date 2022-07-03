package dev.mayuna.modularbot.objects.data;

import com.google.gson.*;
import dev.mayuna.modularbot.ModularBot;
import dev.mayuna.modularbot.logging.Logger;
import dev.mayuna.modularbot.managers.ModuleManager;
import dev.mayuna.modularbot.objects.Module;
import dev.mayuna.modularbot.utils.Config;
import lombok.Getter;
import lombok.NonNull;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DataHolder implements JsonSerializer<DataHolder>, JsonDeserializer<DataHolder> {

    private final @Getter long id;
    private @Getter Map<Class<?>, DataElement> dataElementMap;

    private DataHolder() {
        this.id = -1;
    }

    public DataHolder(long id) {
        this.id = id;
        this.dataElementMap = Collections.synchronizedMap(new HashMap<>());
    }

    /**
     * Loads {@link DataHolder} from {@link JsonObject}
     *
     * @param jsonObject Non-null {@link JsonObject}
     *
     * @return Non-null {@link DataHolder}
     */
    public static @NonNull DataHolder loadFromJsonObject(@NonNull JsonObject jsonObject) {
        return new GsonBuilder().registerTypeAdapter(DataHolder.class, new DataHolder()).create().fromJson(jsonObject, DataHolder.class);
    }

    public void save() {
        ModularBot.getDataManager().save(this);
    }

    /**
     * Returns current {@link DataHolder} as {@link JsonObject}
     *
     * @return Non-null {@link JsonObject}
     */
    public @NonNull JsonObject getAsJsonObject() {
        return new GsonBuilder().registerTypeAdapter(DataHolder.class, new DataHolder()).create().toJsonTree(this).getAsJsonObject();
    }

    /**
     * Gets or creates specified {@link DataElement} by your type {@link T}. Your {@link DataElement} must have at-least one public no-args
     * constructor or this method will result in {@link RuntimeException}
     *
     * @param dataElementClass Non-null class of implementation of your {@link DataElement}
     * @param <T>              Your implementation of {@link DataElement}
     *
     * @return Non-null implementation of your {@link DataElement}
     */
    public <T extends DataElement> T getOrCreateDataElement(Class<T> dataElementClass) {
        synchronized (dataElementMap) {
            for (Map.Entry<Class<?>, DataElement> entry : dataElementMap.entrySet()) {
                if (entry.getKey().equals(dataElementClass)) {
                    return (T) entry.getValue();
                }
            }
        }

        try {
            T dataElement = dataElementClass.getConstructor().newInstance();

            synchronized (dataElementMap) {
                dataElementMap.put(dataElementClass, dataElement);
            }

            return dataElement;
        } catch (Exception exception) {
            throw new RuntimeException("Exception occurred while creating new instance for " + dataElementClass + " data element! Please, check if there is public no-args constructor.", exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataHolder deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        DataHolder dataHolder = new DataHolder(jsonObject.get("id").getAsLong());

        JsonArray jsonArray = jsonObject.getAsJsonArray("dataMap");
        for (JsonElement jsonElement : jsonArray) {
            JsonObject mapEntryJsonObject = jsonElement.getAsJsonObject();
            String clazzToLoad = mapEntryJsonObject.get("class").getAsString();

            try {
                Class<?> clazz = ModuleManager.getInstance().getJarClassLoader().loadClass(clazzToLoad);

                DataElement dataElement = (DataElement) getGsonWithClass(clazz).fromJson(mapEntryJsonObject.get("data").getAsJsonObject(), clazz);
                dataHolder.dataElementMap.put(clazz, dataElement);
            } catch (ClassNotFoundException exception) {
                Logger.error("Class " + clazzToLoad + " (which is in Data Holder with ID " + dataHolder.getId() + ") does not exist currently! This can be due to class' module not loaded (eg. deleted) or some other error. Please, check your logs for any other error. Data from this class cannot be preserved.");

                if (Config.getInstance().getData().isHaltIfEncounteredUnknownClass()) {
                    Logger.warn("haltIfEncounteredUnknownClass is set to true -> Halting the application, data WILL be preserved (since any data won't be saved now)");
                    ModularBot.setDontSaveData(true);
                    ModularBot.shutdownGracefully();
                }
            } catch (Exception exception) {
                throw new JsonParseException(exception);
            }
        }

        return dataHolder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonElement serialize(DataHolder src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", src.id);

        JsonArray jsonArray = new JsonArray();
        for (Map.Entry<Class<?>, DataElement> entry : src.dataElementMap.entrySet()) {
            Class<?> clazz = entry.getKey();

            JsonObject entryJsonObject = new JsonObject();

            entryJsonObject.addProperty("class", entry.getKey().getName());
            entryJsonObject.add("data", getGsonWithClass(clazz).toJsonTree(entry.getValue()));

            jsonArray.add(entryJsonObject);

        }
        jsonObject.add("dataMap", jsonArray);
        return jsonObject;
    }

    private Gson getGsonWithClass(Class<?> clazz) {
        if (clazz.isAssignableFrom(JsonSerializer.class) || clazz.isAssignableFrom(JsonDeserializer.class)) {
            try {
                return new GsonBuilder().registerTypeAdapter(clazz, clazz.getConstructor().newInstance()).create();
            } catch (Exception exception) {
                throw new JsonParseException("Please, check if there is public no-args constructor in class " + clazz + ".", exception);
            }
        } else {
            return new Gson();
        }
    }
}
