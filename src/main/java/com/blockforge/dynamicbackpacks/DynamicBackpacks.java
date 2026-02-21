package com.blockforge.dynamicbackpacks;

import org.bukkit.plugin.java.JavaPlugin;

public class DynamicBackpacks extends JavaPlugin {

    private static DynamicBackpacks instance;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("DynamicBackpacks enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("DynamicBackpacks disabled!");
    }

    public static DynamicBackpacks getInstance() {
        return instance;
    }
}
