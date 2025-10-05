package enterprises.iwakura.modularbot;

import java.nio.file.Path;

/**
 * Modular Bot Constants
 */
public final class ModularBotConstants {

    public static final Path PATH_FOLDER_MODULES = Path.of("./modules");
    public static final String PATH_FOLDER_MODULE_CONFIGS = "./modules/%s";
    public static final String PATH_FOLDER_JSON_DATA = "./json_data/";
    public static final String FILE_NAME_MODULE_INFO = "module_info.json";
    public static final String FILE_NAME_MODULE_CONFIG = "config.json";

    private ModularBotConstants() {
    }

    /**
     * Returns Modular Bot's version
     *
     * @return Version
     */
    public static String getVersion() {
        return Version.VERSION;
    }
}
