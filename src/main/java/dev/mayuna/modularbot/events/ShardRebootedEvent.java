package dev.mayuna.modularbot.events;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;

public class ShardRebootedEvent implements GenericModularEvent {

    private final @Getter JDA jda;

    public ShardRebootedEvent(JDA jda) {
        this.jda = jda;
    }
}
