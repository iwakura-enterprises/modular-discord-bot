package enterprises.iwakura.modularbot.config.serializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import enterprises.iwakura.modularbot.config.ConfigSerializer;

/**
 * Config serializer using Gson
 */
public class GsonConfigSerializer extends ConfigSerializer {

    private final Gson gson;

    /**
     * Creates a new GsonConfigSerializer with the provided Gson instance
     *
     * @param gson Gson instance to use for serialization and deserialization
     */
    public GsonConfigSerializer(Gson gson) {
        this.gson = gson;
    }

    /**
     * Creates a new GsonConfigSerializer with a default Gson instance, which pretty prints JSON
     */
    public GsonConfigSerializer() {
        this(new GsonBuilder().setPrettyPrinting().create());
    }

    /**
     * Serializes object to string
     *
     * @param obj Object to serialize
     *
     * @return Serialized string
     */
    @Override
    public String serialize(Object obj) {
        return gson.toJson(obj);
    }

    /**
     * Deserializes string to object
     *
     * @param data  Data to deserialize
     * @param clazz Class of object
     * @param <T>   Type of object
     *
     * @return Deserialized object
     */
    @Override
    public <T> T deserialize(String data, Class<T> clazz) {
        return gson.fromJson(data, clazz);
    }
}