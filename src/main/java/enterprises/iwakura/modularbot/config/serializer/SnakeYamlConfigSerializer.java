package enterprises.iwakura.modularbot.config.serializer;

import enterprises.iwakura.modularbot.config.ConfigSerializer;
import org.yaml.snakeyaml.Yaml;

/**
 * Config serializer using SnakeYAML
 */
public class SnakeYamlConfigSerializer extends ConfigSerializer {

    private final Yaml yaml;

    /**
     * Creates a new SnakeYamlConfigSerializer with the provided Yaml instance
     *
     * @param yaml Yaml instance to use for serialization and deserialization
     */
    public SnakeYamlConfigSerializer(Yaml yaml) {
        this.yaml = yaml;
    }

    /**
     * Creates a new SnakeYamlConfigSerializer with a default Yaml instance
     */
    public SnakeYamlConfigSerializer() {
        this(new Yaml());
    }

    @Override
    public String serialize(Object obj) {
        return yaml.dump(obj);
    }

    @Override
    public <T> T deserialize(String data, Class<T> clazz) {
        return yaml.loadAs(data, clazz);
    }
}
