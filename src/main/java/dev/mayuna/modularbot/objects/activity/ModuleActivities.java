package dev.mayuna.modularbot.objects.activity;

import dev.mayuna.modularbot.base.Module;
import lombok.NonNull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * Manages Module's Presence Activities
 */
public final class ModuleActivities {

    private final List<ModuleActivity> activities = Collections.synchronizedList(new LinkedList<>());
    private final Module module;

    /**
     * Creates new {@link ModuleActivities}
     *
     * @param module Non-null {@link Module}
     */
    public ModuleActivities(@NonNull Module module) {
        this.module = module;
    }

    /**
     * Adds {@link Activity} to internal list that Modular Bot will use
     *
     * @param name              Non-null activity name
     * @param onActivityRefresh Non-null {@link Function} with {@link JDA} (shard) as an argument and {@link Activity} as a return value
     */
    public void createActivity(@NonNull String name, @NonNull Function<JDA, Activity> onActivityRefresh) {
        removeActivity(name);
        activities.add(new ModuleActivity(module, name, onActivityRefresh));
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
            Iterator<ModuleActivity> iterator = activities.listIterator();

            boolean removedSomething = false;

            while (iterator.hasNext()) {
                ModuleActivity activity = iterator.next();

                if (activity.getName().equals(name)) {
                    iterator.remove();
                    removedSomething = true;
                }
            }

            return removedSomething;
        }
    }
}
