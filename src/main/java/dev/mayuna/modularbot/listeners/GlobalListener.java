package dev.mayuna.modularbot.listeners;

import dev.mayuna.modularbot.ModularBot;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class GlobalListener extends ListenerAdapter {

    @Override
    public void onGenericEvent(@NotNull GenericEvent event) {
        ModularBot.getGlobalEventBus().post(event);
    }
}
