package enterprises.iwakura.modularbot;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ModularBotMain {

    public static void main(String[] args) {
        log.info("Bootstrapping ModularBot...");

        log.info("Initializing Sigewine...");
        //noinspection deprecation
        var sigewine = ModularBot.getSigewine();
        sigewine.scan(ModularBotMain.class);
        log.info("Sigewine initialized with {} beans", sigewine.getSingletonBeans().size());

        log.info("Getting ModularBot bean...");
        var modularBot = sigewine.inject(ModularBot.class);

        log.info("Starting ModularBot...");
        modularBot.start(args);
    }
}
