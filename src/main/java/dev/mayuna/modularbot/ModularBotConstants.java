package dev.mayuna.modularbot;

import java.nio.file.Path;

/**
 * Modular Bot Constants
 */
public final class ModularBotConstants {

    // TODO: Přidělat všechny paths na Path
    public static final Path PATH_FOLDER_MODULES = Path.of("./modules");
    public static final String PATH_FOLDER_MODULE_CONFIGS = "./modules/%s";
    public static final String PATH_FOLDER_JSON_DATA = "./json_data/";
    public static final Path PATH_MODULAR_BOT_CONFIG = Path.of("./modular_bot.json");
    public static final String FILE_NAME_MODULE_INFO = "module_info.json";
    public static final String FILE_NAME_MODULE_CONFIG = "config.json";

    private static final String VERSION = "2.1.6";

    private ModularBotConstants() {
    }

    /**
     * Returns Modular Bot's version
     *
     * @return Version
     */
    public static String getVersion() {
        return VERSION;
    }
}
