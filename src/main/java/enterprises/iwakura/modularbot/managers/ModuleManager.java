package enterprises.iwakura.modularbot.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import enterprises.iwakura.amber.Amber;
import enterprises.iwakura.ganyu.Ganyu;
import enterprises.iwakura.modularbot.ModularBot;
import enterprises.iwakura.modularbot.ModularBotConfig;
import enterprises.iwakura.modularbot.ModularBotConstants;
import enterprises.iwakura.modularbot.amber.ModuleAmberLogger;
import enterprises.iwakura.modularbot.base.Module;
import enterprises.iwakura.modularbot.classloader.ModuleClassLoader;
import enterprises.iwakura.modularbot.config.ModuleConfig;
import enterprises.iwakura.modularbot.irminsul.ModularBotIrminsul;
import enterprises.iwakura.modularbot.objects.ModuleInfo;
import enterprises.iwakura.modularbot.objects.ModuleStatus;
import enterprises.iwakura.modularbot.util.InputStreamUtils;
import enterprises.iwakura.sigewine.core.BeanDefinition;
import enterprises.iwakura.sigewine.core.annotations.RomaritimeBean;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import org.apache.commons.collections4.ListUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

@SuppressWarnings("LombokGetterMayBeUsed")
@RomaritimeBean
@Log4j2
public final class ModuleManager {

    private final ModularBotIrminsul irminsul;
    private final ModularBotConfig modularBotConfig;

    private final List<ClassLoader> moduleClassLoaders = Collections.synchronizedList(new LinkedList<>());
    private final List<Module<?>> modules = Collections.synchronizedList(new LinkedList<>());

    public ModuleManager(ModularBotIrminsul irminsul, ModularBotConfig modularBotConfig) {
        this.irminsul = irminsul;
        this.modularBotConfig = modularBotConfig;
    }

    /**
     * Returns list of loaded modules in memory.
     *
     * @return List of modules
     */
    public List<Module<?>> getModules() {
        return modules;
    }

    /**
     * Returns module by its name, if loaded.
     *
     * @param name Name of the module
     *
     * @return Optional of {@link Module}
     */
    public Optional<Module<?>> getModuleByName(String name) {
        return modules.stream().filter(module -> module.getModuleInfo().getName().equalsIgnoreCase(name)).findFirst();
    }

    /**
     * Loads all modules from the modules directories.
     *
     * @return True if modules were loaded successfully, false otherwise.
     */
    public boolean loadModules() {
        log.info("Loading modules...");

        if (!modules.isEmpty()) {
            log.warn("Some modules are loaded - unloading them...");
            unloadModules();
        }

        List<Path> loadedModuleDirectories = new ArrayList<>();
        var directories = ListUtils.emptyIfNull(modularBotConfig.getModules().getModuleDirectories());

        for (String directory : directories) {
            Path moduleDirectoryPath = Path.of(directory);

            if (Files.exists(moduleDirectoryPath) && Files.isDirectory(moduleDirectoryPath)) {
                if (loadedModuleDirectories.contains(moduleDirectoryPath)) {
                    log.warn("Module directory {} is already loaded, skipping...", moduleDirectoryPath);
                    continue;
                }

                loadedModuleDirectories.add(moduleDirectoryPath);
            } else {
                log.warn("Module directory {} does not exist or is not a directory, skipping...", moduleDirectoryPath);
            }
        }

        List<Path> moduleFiles = new ArrayList<>();

        for (Path moduleDirectory : loadedModuleDirectories) {
            if (!Files.exists(moduleDirectory)) {
                try {
                    Files.createDirectories(moduleDirectory);
                } catch (IOException exception) {
                    log.error("Failed to create modules directory!");
                    return false;
                }

                log.warn("The modules directory was just created, there won't be any modules.");
                return true;
            }

            try (Stream<Path> paths = Files.walk(moduleDirectory, 1)) {
                moduleFiles.addAll(paths
                        .filter(Files::isRegularFile) // Only files
                        .filter(path -> path.getFileName().toString().endsWith(".jar")) // Only jar files
                        .toList()
                );
            } catch (IOException exception) {
                log.error("Failed to list files in modules directory!", exception);
                return false;
            }
        }

        List<Class<?>> irminsulEntities = new ArrayList<>();

        for (Path moduleFile : moduleFiles) {
            Optional<Module<?>> optionalModule = loadModuleFile(moduleFile);

            // Could not load module, error logged
            if (optionalModule.isEmpty()) {
                continue;
            }

            Module<?> module = optionalModule.get();

            // Load the module
            loadModule(module);

            // Collect any irminsul entities
            if (module.getIrminsulEntities() != null) {
                irminsulEntities.addAll(module.getIrminsulEntities());
            }
        }

        if (!irminsulEntities.isEmpty()) {
            log.info("Initializing {} Irminsul entities...", irminsulEntities.size());
            irminsul.initialize(irminsulEntities.toArray(new Class[0]));
        }

        return true;
    }

