package enterprises.iwakura.modularbot.objects;

import com.google.gson.JsonObject;
import dev.mayuna.mayusjsonutils.MayuJson;
import enterprises.iwakura.modularbot.ModularBotConstants;
import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Module config
 */
@Getter
public final class ModuleConfig {

    private final ModuleInfo moduleInfo;
    private final JsonObject defaultConfig;

    private Path dataDirectoryPath;
    private Path configPath;
    private MayuJson mayuJson;

    /**
     * Creates new {@link ModuleConfig}
     *
     * @param moduleInfo        Non-null {@link ModuleInfo}
     * @param defaultConfig Non-null {@link JsonObject}
     */
    public ModuleConfig(@NonNull ModuleInfo moduleInfo, @NonNull JsonObject defaultConfig) {
        this.moduleInfo = moduleInfo;
        this.defaultConfig = defaultConfig;

        reload();
    }

    /**
     * Reloads module's config
     */
    public void reload() {
        dataDirectoryPath = Path.of(String.format(ModularBotConstants.PATH_FOLDER_MODULE_CONFIGS, moduleInfo.getName()));

        if (!Files.exists(dataDirectoryPath)) {
            try {
                Files.createDirectories(dataDirectoryPath);
            } catch (IOException exception) {
                throw new RuntimeException(new IOException("Could not create data folder for module " + moduleInfo.getName() + " (" + moduleInfo.getVersion() + ") with path " + dataDirectoryPath + "! Maybe the module name has illegal characters?"));
            }
        }

        configPath = Path.of(dataDirectoryPath.toString(), "config.json");

        try {
            mayuJson = MayuJson.createOrLoadJsonObject(configPath);
        } catch (IOException exception) {
            throw new RuntimeException("Could not create/load config.json file for module " + moduleInfo.getName() + " (" + moduleInfo.getVersion() + ") with path " + configPath + "!");
        }
    }

    /**
     * Saves module's config
     */
    public void save() {
        try {
            mayuJson.save();
        } catch (Exception exception) {
            throw new RuntimeException("Could not save config for module " + moduleInfo.getName() + " (" + moduleInfo.getVersion() + ") with config path " + mayuJson.getPath() + "!", exception);
        }
    }

    /**
     * Copies default config if empty
     */
    public void copyDefaultsIfEmpty() {
        if (!mayuJson.getJsonObject().entrySet().isEmpty()) {
            return;
        }

        mayuJson.setJsonObject(defaultConfig);
        save();
    }
}
