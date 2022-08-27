package dev.mayuna.modularbot.objects;

import com.google.gson.JsonObject;
import dev.mayuna.mayusjsonutils.JsonUtil;
import dev.mayuna.mayusjsonutils.objects.MayuJson;
import dev.mayuna.modularbot.ModularBot;
import lombok.Getter;

import java.io.File;
import java.io.IOException;

public class ModuleConfig {

    private final Module module;
    private final ModuleInfo moduleInfo;
    private final @Getter JsonObject defaultConfig;
    private @Getter File dataFolder;
    private @Getter MayuJson mayuJson;

    public ModuleConfig(Module module, JsonObject defaultConfig) {
        this.module = module;
        this.defaultConfig = defaultConfig;

        this.moduleInfo = module.getModuleInfo();

        reload();
    }

    public ModuleConfig(Module module) {
        this(module, null);
    }

    public void reload() {
        String finalPath = String.format(ModularBot.Values.getPathFolderModuleConfigs(), module.getModuleInfo().name());

        dataFolder = new File(finalPath);

        if (!dataFolder.exists()) {
            if (!dataFolder.mkdirs()) {
                throw new RuntimeException(new IOException("Could not create data folder for module " + moduleInfo.name() + " (" + moduleInfo.version() + ") with path " + finalPath + "! Maybe the module name has illegal characters?"));
            }
        }

        try {
            mayuJson = JsonUtil.createOrLoadJsonFromFile(new File(dataFolder, "config.json"));
        } catch (Exception exception) {
            throw new RuntimeException("Could not create/load config.json file for module " + moduleInfo.name() + " (" + moduleInfo.version() + ") with path " + new File(dataFolder, "config.json") + "!");
        }
    }

    public void save() {
        try {
            mayuJson.saveJson();
        } catch (Exception exception) {
            throw new RuntimeException("Could not save config for module " + moduleInfo.name() + " (" + moduleInfo.version() + ") with config path " + mayuJson.getFile() + "!", exception);
        }
    }

    public void copyDefaultsIfEmpty() {
        if (!mayuJson.getJsonObject().entrySet().isEmpty()) {
            return;
        }

        mayuJson.setJsonObject(defaultConfig);
        save();
    }
}
