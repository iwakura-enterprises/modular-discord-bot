package dev.mayuna.modularbot.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mayuna.mayusjdautils.MayusJDAUtilities;
import dev.mayuna.modularbot.ModularBot;
import dev.mayuna.modularbot.ModularBotConstants;
import dev.mayuna.modularbot.amber.ModuleAmberLogger;
import dev.mayuna.modularbot.base.Module;
import dev.mayuna.modularbot.base.ModuleManager;
import dev.mayuna.modularbot.classloader.ModuleClassLoader;
import dev.mayuna.modularbot.concurrent.ModuleScheduler;
import dev.mayuna.modularbot.irminsul.ModularBotIrminsul;
import dev.mayuna.modularbot.objects.ModuleConfig;
import dev.mayuna.modularbot.objects.ModuleInfo;
import dev.mayuna.modularbot.objects.ModuleStatus;
import dev.mayuna.modularbot.util.InputStreamUtils;
import enterprises.iwakura.amber.Amber;
import enterprises.iwakura.sigewine.core.BeanDefinition;
import enterprises.iwakura.sigewine.core.annotations.RomaritimeBean;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

@RomaritimeBean
@Log4j2
public final class DefaultModuleManager implements ModuleManager {

    public static final String MODULE_CONFIG_BEAN_NAME = "moduleConfig_%s";

    private final MayusJDAUtilities baseMayusJDAUtilities;
    private final ModularBotIrminsul irminsul;

    private final List<ClassLoader> moduleClassLoaders = Collections.synchronizedList(new LinkedList<>());
    private List<Module> modules = createModuleList();
    private List<Module> internalModules = createModuleList();

    private boolean stateLoaded = false;
    private boolean stateEnabled = false;
    private boolean stateUnloaded = false;

    public DefaultModuleManager(
            @RomaritimeBean(name = "modularBotMayusJDAUtilities") MayusJDAUtilities baseMayusJDAUtilities,
            ModularBotIrminsul irminsul
    ) {
        this.baseMayusJDAUtilities = baseMayusJDAUtilities;
        this.irminsul = irminsul;
    }

    /**
     * Creates empty list for modules
     *
     * @return List of {@link Module}
     */
    private List<Module> createModuleList() {
        return Collections.synchronizedList(new LinkedList<>());
    }

    /**
     * Returns list of loaded modules in memory.
     *
     * @return List of modules
     */
    @Override
    public List<Module> getModules() {
        return modules;
    }

    /**
     * Adds internal module(s) to memory. If not loaded, they will be loaded and enabled.
     *
     * @param internalModules Module(s) to add
     */
    @Override
    public void addInternalModules(@NonNull Module... internalModules) {
        if (stateUnloaded) {
            throw new IllegalStateException("Cannot add internal modules after unloading!");
        }

        // Prepare modules
        var listModules = List.of(internalModules);
        listModules.forEach(module -> {
            module.setModuleStatus(ModuleStatus.NOT_LOADED);
            module.setModuleConfig(new ModuleConfig(module.getModuleInfo(), new JsonObject()));
            module.setModuleScheduler(new ModuleScheduler(module));

            var mayusJdaUtilities = new MayusJDAUtilities();
            mayusJdaUtilities.copyFrom(baseMayusJDAUtilities);
            module.setMayusJDAUtilities(mayusJdaUtilities);

            // Add the module's class loader to the list of class loaders
            synchronized (moduleClassLoaders) {
                var moduleClassLoader = module.getClass().getClassLoader();

                if (!moduleClassLoaders.contains(moduleClassLoader)) {
                    moduleClassLoaders.add(moduleClassLoader);
                }
            }
        });

        // Loading did not start yet
        if (!stateLoaded) {
            log.info("Adding {} internal module(s) to memory to be loaded", listModules.size());
            this.internalModules.addAll(listModules);
            return;
        }

        // Loading started, but not enabled -> load them
        if (!stateEnabled) {
            log.info("Loading {} internal module(s) & adding them to memory to be enabled", listModules.size());
            listModules.forEach(this::loadModule);
            return;
        }

        // Loading & enabling started -> enable them
        log.info("Loading & enabling {} internal module(s) & adding them to memory", listModules.size());
        listModules.forEach(this::loadModule);
        listModules.forEach(this::enableModule);
    }

