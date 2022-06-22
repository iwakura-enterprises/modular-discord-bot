package dev.mayuna.modularbot.objects;

import lombok.Getter;
import lombok.Setter;

public abstract class BaseModule {

    private @Getter @Setter ModuleInfo moduleInfo;

    public void onLoad() {
        // Empty
    }

    public abstract void onEnable();

    public abstract void onDisable();

    public void onUnload() {
        // Empty
    }
}
