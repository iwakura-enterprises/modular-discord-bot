package enterprises.iwakura.modularbot.config;

import enterprises.iwakura.modularbot.objects.ModuleInfo;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Module config
 */
public abstract class ModuleConfig {

    private final Map<Class<?>, RegisteredConfig> registeredConfigs = Collections.synchronizedMap(new HashMap<>());
    private final Map<Class<?>, Object> loadedConfigs = Collections.synchronizedMap(new HashMap<>());

    private final ModuleInfo moduleInfo;
    private final String modulesDirectoryPath;

    /**
     * Creates new {@link ModuleConfig}
     *
     * @param moduleInfo           Module info
     * @param modulesDirectoryPath Path to the modules directory, or null to use the default "./modules"
     */
    public ModuleConfig(ModuleInfo moduleInfo, String modulesDirectoryPath) {
        this.moduleInfo = moduleInfo;
        this.modulesDirectoryPath = Optional.ofNullable(modulesDirectoryPath).orElse("./modules");
    }

    /**
     * Registers all configs here using the {@link #register(Class, String, ConfigSerializer)} method
     */
    public abstract void register();

    /**
     * Registers a config class with the given name and serializer
     *
     * @param clazz            Class of the config
     * @param name             Name of the config file
     * @param configSerializer Serializer to use
     */
    public void register(Class<?> clazz, String name, ConfigSerializer configSerializer) {
        if (registeredConfigs.containsKey(clazz)) {
            throw new IllegalStateException("Config class " + clazz.getName() + " is already registered!");
        }

        registeredConfigs.put(clazz, new RegisteredConfig(clazz, name, configSerializer));
    }

    /**
     * Copies all registered config files from the resources to the module's config directory if they do not exist yet
     *
     * @param classLoader ClassLoader to use to load the resources
     */
    public void copyResourceConfigs(ClassLoader classLoader) {
        for (var registeredConfig : registeredConfigs.values()) {
            var configPath = Path.of("%s/%s/%s".formatted(modulesDirectoryPath, moduleInfo.getName(), registeredConfig.getName()));
            try {
                Files.createDirectories(configPath.getParent());

                if (!Files.exists(configPath)) {
                    try (var inputStream = classLoader.getResourceAsStream(registeredConfig.getName())) {
                        if (inputStream != null) {
                            Files.copy(inputStream, configPath);
                        }
                    }
                }
            } catch (IOException exception) {
                throw new RuntimeException("Could not copy resource config file for class %s at path %s!".formatted(
                        registeredConfig.getClazz().getName(), configPath), exception
                );
            }
        }
    }

    /**
     * Gets the config of the given class, or loads it if not loaded yet. If the config file does not exist, it will be created with default values
     * (using the public no-args constructor of the class).
     *
     * @param clazz Class of the config
     * @param <T>   Type of the config
     *
     * @return Instance of the config
     */
    public <T> T getOrLoad(Class<T> clazz) {
        var registeredConfig = registeredConfigs.get(clazz);

        if (registeredConfig == null) {
            throw new IllegalStateException("Config class " + clazz.getName() + " is not registered!");
        }

        if (loadedConfigs.containsKey(clazz)) {
            return clazz.cast(loadedConfigs.get(clazz));
        }

        var configPath = Path.of("%s/%s/%s".formatted(modulesDirectoryPath, moduleInfo.getName(), registeredConfig.getName()));
        String contents;
        try {
            Files.createDirectories(configPath.getParent());

            if (!Files.exists(configPath)) {
                Files.createFile(configPath);
                Files.writeString(configPath, registeredConfig.getConfigSerializer().serialize(clazz.getDeclaredConstructor().newInstance()));
            }

            contents = Files.readString(configPath);
        } catch (IOException exception) {
            throw new RuntimeException("Could not read config file for class " + clazz.getName() + " at path " + configPath + "!", exception);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException("Could not create new instance of the config class! (missing public no-args constructor?)", e);
        }
        var config = registeredConfig.getConfigSerializer().deserialize(contents, clazz);
        loadedConfigs.put(clazz, config);
        return config;
    }

    /**
     * Saves the given config object to its corresponding file
     *
     * @param object Config object to save
     */
    public void save(Object object) {
        var clazz = object.getClass();
        var registeredConfig = registeredConfigs.get(clazz);

        if (registeredConfig == null) {
            throw new IllegalStateException("Config class " + clazz.getName() + " is not registered!");
        }

        var configPath = Path.of("%s/%s/%s".formatted(modulesDirectoryPath, moduleInfo.getName(), registeredConfig.getName()));
        String contents = registeredConfig.getConfigSerializer().serialize(object);
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, contents);
        } catch (IOException exception) {
            throw new RuntimeException("Could not write config file for class " + clazz.getName() + " at path " + configPath + "!", exception);
        }
    }

    /**
     * Contains information about a registered config
     */
    @Data
    @RequiredArgsConstructor
    public static class RegisteredConfig {

        private final Class<?> clazz;
        private final String name;
        private final ConfigSerializer configSerializer;

    }
}