    @Override
    public boolean loadModules() {
        log.info("Loading modules...");

        if (!modules.isEmpty()) {
            log.warn("Some modules are loaded - unloading them...");
            unloadModules();
        }

        Path modulesDirectory = ModularBotConstants.PATH_FOLDER_MODULES;

        if (!Files.exists(modulesDirectory)) {
            try {
                Files.createDirectories(modulesDirectory);
            } catch (IOException exception) {
                log.error("Failed to create modules directory!");
                return false;
            }

            log.warn("The modules directory was just created, there won't be any modules.");
            return true;
        }

        List<Path> moduleFiles;

        try (Stream<Path> paths = Files.walk(modulesDirectory, 1)) {
            moduleFiles = paths
                    .filter(Files::isRegularFile) // Only files
                    .filter(path -> path.getFileName().toString().endsWith(".jar")) // Only jar files
                    .toList();
        } catch (IOException exception) {
            log.error("Failed to list files in modules directory!", exception);
            return false;
        }

        List<Class<?>> irminsulEntities = new ArrayList<>();

        for (Path moduleFile : moduleFiles) {
            Optional<Module> optionalModule = loadModuleFile(moduleFile);

            // Could not load module, error logged
            if (optionalModule.isEmpty()) {
                continue;
            }

            Module module = optionalModule.get();

            // Load the module
            loadModule(module);

            // Collect any irminsul entities
            if (module.getIrminsulEntities() != null) {
                irminsulEntities.addAll(module.getIrminsulEntities());
            }
        }

        // Loading any internal modules
        log.info("Loading {} internal modules...", internalModules.size());
        internalModules.forEach(module -> {
            loadModule(module);

            // Collect any irminsul entities
            if (module.getIrminsulEntities() != null) {
                irminsulEntities.addAll(module.getIrminsulEntities());
            }
        });
        internalModules.clear();

        if (!irminsulEntities.isEmpty()) {
            log.info("Initializing {} Irminsul entities...", irminsulEntities.size());
            irminsul.initialize(irminsulEntities.toArray(new Class[0]));
        }


        stateLoaded = true;
        return true;
    }

