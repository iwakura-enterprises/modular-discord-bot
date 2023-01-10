package dev.mayuna.modularbot.objects.activity;

import dev.mayuna.modularbot.objects.Module;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

import java.util.function.Function;

public class ModularActivity {

    private final @Getter Module module;
    private final @Getter String name;
    private final @Getter Function<JDA, Activity> onActivityRefresh;

    public ModularActivity(Module module, String name, Function<JDA, Activity> onActivityRefresh) {
        this.module = module;
        this.name = name;
        this.onActivityRefresh = onActivityRefresh;
    }
}