    /**
     * Loads {@link Module} from specified {@link Path}
     *
     * @param moduleFile {@link Path} to the module file
     *
     * @return Optional of {@link Module}
     */
    private Optional<Module<?>> loadModuleFile(Path moduleFile) {
        log.info("Loading module: {}", moduleFile.getFileName());
        ModuleClassLoader moduleClassLoader;

        log.info("Bootstrapping module with Amber...");
        Amber amber = Amber.jarFiles(List.of(moduleFile), new ModuleAmberLogger());
        List<Path> moduleJarDependencies = new ArrayList<>();
        moduleJarDependencies.add(moduleFile);

        try {
            moduleJarDependencies.addAll(amber.bootstrap());
        } catch (Exception exception) {
            log.error("Failed to bootstrap module: {}", moduleFile.getFileName(), exception);
            return Optional.empty();
        }

        try {
            moduleClassLoader = new ModuleClassLoader(moduleJarDependencies, ModuleManager.class.getClassLoader(), moduleClassLoaders);
        } catch (MalformedURLException exception) {
            log.error("Failed to create class loader for module: {}", moduleFile.getFileName(), exception);
            return Optional.empty();
        }

        try (ZipFile zipFile = new ZipFile(moduleFile.toFile())) {
            InputStream moduleInfoInputStream = InputStreamUtils.openFileAsInputStream(zipFile, ModularBotConstants.FILE_NAME_MODULE_INFO);

            if (moduleInfoInputStream == null) {
                log.warn("Module {} does not contain module_info.json! However, it will be loaded in classpath.", moduleFile.getFileName());
                return Optional.empty();
            }

            String moduleInfoFileContent = InputStreamUtils.readStreamAsString(moduleInfoInputStream);
            ModuleInfo moduleInfo = ModuleInfo.loadFromJsonObject(JsonParser.parseString(moduleInfoFileContent).getAsJsonObject());

            Class<?> moduleConfigClass = null;
            ModuleConfig moduleConfig = null;
            if (moduleInfo.getConfigClass() != null) {
                moduleConfigClass = moduleClassLoader.loadClass(moduleInfo.getConfigClass());

                if (!ModuleConfig.class.isAssignableFrom(moduleConfigClass)) {
                    log.error("Module {} specified config class {} which does not extend ModuleConfig!", moduleInfo.getName(), moduleInfo.getConfigClass());
                    return Optional.empty();
                }

                log.info("Loading configuration for module {}...", moduleInfo.getName());
                moduleConfig = (ModuleConfig) moduleConfigClass.getConstructor(ModuleInfo.class, String.class).newInstance(moduleInfo, ModularBotConstants.PATH_FOLDER_MODULES.toString());
                moduleConfig.register();
                moduleConfig.copyResourceConfigs(moduleClassLoader);
            } else {
                log.warn("Module {} does not specify a config class, proceeding without configuration...", moduleInfo.getName());
            }

            Module<?> module;

            // Load module with sigewine
            if (moduleInfo.isSigewineRequired()) {
                var mainClass = moduleClassLoader.loadClass(moduleInfo.getMainClass());

                if (moduleConfigClass != null) {
                    final var moduleConfigBeanName = moduleConfigClass.getSimpleName();
                    log.debug("Adding module config class {} to sigewine", moduleConfigClass);
                    // FIXME: Better way to create bean definition
                    final var beanDefinition = new BeanDefinition(moduleConfigBeanName, moduleConfigClass, null);
                    ModularBot.getSigewine().getSingletonBeans().put(beanDefinition, moduleConfig);
                }

                final var modulePackagePath = Optional.ofNullable(moduleInfo.getSigewinePackagePath()).orElse(mainClass.getPackageName());
                log.info("Module {} requires Sigewine, treating its package {} (class loader {})...", moduleInfo.getName(), modulePackagePath, mainClass.getClassLoader());
                ModularBot.getSigewine().treatment(modulePackagePath, moduleClassLoader);

                log.info("Syringing main class {} for module {}...", mainClass.getCanonicalName(), moduleInfo.getName());
                module = (Module<?>) ModularBot.getSigewine().syringe(mainClass);
            } else {
                // Just create new instance of the module, w/o sigewine
                module = (Module<?>) moduleClassLoader.loadClass(moduleInfo.getMainClass()).getConstructor().newInstance();
            }

            module.setModuleInfo(moduleInfo);
            module.setModuleStatus(ModuleStatus.NOT_LOADED);
            module.setModuleConfig(moduleConfig);

            // Add the module's class loader to the list of class loaders
            synchronized (moduleClassLoaders) {
                moduleClassLoaders.add(moduleClassLoader);
            }

            return Optional.of(module);
        } catch (IOException exception) {
            log.error("Failed to read module: {}", moduleFile.getFileName(), exception);
        } catch (ClassNotFoundException exception) {
            log.error("Could not find main class for module: {}", moduleFile.getFileName(), exception);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException exception) {
            log.error("Could not create module instance for module: {} (does the main class have public no-args constructor?)", moduleFile.getFileName(), exception);
        } catch (Throwable exception) {
            log.error("Failed to load module: {}", moduleFile.getFileName(), exception);
        }

        return Optional.empty();
    }

