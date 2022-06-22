package dev.mayuna.modularbot.managers;

import com.google.gson.JsonParser;
import dev.mayuna.modularbot.ModularBot;
import dev.mayuna.modularbot.logging.Logger;
import dev.mayuna.modularbot.objects.BaseModule;
import dev.mayuna.modularbot.objects.ModuleInfo;
import lombok.Getter;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.JclObjectFactory;

import java.io.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModuleManager {

    private static ModuleManager instance;

    private final @Getter List<BaseModule> loadedModules = Collections.synchronizedList(new LinkedList<>());

    private ModuleManager() {
    }

    public static ModuleManager getInstance() {
        if (instance == null) {
            instance = new ModuleManager();
        }

        return instance;
    }

    public void loadModules() throws IOException {
        Logger.debug("Loading modules...");

        if (!loadedModules.isEmpty()) {
            Logger.debug("Unloading loaded modules...");
            unloadModules();
        }

        File modulesFolder = new File(ModularBot.Constants.PATH_FOLDER_MODULES);

        if (!modulesFolder.exists()) {
            if (!modulesFolder.mkdirs()) {
                throw new IOException("Could not create all necessary folders for path " + modulesFolder.getPath() + "!");
            }
        }

        File[] files = modulesFolder.listFiles();

        if (files == null) {
            throw new IOException("Could not list files in folder in path " + modulesFolder.getPath() + "!");
        }

        JarClassLoader jcl = new JarClassLoader();
        JclObjectFactory jclObjectFactory = JclObjectFactory.getInstance();

        long start = System.currentTimeMillis();

        for (File file : files) {
            Logger.debug("Loading file/module " + file.getName() + "...");
            jcl.add(file.getAbsolutePath());

            zip_file_read: try (ZipFile zipFile = new ZipFile(file)) {
                Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();

                while (zipEntries.hasMoreElements()) {
                    ZipEntry zipEntry = zipEntries.nextElement();
                    String fileName = zipEntry.getName();

                    if (ModularBot.Constants.FILE_NAME_MODULE_INFO.equals(fileName)) {
                        InputStream inputStream = zipFile.getInputStream(zipEntry);

                        try {
                            String fileContent = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
                            ModuleInfo moduleInfo = ModuleInfo.loadFromJsonObject(JsonParser.parseString(fileContent).getAsJsonObject());

                            try {
                                BaseModule baseModule = (BaseModule) jclObjectFactory.create(jcl, moduleInfo.mainClass());
                                baseModule.setModuleInfo(moduleInfo);

                                loadModule(baseModule);
                                break zip_file_read;
                            } catch (Exception exception) {
                                throw new RuntimeException("Exception occurred while loading Main Class for file/module " + file.getName() + "!", exception);
                            }
                        } catch (Exception exception) {
                            throw new IOException("Exception occurred while reading " + fileName + " contents within file/module " + file.getName() + "!", exception);
                        }
                    }
                }

                Logger.error("File " + file.getName() + " does not contain module_info.json file! Cannot load this file/module.");
            }
        }

        Logger.success("Loaded " + loadedModules.size() + " modules in " + (System.currentTimeMillis() - start) + "ms!");
    }

    public void unloadModules() {
        if (loadedModules.isEmpty()) {
            return;
        }

        loadedModules.forEach(this::unloadModule);
    }

    /**
     * Loads module
     * @param baseModule Module
     * @return True if loading was successful, false otherwise
     */
    public boolean loadModule(BaseModule baseModule) {
        String moduleName = baseModule.getModuleInfo().name();

        try {
            Logger.debug("Loading module " + moduleName + "...");
            baseModule.onLoad();

            Logger.debug("Enabling module " + moduleName + "...");
            baseModule.onEnable(); // TODO: Enable až po načtení JDA...

            loadedModules.add(baseModule);
            Logger.info("Module " + moduleName + " was loaded and enabled.");
            return true;
        } catch (Exception exception) {
            Logger.get().error("Exception occurred while loading/enabling module " + moduleName + "! Cannot load this module.", exception);
            return false;
        }
    }

    public void unloadModule(BaseModule baseModule) {
        String moduleName = baseModule.getModuleInfo().name();

        try {
            Logger.debug("Disabling module " + moduleName + "...");
            baseModule.onDisable();

            Logger.debug("Unloading module " + moduleName + "...");
            baseModule.onUnload();
        } catch (Exception exception) {
            Logger.get().error("Exception occurred while disabling/unloading module " + moduleName + "!", exception);
        }

        loadedModules.remove(baseModule);
    }
}
