package dev.mayuna.modularbot.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import dev.mayuna.modularbot.ModularBot;
import dev.mayuna.modularbot.logging.Logger;
import dev.mayuna.modularbot.logging.MayuLogger;
import dev.mayuna.modularbot.objects.Module;
import dev.mayuna.modularbot.objects.ModuleConfig;
import dev.mayuna.modularbot.objects.ModuleInfo;
import dev.mayuna.modularbot.objects.ModuleStatus;
import dev.mayuna.modularbot.utils.CustomJarClassLoader;
import dev.mayuna.modularbot.utils.ZipUtil;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import org.xeustechnologies.jcl.JclObjectFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

public class ModuleManager {

    private static ModuleManager instance;

    private final @Getter List<Module> modules = Collections.synchronizedList(new LinkedList<>());
    private final @Getter CustomJarClassLoader jarClassLoader = new CustomJarClassLoader();
    private final @Getter JclObjectFactory jclObjectFactory = JclObjectFactory.getInstance();

    private ModuleManager() {
    }

    /**
     * Gets current instance of {@link ModuleManager}
     *
     * @return Non-null {@link ModuleManager}
     */
    public static @NonNull ModuleManager getInstance() {
        if (instance == null) {
            instance = new ModuleManager();
        }

        return instance;
    }

    ///////////////
    // Utilities //
    ///////////////

    /**
     * Determines if the module is loaded (is in {@link #modules} list
     *
     * @param moduleName Module name
     *
     * @return True if the specified module is loaded, false otherwise
     */
    public boolean isModuleLoaded(String moduleName) {
        return getModuleByName(moduleName).orElse(null) != null;
    }

    /////////////
    // Getters //
    /////////////

    /**
     * Gets {@link Module} by name
     *
     * @param moduleName Module name
     *
     * @return {@link Optional} with {@link Module}
     */
    public Optional<Module> getModuleByName(String moduleName) {
        return modules.stream().filter(module -> module.getModuleInfo().name().equalsIgnoreCase(moduleName)).findFirst();
    }

    ///////////////////////////
    // Loading and unloading //
    ///////////////////////////

    /**
     * Loads modules from modules folder
     *
     * @throws IOException This exception is thrown when some exception occurred during the loading
     */
    public void loadModules() throws IOException {
        Logger.debug("Loading modules...");

        if (!modules.isEmpty()) {
            Logger.debug("Unloading loaded modules...");
            unloadModules();
        }

        File modulesFolder = new File(ModularBot.Values.getPathFolderModules());

        if (!modulesFolder.exists()) {
            if (!modulesFolder.mkdirs()) {
                throw new IOException("Could not create all necessary folders for path " + modulesFolder.getPath() + "!");
            }
        }

        File[] files = modulesFolder.listFiles();

        if (files == null) {
            throw new IOException("Could not list files in folder in path " + modulesFolder.getPath() + "!");
        }

        long start = System.currentTimeMillis();

        for (File file : files) {
            if (file.isDirectory()) {
                continue;
            }

            if (!file.getName().endsWith(".jar")) {
                continue;
            }

            Logger.debug("Loading file/module " + file.getName() + "...");
            jarClassLoader.add(file.getAbsolutePath());

            try (ZipFile zipFile = new ZipFile(file)) {
                InputStream moduleInfoStream = ZipUtil.openFileAsInputStream(zipFile, ModularBot.Values.getFileNameModuleInfo());

                if (moduleInfoStream == null) {
                    Logger.error("File " + file.getName() + " does not contain module_info.json file! Cannot load this file/module, however it will be still loaded in classpath.");
                    continue;
                }

                String moduleInfoFileContent = new BufferedReader(new InputStreamReader(moduleInfoStream)).lines().collect(Collectors.joining("\n"));
                ModuleInfo moduleInfo = ModuleInfo.loadFromJsonObject(JsonParser.parseString(moduleInfoFileContent).getAsJsonObject());

                JsonObject defaultConfig = null;
                InputStream defaultConfigStream = ZipUtil.openFileAsInputStream(zipFile, ModularBot.Values.getFileNameModuleConfig());

                if (defaultConfigStream != null) {
                    String defaultConfigFileContent = new BufferedReader(new InputStreamReader(defaultConfigStream)).lines()
                                                                                                                    .collect(Collectors.joining("\n"));

                    defaultConfig = JsonParser.parseString(defaultConfigFileContent).getAsJsonObject();
                }

                try {
                    Module module = (Module) jclObjectFactory.create(jarClassLoader, moduleInfo.mainClass());
                    module.setModuleInfo(moduleInfo);
                    module.setModuleStatus(ModuleStatus.NOT_LOADED);
                    module.setModuleConfig(new ModuleConfig(module, defaultConfig));
                    module.setLogger(MayuLogger.create(module.getModuleInfo().name()));

                    loadModule(module);
                } catch (Exception exception) {
                    throw new RuntimeException("Exception occurred while loading Main Class for file/module " + file.getName() + "!", exception);
                }
            } catch (Exception exception) {
                throw new RuntimeException("There was some error while loading file/module " + file.getName() + "!", exception);
            }
        }

        Logger.success("Loaded " + modules.size() + " modules in " + (System.currentTimeMillis() - start) + "ms!");
    }