    /**
     * Loads specified module into memory.
     *
     * @param module Module to load
     */
    public void loadModule(Module<?> module) {
        String moduleName = module.getModuleInfo().getName();

        if (module.getModuleStatus() != ModuleStatus.NOT_LOADED) {
            log.warn("Tried loading module {}, which does not have status of NOT_LOADED!", moduleName);
            return;
        }

        log.info("Loading module {}...", moduleName);
        module.setModuleStatus(ModuleStatus.LOADING);

        try {
            module.onLoad();
        } catch (Exception exception) {
            log.error("Exception occurred while loading module {}!", moduleName, exception);
            module.setModuleStatus(ModuleStatus.FAILED);

            // Remove the module's class loader from the list of class loaders
            synchronized (moduleClassLoaders) {
                moduleClassLoaders.remove(module.getClass().getClassLoader());
            }
            return;
        }

        log.info("Module {} loaded successfully.", moduleName);
        module.setModuleStatus(ModuleStatus.LOADED);
        modules.add(module);
    }

    /**
     * Enables all loaded modules in memory.
     */
    public void enableModules() {
        log.info("Enabling {} modules...", modules.size());

        modules.forEach(module -> {
            try {
                enableModule(module);
            } catch (StackOverflowError stackOverflowError) {
                String moduleName = module.getModuleInfo().getName();
                String depend = Arrays.toString(module.getModuleInfo().getDepend());
                String softDepend = Arrays.toString(module.getModuleInfo().getSoftDepend());

                log.error("StackOverflowError occurred while loading module {}! It depends on {}, soft-depends on {}", moduleName, depend, softDepend);
                unloadModule(module);
            }
        });

        log.debug("Unloading modules that failed to enable, if any...");
        modules.forEach(module -> {
            if (module.getModuleStatus() != ModuleStatus.ENABLED) {
                unloadModule(module);
            }
        });

        log.info("Enabled {} modules successfully.", modules.size());
    }

