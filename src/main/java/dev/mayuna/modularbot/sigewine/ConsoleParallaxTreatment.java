package dev.mayuna.modularbot.sigewine;

import dev.mayuna.consoleparallax.ConsoleParallax;
import dev.mayuna.modularbot.console.ModularBotOutputHandler;
import enterprises.iwakura.sigewine.core.annotations.RomaritimeBean;

import java.util.concurrent.Executors;

public class ConsoleParallaxTreatment {

    @RomaritimeBean
    public ConsoleParallax consoleParallax() {
        return ConsoleParallax.builder()
                .setOutputHandler(new ModularBotOutputHandler())
                .setCommandExecutor(Executors.newCachedThreadPool())
                .build();
    }
}
