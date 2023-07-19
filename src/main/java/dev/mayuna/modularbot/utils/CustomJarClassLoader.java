package dev.mayuna.modularbot.utils;

import dev.mayuna.modularbot.ModularBot;
import org.xeustechnologies.jcl.JarClassLoader;

import java.io.InputStream;

public class CustomJarClassLoader extends JarClassLoader {

    @Override
    public Class loadClass(String className, boolean resolveIt) throws ClassNotFoundException {
        try {
            return Class.forName(className);
        } catch (Exception ignored) {
        }

        return super.loadClass(className, resolveIt);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream inputStream = ModularBot.class.getResourceAsStream(name);

        if (inputStream != null) {
            return inputStream;
        }

        return super.getResourceAsStream(name);
    }
}
