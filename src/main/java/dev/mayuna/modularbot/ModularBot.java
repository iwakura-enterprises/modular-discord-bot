package dev.mayuna.modularbot;

import dev.mayuna.modularbot.logging.Logger;
import dev.mayuna.modularbot.managers.ModuleManager;
import dev.mayuna.modularbot.objects.BaseModule;

public class ModularBot {

    public static void main(String[] args) {
        Logger.info("");
        Logger.info("Modular Discord Bot");
        Logger.info("===================");
        Logger.info("Made by Mayuna");
        Logger.info("");
        Logger.info("Loading...");

        try {
            ModuleManager.getInstance().loadModules();
        } catch (Exception exception) {
            Logger.get().fatal("Exception occurred while loading modules!", exception);
        }
    }

    public static class Constants {

        public static final String PATH_FOLDER_MODULES = "./modules/";
        public static final String FILE_NAME_MODULE_INFO = "module_info.json";

    }
}
