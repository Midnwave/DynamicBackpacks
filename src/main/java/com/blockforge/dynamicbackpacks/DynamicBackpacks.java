package com.blockforge.dynamicbackpacks;

import com.blockforge.dynamicbackpacks.backpack.BackpackManager;
import com.blockforge.dynamicbackpacks.backpack.BackpackSession;
import com.blockforge.dynamicbackpacks.commands.AdminCommand;
import com.blockforge.dynamicbackpacks.commands.BackpackCommand;
import com.blockforge.dynamicbackpacks.config.ConfigManager;
import com.blockforge.dynamicbackpacks.database.DatabaseManager;
import com.blockforge.dynamicbackpacks.database.SQLiteDatabase;
import com.blockforge.dynamicbackpacks.listeners.BackpackInteractListener;
import com.blockforge.dynamicbackpacks.listeners.BackpackInventoryListener;
import com.blockforge.dynamicbackpacks.listeners.BackpackProtectListener;
import com.blockforge.dynamicbackpacks.recipe.RecipeManager;
import org.bukkit.plugin.java.JavaPlugin;

public class DynamicBackpacks extends JavaPlugin {

    private static DynamicBackpacks instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private BackpackSession backpackSession;
    private BackpackManager backpackManager;
    private RecipeManager recipeManager;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.load();

        if (configManager.getMode() == ConfigManager.Mode.OFF) {
            getLogger().info("DynamicBackpacks is set to OFF — all functionality disabled.");
            return;
        }

        databaseManager = new SQLiteDatabase(this, configManager.getDatabaseFile());
        try {
            databaseManager.initialize();
        } catch (Exception e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        backpackSession = new BackpackSession();
        backpackManager = new BackpackManager(this);

        recipeManager = new RecipeManager(this);
        recipeManager.registerAll();
        getServer().getPluginManager().registerEvents(recipeManager, this);

        getServer().getPluginManager().registerEvents(new BackpackInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new BackpackProtectListener(this), this);
        getServer().getPluginManager().registerEvents(new BackpackInventoryListener(this), this);

        BackpackCommand bpCmd = new BackpackCommand(this);
        getCommand("bp").setExecutor(bpCmd);
        getCommand("bp").setTabCompleter(bpCmd);

        AdminCommand dbpCmd = new AdminCommand(this);
        getCommand("dbp").setExecutor(dbpCmd);
        getCommand("dbp").setTabCompleter(dbpCmd);

        getLogger().info("DynamicBackpacks enabled! Mode: " + configManager.getMode());
    }

    @Override
    public void onDisable() {
        if (backpackManager != null) {
            backpackManager.saveAll();
        }
        if (recipeManager != null) {
            recipeManager.unregisterAll();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        getLogger().info("DynamicBackpacks disabled.");
    }

    public void reload() {
        if (backpackManager != null) {
            backpackManager.saveAll();
        }

        configManager.load();

        if (recipeManager != null) {
            recipeManager.unregisterAll();
            recipeManager.registerAll();
        }

        getLogger().info("DynamicBackpacks reloaded.");
    }


    public static DynamicBackpacks getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public BackpackSession getBackpackSession() { return backpackSession; }
    public BackpackManager getBackpackManager() { return backpackManager; }
    public RecipeManager getRecipeManager() { return recipeManager; }
}
