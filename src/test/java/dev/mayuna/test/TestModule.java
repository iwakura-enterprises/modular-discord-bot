package dev.mayuna.test;

import dev.mayuna.modularbot.base.Module;

public class TestModule extends Module {

    @Override
    public void onEnable() {
        getModuleScheduler().runAsync();
    }

    @Override
    public void onDisable() {

    }
}
