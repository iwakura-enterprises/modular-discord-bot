package dev.mayuna.modularbot.events;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;

public class ShardStartedEvent implements GenericModularEvent {

    private final @Getter JDA jda;

    public ShardStartedEvent(JDA jda) {
        this.jda = jda;
    }
}