    /**
     * Loads {@link Module} from specified {@link Path}
     *
     * @param moduleFile {@link Path} to the module file
     *
     * @return Optional of {@link Module}
     */
    private Optional<Module> loadModuleFile(Path moduleFile) {
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
            moduleClassLoader = new ModuleClassLoader(moduleJarDependencies, DefaultModuleManager.class.getClassLoader(), moduleClassLoaders);
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

            JsonObject defaultConfig;
            InputStream defaultConfigStream = InputStreamUtils.openFileAsInputStream(zipFile, ModularBotConstants.FILE_NAME_MODULE_CONFIG);

            if (defaultConfigStream != null) {
                String defaultConfigFileContent = InputStreamUtils.readStreamAsString(defaultConfigStream);

                defaultConfig = JsonParser.parseString(defaultConfigFileContent).getAsJsonObject();
            } else {
                log.warn("Module {} does not have default config.", moduleInfo.getName());
                defaultConfig = new JsonObject();
            }

            final var moduleConfig = new ModuleConfig(moduleInfo, defaultConfig);
            log.info("Loading configuration for module {}...", moduleInfo.getName());
            moduleConfig.copyDefaultsIfEmpty();
            moduleConfig.reload();

            Module module;

            if (moduleInfo.isSigewineRequired()) {
                var mainClass = moduleClassLoader.loadClass(moduleInfo.getMainClass());

                final var moduleConfigBeanName = MODULE_CONFIG_BEAN_NAME.formatted(moduleInfo.getName());
                log.debug("Adding module config to bean as {}: {}", moduleConfigBeanName, moduleConfig);
                // FIXME: Better way to create bean definition
                final var beanDefinition = new BeanDefinition(moduleConfigBeanName, moduleConfig.getClass(), null);
                ModularBot.getSigewine().getSingletonBeans().put(beanDefinition, moduleConfig);

                final var modulePackagePath = Optional.ofNullable(moduleInfo.getSigewinePackagePath()).orElse(mainClass.getPackageName());
                log.info("Module {} requires Sigewine, treating its package {} (class loader {})...", moduleInfo.getName(), modulePackagePath, mainClass.getClassLoader());
                ModularBot.getSigewine().treatment(modulePackagePath, moduleClassLoader);

                log.info("Syringing main class {} for module {}...", mainClass.getCanonicalName(), moduleInfo.getName());
                module = (Module) ModularBot.getSigewine().syringe(mainClass);
            } else {
                module = (Module) moduleClassLoader.loadClass(moduleInfo.getMainClass()).getConstructor().newInstance();
            }

            module.setModuleInfo(moduleInfo);
            module.setModuleStatus(ModuleStatus.NOT_LOADED);
            module.setModuleConfig(moduleConfig);
            module.setModuleScheduler(new ModuleScheduler(module));

            var mayusJdaUtilities = new MayusJDAUtilities();
            mayusJdaUtilities.copyFrom(baseMayusJDAUtilities);
            module.setMayusJDAUtilities(mayusJdaUtilities);

            // Add the module's class loader to the list of class loaders
            synchronized (moduleClassLoaders) {
                moduleClassLoaders.add(moduleClassLoader);
            }

            // Copy default config, if empty
            module.getModuleConfig().copyDefaultsIfEmpty();

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

    @Override
    public void loadModule(Module module) {
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

    @Override
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

        stateEnabled = true;
        log.info("Enabled {} modules successfully.", modules.size());
    }

    @Override
    public void enableModule(Module module) {
        if (module.getModuleStatus() == ModuleStatus.ENABLED) {
            return;
        }

        ModuleInfo moduleInfo = module.getModuleInfo();

        // Depend
        for (String dependentName : moduleInfo.getDepend()) {
            Optional<Module> optionalDependentModule = getModuleByName(dependentName);

            if (optionalDependentModule.isEmpty()) {
                log.error("Module {} specified {} as dependent but the module is not loaded!", moduleInfo.getName(), dependentName);
                return;
            }

            enableModule(optionalDependentModule.get());
        }

        // Soft-depend
        for (String dependentModule : moduleInfo.getSoftDepend()) {
            Optional<Module> optionalDependentModule = getModuleByName(dependentModule);

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

    @Override
    public void unloadModules() {
        if (modules.isEmpty()) {
            return;
        }

        // Copy modules to temporary field
        List<Module> oldModules = modules;
        modules = createModuleList();
        internalModules = createModuleList();

        oldModules.forEach(this::unloadModule);

        stateUnloaded = true;
        log.info("Unloaded {} modules successfully.", oldModules.size());
    }

    @Override
    public void unloadModule(Module module) {
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

                modules.remove(module);

                synchronized (moduleClassLoaders) {
                    moduleClassLoaders.remove(module.getClass().getClassLoader());
                }

                module.setModuleStatus(ModuleStatus.NOT_LOADED);

                log.info("Module {} unloaded successfully.", moduleName);
            }
            case ENABLED -> {
                log.info("Disabling module {}...", moduleName);
                module.setModuleStatus(ModuleStatus.DISABLING);

                try {
                    module.onDisable();
                    module.getModuleScheduler().cancelTasks();
                } catch (Exception disableException) {
                    log.error("Exception occurred while disabling module {}!", moduleName, disableException);
                }

                module.setModuleStatus(ModuleStatus.DISABLED);
                log.info("Module {} disabled successfully.", moduleName);

                unloadModule(module);
            }
        }
    }
}
