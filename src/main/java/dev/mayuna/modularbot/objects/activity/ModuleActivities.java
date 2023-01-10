package dev.mayuna.modularbot.objects.activity;

import dev.mayuna.modularbot.objects.Module;
import lombok.Getter;
import lombok.NonNull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

import java.util.*;
import java.util.function.Function;

public class ModuleActivities {

    private final @Getter List<ModularActivity> activities = Collections.synchronizedList(new LinkedList<>());
    private final Module module;

    public ModuleActivities(Module module) {
        this.module = module;
    }

    /**
     * Adds {@link Activity} to internal list that Modular Bot will use
     *
     * @param name              Non-null activity name
     * @param onActivityRefresh Non-null {@link Function} with {@link JDA} (shard) as an argument and {@link Activity} as a return value
     */
    public void addActivity(@NonNull String name, @NonNull Function<JDA, Activity> onActivityRefresh) {
        activities.add(new ModularActivity(module, name, onActivityRefresh));
    }

    /**
     * Removes all {@link Activity} by their name from internal map.
     *
     * @param name Non-null activity name
     *
     * @return True if anything was removed from the internal list
     */
    public boolean removeActivity(@NonNull String name) {
        synchronized (activities) {
            Iterator<ModularActivity> iterator = activities.listIterator();

            boolean removedSomething = false;
            
            while (iterator.hasNext()) {
                ModularActivity activity = iterator.next();

                if (activity.getName().equals(name)) {
                    iterator.remove();
                    removedSomething = true;
                }
            }

            return removedSomething;
        }
    }
}
