package enterprises.iwakura.modularbot.config;

/**
 * Abstract class for config serializers
 */
public abstract class ConfigSerializer {

    /**
     * Serializes object to string
     *
     * @param obj Object to serialize
     *
     * @return Serialized string
     */
    public abstract String serialize(Object obj);

    /**
     * Deserializes string to object
     *
     * @param data  Data to deserialize
     * @param clazz Class of object
     * @param <T>   Type of object
     *
     * @return Deserialized object
     */
    public abstract <T> T deserialize(String data, Class<T> clazz);

}
