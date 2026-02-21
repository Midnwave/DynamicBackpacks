package com.blockforge.dynamicbackpacks.config;

import java.util.List;
import java.util.Map;

public class BackpackTierConfig {

    private final int tier;
    private final int rows;
    private final String displayName;
    private final String texture;
    private final boolean craftingEnabled;
    private final List<String> craftingShape;
    private final Map<Character, String> craftingIngredients;
    // location key -> [enabled(0/1), chance(0-100)]
    private final Map<String, int[]> loot;

    public BackpackTierConfig(int tier, int rows, String displayName, String texture,
                              boolean craftingEnabled, List<String> craftingShape,
                              Map<Character, String> craftingIngredients,
                              Map<String, int[]> loot) {
        this.tier = tier;
        this.rows = rows;
        this.displayName = displayName;
        this.texture = texture;
        this.craftingEnabled = craftingEnabled;
        this.craftingShape = craftingShape;
        this.craftingIngredients = craftingIngredients;
        this.loot = loot;
    }

    public int getTier() { return tier; }
    public int getRows() { return rows; }
    public String getDisplayName() { return displayName; }
    public String getTexture() { return texture; }
    public boolean isCraftingEnabled() { return craftingEnabled; }
    public List<String> getCraftingShape() { return craftingShape; }
    public Map<Character, String> getCraftingIngredients() { return craftingIngredients; }

    public boolean isLootEnabled(String location) {
        int[] data = loot.get(location);
        return data != null && data[0] == 1;
    }

    public int getLootChance(String location) {
        int[] data = loot.get(location);
        return data != null ? data[1] : 0;
    }
}
