package dev.mayuna.modularbot.objects.activity;

import dev.mayuna.modularbot.base.Module;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

import java.util.function.Function;

/**
 * Module's presence activity
 */
public class ModuleActivity {

    private final @Getter Module module;
    private final @Getter String name;
    private final @Getter Function<JDA, Activity> onActivityRefresh;

    /**
     * Creates new {@link ModuleActivity}
     *
     * @param module            Non-null module
     * @param name              Non-null activity name
     * @param onActivityRefresh Non-null function which will be invoked on all shards and shall return an {@link Activity}
     */
    ModuleActivity(@NonNull Module module, @NonNull String name, @NonNull Function<JDA, Activity> onActivityRefresh) {
        this.module = module;
        this.name = name;
        this.onActivityRefresh = onActivityRefresh;
    }
}
