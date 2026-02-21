package com.blockforge.dynamicbackpacks.config;

import com.blockforge.dynamicbackpacks.DynamicBackpacks;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ConfigManager {

    public enum Mode { BOTH, ITEM, COMMAND, OFF }

    private static final int CONFIG_VERSION = 2;

    private final DynamicBackpacks plugin;
    private final Logger log;

    private Mode mode;
    private String databaseFile;
    private int commandBackpackDefaultTier;
    private boolean lootEnabled;
    private boolean allowShulkerBoxes;
    private final Map<Integer, BackpackTierConfig> tiers = new HashMap<>();

    public ConfigManager(DynamicBackpacks plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void load() {
        tiers.clear();
        loadMainConfig();
        loadBackpacksConfig();
    }

    private void loadMainConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        int storedVersion = cfg.getInt("config-version", 0);
        if (storedVersion < CONFIG_VERSION) {
            addMissingDefaults(cfg);
        }

        String modeStr = cfg.getString("mode", "both").toUpperCase();
        try {
            mode = Mode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            log.warning("Invalid mode '" + modeStr + "' in config.yml — defaulting to BOTH.");
            mode = Mode.BOTH;
        }

        databaseFile = cfg.getString("database.file", "backpacks.db");
        commandBackpackDefaultTier = Math.max(1, Math.min(6,
                cfg.getInt("command-backpacks.default-tier", 6)));
        lootEnabled = cfg.getBoolean("loot.enabled", false);
        allowShulkerBoxes = cfg.getBoolean("inventory.allow-shulker-boxes", false);
    }

    private void addMissingDefaults(FileConfiguration cfg) {
        if (!cfg.contains("loot.enabled")) {
            cfg.set("loot.enabled", false);
        }
        if (!cfg.contains("inventory.allow-shulker-boxes")) {
            cfg.set("inventory.allow-shulker-boxes", false);
        }
        cfg.set("config-version", CONFIG_VERSION);
        try {
            cfg.save(new File(plugin.getDataFolder(), "config.yml"));
            log.info("config.yml migrated to version " + CONFIG_VERSION + ".");
        } catch (IOException e) {
            log.warning("Failed to save migrated config.yml: " + e.getMessage());
        }
    }

    private void loadBackpacksConfig() {
        File file = new File(plugin.getDataFolder(), "backpacks.yml");
        if (!file.exists()) {
            plugin.saveResource("backpacks.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection tiersSection = cfg.getConfigurationSection("tiers");
        if (tiersSection == null) {
            log.warning("No 'tiers' section found in backpacks.yml!");
            return;
        }

        for (String key : tiersSection.getKeys(false)) {
            int tierNum;
            try {
                tierNum = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                log.warning("Invalid tier key '" + key + "' in backpacks.yml — skipping.");
                continue;
            }

            ConfigurationSection ts = tiersSection.getConfigurationSection(key);
            if (ts == null) continue;

            boolean tierEnabled = ts.getBoolean("enabled", true);
            if (!tierEnabled) {
                log.info("Tier " + tierNum + " is disabled — skipping.");
                continue;
            }

            int rows = Math.max(1, Math.min(6, ts.getInt("rows", tierNum)));
            String displayName = ts.getString("display-name", "&fTier " + tierNum + " Backpack");
            String texture = ts.getString("texture", "");

            boolean craftEnabled = ts.getBoolean("crafting.enabled", true);
            List<String> shape = ts.getStringList("crafting.shape");
            Map<Character, String> ingredients = new HashMap<>();

            ConfigurationSection ingSection = ts.getConfigurationSection("crafting.ingredients");
            if (ingSection != null) {
                for (String ingKey : ingSection.getKeys(false)) {
                    if (ingKey.length() == 1) {
                        ingredients.put(ingKey.charAt(0), ingSection.getString(ingKey));
                    }
                }
            }

            Map<String, int[]> lootMap = new HashMap<>();
            ConfigurationSection lootSection = ts.getConfigurationSection("loot");
            if (lootSection != null) {
                for (String loc : lootSection.getKeys(false)) {
                    ConfigurationSection locSec = lootSection.getConfigurationSection(loc);
                    if (locSec != null) {
                        boolean enabled = locSec.getBoolean("enabled", false);
                        int chance = Math.max(0, Math.min(100, locSec.getInt("chance", 0)));
                        lootMap.put(loc, new int[]{enabled ? 1 : 0, chance});
                    }
                }
            }

            tiers.put(tierNum, new BackpackTierConfig(tierNum, rows, displayName, texture,
                    craftEnabled, shape, ingredients, lootMap));
        }

        log.info("Loaded " + tiers.size() + " backpack tier(s).");
    }

    public Mode getMode() { return mode; }
    public boolean isItemEnabled() { return mode == Mode.BOTH || mode == Mode.ITEM; }
    public boolean isCommandEnabled() { return mode == Mode.BOTH || mode == Mode.COMMAND; }
    public String getDatabaseFile() { return databaseFile; }
    public int getCommandBackpackDefaultTier() { return commandBackpackDefaultTier; }
    public boolean isLootEnabled() { return lootEnabled; }
    public boolean isShulkerBoxAllowed() { return allowShulkerBoxes; }

    public BackpackTierConfig getTier(int tier) { return tiers.get(tier); }
    public Map<Integer, BackpackTierConfig> getAllTiers() { return tiers; }
    public int getMaxTier() { return tiers.isEmpty() ? 6 : tiers.keySet().stream().max(Integer::compareTo).orElse(6); }
}
