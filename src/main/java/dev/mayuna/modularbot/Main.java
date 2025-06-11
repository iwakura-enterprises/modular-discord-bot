package dev.mayuna.modularbot;

import dev.mayuna.modularbot.util.logging.ModularBotLogger;

public final class Main {

    private static final ModularBotLogger LOGGER = ModularBotLogger.create("Bootstrap");
    private static ModularBot modularBot;

    public static void main(String[] args) {
        LOGGER.info("Bootstrapping ModularBot...");

        LOGGER.info("Initializing Sigewine...");
        final var sigewine = ModularBot.getSigewine();
        sigewine.treatment(Main.class);
        LOGGER.info("Sigewine initialized with {} beans", ModularBot.getSigewine().getSingletonBeans().size());

        LOGGER.info("Getting ModularBot bean...");
        modularBot = sigewine.syringe(ModularBot.class);

        LOGGER.info("Starting ModularBot...");
        modularBot.start(args);
    }
}
