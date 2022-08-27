package dev.mayuna.modularbot.utils;

import dev.mayuna.modularbot.ModularBot;
import dev.mayuna.modularbot.managers.DataManager;
import dev.mayuna.pumpk1n.api.ClassGetter;
import org.xeustechnologies.jcl.JarClassLoader;

public class CustomJarClassLoader extends JarClassLoader {

    public static void processDataManager(DataManager dataManager) {
        dataManager.getClassHelper().registerNewClassGetter(new ClassGetter() {
            @Override
            public Class<?> getClass(String className) {
                try {
                    return ModularBot.getModuleManager().getJarClassLoader().loadClass(className, true);
                } catch (Exception ignored) {
                    return null;
                }
            }
        });
    }

    @Override
    public Class loadClass(String className, boolean resolveIt) throws ClassNotFoundException {
        try {
            return Class.forName(className);
        } catch (Exception ignored) {
        }

        return super.loadClass(className, resolveIt);
    }
}
