package dev.mayuna.modularbot;

import lombok.extern.log4j.Log4j2;

@Log4j2
public final class Main {

    private static ModularBot modularBot;

    public static void main(String[] args) {
        log.info("Bootstrapping ModularBot...");

        log.info("Initializing Sigewine...");
        final var sigewine = ModularBot.getSigewine();
        sigewine.treatment(Main.class);
        log.info("Sigewine initialized with {} beans", ModularBot.getSigewine().getSingletonBeans().size());

        log.info("Getting ModularBot bean...");
        modularBot = sigewine.syringe(ModularBot.class);

        log.info("Starting ModularBot...");
        modularBot.start(args);
    }
}