    /**
     * Loads a module by calling its onLoad() method.
     *
     * @param module The module to load.
     */
    private synchronized void loadModule(Module module) {
        String moduleName = module.getModuleInfo().name();

        if (module.getModuleStatus() != ModuleStatus.NOT_LOADED) {
            Logger.warn("Tried loading module which is already loaded!");
            return;
        }

        Logger.debug("Loading module " + moduleName + "...");
        module.setModuleStatus(ModuleStatus.LOADING);

        try {
            module.onLoad();
        } catch (Exception loadException) {
            Logger.get().error("Exception occurred while loading module " + moduleName + "! Cannot load this module.", loadException);
            return;
        }

        modules.add(module);
        module.setModuleStatus(ModuleStatus.LOADED);
        Logger.debug("Module " + moduleName + " loaded successfully.");
    }

    public void enableModules() {
        Logger.debug("Enabling " + modules.size() + " modules...");

        long start = System.currentTimeMillis();

        modules.forEach(module -> {
            try {
                enableModule(module);
            } catch (StackOverflowError stackOverflowError) {
                String moduleName = module.getModuleInfo().name();
                String depend = Arrays.toString(module.getModuleInfo().depend());
                String softDepend = Arrays.toString(module.getModuleInfo().softDepend());

                Logger.error("Stack Overflow Error occurred while loading " + moduleName + "! Their depend: " + depend + ", Their soft-depend: " + softDepend + " (Some modules may link to each another with depend or soft-depend field in module_info.json.)");
                unloadModule(module);
            }
        });

        checkNonEnabledModules();

        Logger.success("Enabled " + modules.size() + " modules in " + (System.currentTimeMillis() - start) + "ms!");
    }

    private void checkNonEnabledModules() {
        modules.forEach(module -> {
            if (module.getModuleStatus() != ModuleStatus.ENABLED) {
                unloadModule(module);
            }
        });
    }

    private synchronized void enableModule(Module module) {
        if (module.getModuleStatus() == ModuleStatus.ENABLED) {
            return;
        }

        for (String dependentName : module.getModuleInfo().depend()) {
            Module dependentModule = getModuleByName(dependentName).orElse(null);

            if (dependentModule == null) {
                Logger.error("Cannot enable " + module.getModuleInfo().name() + " since there is no dependent module named " + dependentName + "!");
                return;
            }

            enableModule(dependentModule);
        }

        for (String dependentName : module.getModuleInfo().softDepend()) {
            Module dependentModule = getModuleByName(dependentName).orElse(null);

            if (dependentModule == null) {
                return;
            }

            enableModule(dependentModule);
        }

        Logger.debug("Enabling module " + module.getModuleInfo().name() + "...");
        module.setModuleStatus(ModuleStatus.ENABLING);

        try {
            module.onEnable();
        } catch (Exception enableException) {
            Logger.get().error("Exception occurred while enabling module " + module.getModuleInfo()
                                                                                   .name() + "! Cannot enable this module.", enableException);
            unloadModule(module);
            return;
        }

        module.setModuleStatus(ModuleStatus.ENABLED);
        Logger.debug("Module " + module.getModuleInfo().name() + " enabled successfully.");
    }

    /**
     * Unloads module
     *
     * @param module Base module
     */
    private synchronized void unloadModule(Module module) {
        String moduleName = module.getModuleInfo().name();

        switch (module.getModuleStatus()) {
            case NOT_LOADED -> {
                Logger.warn("Tried unloading module which is not loaded!");
            }
            case LOADED, ENABLING, DISABLED -> {
                Logger.debug("Unloading module " + moduleName + "...");
                module.setModuleStatus(ModuleStatus.UNLOADING);

                try {
                    module.onUnload();
                } catch (Exception unloadException) {
                    Logger.get().error("Exception occurred while unloading module " + moduleName + "!", unloadException);
                }

                modules.remove(module);
                module.setModuleStatus(ModuleStatus.NOT_LOADED);
                Logger.debug("Module " + moduleName + " unloaded successfully.");
            }
            case ENABLED -> {
                Logger.debug("Disabling module " + moduleName + "...");
                module.setModuleStatus(ModuleStatus.DISABLING);

                try {
                    module.onDisable();
                } catch (Exception disableException) {
                    Logger.get().error("Exception occurred while disabling module " + moduleName + "!", disableException);
                }

                module.setModuleStatus(ModuleStatus.DISABLED);
                Logger.debug("Module " + moduleName + " disabled successfully.");
                unloadModule(module);
            }
        }
    }

    /**
     * Unloads all modules
     */
    public void unloadModules() {
        if (modules.isEmpty()) {
            return;
        }

        int size = modules.size();
        long start = System.currentTimeMillis();

        modules.forEach(this::unloadModule);

        Logger.success("Unloaded " + size + " modules in " + (System.currentTimeMillis() - start) + "ms!");
    }

    ////////////////
    // Processing //
    ////////////////

    public void processCommandClientBuilder(CommandClientBuilder commandClientBuilder) {
        modules.forEach(module -> module.onCommandClientBuilderInitialization(commandClientBuilder));
    }

    public void processShardBuilder(DefaultShardManagerBuilder shardManagerBuilder) {
        modules.forEach(module -> module.onShardManagerBuilderInitialization(shardManagerBuilder));
    }

    public void processException(Throwable throwable) {
        modules.forEach(module -> {
            try {
                for (var stackTraceElement : throwable.getStackTrace()) {
                    for (String packageName : module.getModuleInfo().exceptionHandlingPackages()) {
                        if (stackTraceElement.getClassName().contains(packageName)) {
                            module.onUncaughtException(throwable);
                            return;
                        }
                    }
                }
            } catch (Exception exception) {
                Logger.get().error("Exception occurred while processing modules with uncaught exception!", exception);
            }
        });
    }
}
