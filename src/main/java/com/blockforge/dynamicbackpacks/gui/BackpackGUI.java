package com.blockforge.dynamicbackpacks.gui;

import com.blockforge.dynamicbackpacks.backpack.Backpack;
import com.blockforge.dynamicbackpacks.config.BackpackTierConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class BackpackGUI {

    // used to identify backpack GUIs in event listeners
    public static final String TITLE_PREFIX = ChatColor.DARK_GRAY + "[Backpack] ";

    private BackpackGUI() {}

    @SuppressWarnings("deprecation")
    public static Inventory build(Backpack backpack, BackpackTierConfig tierConfig, String displayName, boolean adminView) {
        int slots = tierConfig.getRows() * 9;
        String rawName = ChatColor.translateAlternateColorCodes('&', displayName);
        String title = TITLE_PREFIX + rawName + (adminView ? ChatColor.GRAY + " (Admin)" : "");

        Inventory inv = Bukkit.createInventory(null, slots, title);

        ItemStack[] contents = backpack.getContents();
        if (contents != null && contents.length > 0) {
            int copyLen = Math.min(contents.length, slots);
            for (int i = 0; i < copyLen; i++) {
                if (contents[i] != null) {
                    inv.setItem(i, contents[i]);
                }
            }
        }

        return inv;
    }

    public static boolean isBackpackTitle(String title) {
        return title != null && title.startsWith(TITLE_PREFIX);
    }
}
