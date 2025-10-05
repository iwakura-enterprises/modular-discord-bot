package enterprises.iwakura.modularbot;

import java.nio.file.Path;

/**
 * Modular Bot Constants
 */
public final class ModularBotConstants {

    public static final Path PATH_FOLDER_MODULES = Path.of("./modules");
    public static final String FILE_NAME_MODULE_INFO = "module_info.json";

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