    /**
     * Enables specified module.
     *
     * @param module Module to enable
     */
    public void enableModule(Module<?> module) {
        if (module.getModuleStatus() == ModuleStatus.ENABLED) {
            return;
        }

        ModuleInfo moduleInfo = module.getModuleInfo();

        // Depend
        for (String dependentName : moduleInfo.getDepend()) {
            Optional<Module<?>> optionalDependentModule = getModuleByName(dependentName);

            if (optionalDependentModule.isEmpty()) {
                log.error("Module {} specified {} as dependent but the module is not loaded!", moduleInfo.getName(), dependentName);
                return;
            }

            enableModule(optionalDependentModule.get());
        }

        // Soft-depend
        for (String dependentModule : moduleInfo.getSoftDepend()) {
            Optional<Module<?>> optionalDependentModule = getModuleByName(dependentModule);

            if (optionalDependentModule.isEmpty()) {
                log.warn("Module {} specified {} as soft-dependent but the module is not loaded.", moduleInfo.getName(), dependentModule);
                continue;
            }

            enableModule(optionalDependentModule.get());
        }

        log.info("Enabling module {}...", moduleInfo.getName());
        module.setModuleStatus(ModuleStatus.ENABLING);

        try {
            module.onEnable();
        } catch (Exception exception) {
            log.error("Failed to enable module {}!", moduleInfo.getName(), exception);
            unloadModule(module);
            return;
        }

        log.info("Module {} enabled successfully.", moduleInfo.getName());
        module.setModuleStatus(ModuleStatus.ENABLED);
    }

    /**
     * Unloads all loaded modules in memory.
     */
    public void unloadModules() {
        if (modules.isEmpty()) {
            return;
        }

        modules.forEach(this::unloadModule);

        log.info("Unloaded {} modules successfully.", modules.size());
    }

    /**
     * Unloads specified module from memory.
     *
     * @param module Module to unload
     */
    public void unloadModule(Module<?> module) {
        String moduleName = module.getModuleInfo().getName();

        switch (module.getModuleStatus()) {
            case NOT_LOADED -> log.warn("Tried unloading module ({}) which is not loaded!", moduleName);
            case LOADED, ENABLING, DISABLED -> {
                log.info("Unloading module {}...", moduleName);
                module.setModuleStatus(ModuleStatus.UNLOADING);

                try {
                    module.onUnload();
                } catch (Exception unloadException) {
                    log.error("Exception occurred while unloading module {}!", moduleName, unloadException);
                }

                module.setModuleStatus(ModuleStatus.NOT_LOADED);

                log.info("Module {} unloaded successfully.", moduleName);
            }
            case ENABLED -> {
                log.info("Disabling module {}...", moduleName);
                module.setModuleStatus(ModuleStatus.DISABLING);

                try {
                    module.onDisable();
                } catch (Exception disableException) {
                    log.error("Exception occurred while disabling module {}!", moduleName, disableException);
                }

                module.setModuleStatus(ModuleStatus.DISABLED);
                log.info("Module {} disabled successfully.", moduleName);

                unloadModule(module);
            }
        }
    }

    /**
     * Processes all modules with specified {@link CommandClientBuilder}
     *
     * @param commandClientBuilder Non-null {@link CommandClientBuilder}
     */
    public void processCommandClientBuilder(CommandClientBuilder commandClientBuilder) {
        modules.forEach(module -> module.onCommandClientBuilderInitialization(commandClientBuilder));
    }

    /**
     * Processes all modules with specified {@link Ganyu}
     *
     * @param ganyu Non-null {@link Ganyu}
     */
    public void processGanyu(Ganyu ganyu) {
        modules.forEach(module -> module.onConsoleCommandRegistration(ganyu));
    }

    /**
     * Processes all modules with specified {@link DefaultShardManagerBuilder}
     *
     * @param shardManagerBuilder Non-null {@link DefaultShardManagerBuilder}
     */
    public void processShardBuilder(DefaultShardManagerBuilder shardManagerBuilder) {
        modules.forEach(module -> module.onShardManagerBuilderInitialization(shardManagerBuilder));
    }

    /**
     * Processes all modules with specified {@link Throwable} based on their exception handling packages
     *
     * @param throwable Non-null {@link Throwable}
     */
    public void processException(Throwable throwable) {
        modules.forEach(module -> {
            try {
                for (var stackTraceElement : throwable.getStackTrace()) {
                    for (String packageName : module.getModuleInfo().getExceptionHandlingPackages()) {
                        if (stackTraceElement.getClassName().contains(packageName)) {
                            module.onUncaughtException(throwable);
                            return;
                        }
                    }
                }
            } catch (Exception exception) {
                log.error("Exception occurred while processing modules with uncaught exception!", exception);
            }
        });
    }
}
