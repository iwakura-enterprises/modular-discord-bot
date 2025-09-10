package enterprises.iwakura.modularbot;

import enterprises.iwakura.amber.Amber;
import enterprises.iwakura.amber.BootstrapOptions;

import java.io.IOException;

public class BootstrapMain {

    public static void main(String[] args) throws IOException {
        System.out.println("Bootstrapping Modular Bot's dependencies...");
        Amber amber = Amber.classLoader();
        amber.bootstrap(BootstrapOptions.builder()
                //.exitCodeAfterDownload(-500)
                .build()
        );
        Main.main(args);
    }
}
