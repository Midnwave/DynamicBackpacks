package com.blockforge.dynamicbackpacks.config;

import com.blockforge.dynamicbackpacks.DynamicBackpacks;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ConfigManager {

    public enum Mode { BOTH, ITEM, COMMAND, OFF }

    private final DynamicBackpacks plugin;
    private final Logger log;

    private Mode mode;
    private String databaseFile;
    private int commandBackpackDefaultTier;
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

            tiers.put(tierNum, new BackpackTierConfig(tierNum, rows, displayName, texture,
                    craftEnabled, shape, ingredients));
        }

        log.info("Loaded " + tiers.size() + " backpack tier(s).");
    }

    public Mode getMode() { return mode; }
    public boolean isItemEnabled() { return mode == Mode.BOTH || mode == Mode.ITEM; }
    public boolean isCommandEnabled() { return mode == Mode.BOTH || mode == Mode.COMMAND; }
    public String getDatabaseFile() { return databaseFile; }
    public int getCommandBackpackDefaultTier() { return commandBackpackDefaultTier; }

    public BackpackTierConfig getTier(int tier) { return tiers.get(tier); }
    public Map<Integer, BackpackTierConfig> getAllTiers() { return tiers; }
    public int getMaxTier() { return tiers.isEmpty() ? 6 : tiers.keySet().stream().max(Integer::compareTo).orElse(6); }
}
