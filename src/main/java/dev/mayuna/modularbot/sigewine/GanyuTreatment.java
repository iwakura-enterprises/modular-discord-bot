package dev.mayuna.modularbot.sigewine;

import dev.mayuna.modularbot.console.ModularBotGanyuOutput;
import dev.mayuna.modularbot.console.ModularConsoleCommand;
import dev.mayuna.modularbot.console.StopConsoleCommand;
import enterprises.iwakura.ganyu.Ganyu;
import enterprises.iwakura.ganyu.impl.ConsoleInput;
import enterprises.iwakura.sigewine.core.annotations.RomaritimeBean;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
public class GanyuTreatment {

    private final static Executor EXECUTOR = Executors.newCachedThreadPool();

    private final StopConsoleCommand stopConsoleCommand;
    private final ModularConsoleCommand modularConsoleCommand;

    @RomaritimeBean
    public Ganyu ganyu() {
        var ganyu = Ganyu.standardWithExecutor(new ConsoleInput(), new ModularBotGanyuOutput(), EXECUTOR);
        ganyu.registerCommands(
                stopConsoleCommand,
                modularConsoleCommand
        );
        ganyu.run();
        return ganyu;
    }
}
