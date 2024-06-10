package dev.mayuna.modularbot.classloaders;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

/**
 * {@link dev.mayuna.modularbot.base.Module}'s class loader
 */
public final class ModuleClassLoader extends URLClassLoader {

    private final List<ModuleClassLoader> otherClassLoaders;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    /**
     * Creates new class loader for specified jar file with specified {@link ClassLoader} as parent
     *
     * @param jarFile Jar File
     * @param parent  Parent {@link ClassLoader}
     *
     * @throws MalformedURLException If the jar file could not be converted to URL
     */
    public ModuleClassLoader(Path jarFile, ClassLoader parent, List<ModuleClassLoader> otherClassLoaders) throws MalformedURLException {
        super(new URL[] {jarFile.toUri().toURL()}, parent);
        this.otherClassLoaders = otherClassLoaders;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // If the class is loaded by this class loader, e.g., it is module's class, return it
        try {
            Class<?> result = super.loadClass(name, resolve);

            if (result.getClassLoader() == this) {
                return result;
            }
        } catch (ClassNotFoundException ignored) {
        }

        // Load other module's class
        synchronized (otherClassLoaders) {
            for (ModuleClassLoader otherClassLoader : otherClassLoaders) {
                if (otherClassLoader == this) {
                    continue;
                }

                try {
                    return otherClassLoader.loadClass(name, resolve);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }

        return getParent().loadClass(name);
    }
}
