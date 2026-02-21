package com.blockforge.dynamicbackpacks.listeners;

import com.blockforge.dynamicbackpacks.DynamicBackpacks;
import com.blockforge.dynamicbackpacks.gui.BackpackGUI;
import com.blockforge.dynamicbackpacks.item.BackpackItemFactory;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class BackpackProtectListener implements Listener {

    private final DynamicBackpacks plugin;

    public BackpackProtectListener(DynamicBackpacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (BackpackItemFactory.isBackpack(event.getItemInHand())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot place a backpack as a block.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        // Prevent direct drag of backpack into an armor slot (helmet equip)
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            if (BackpackItemFactory.isBackpack(event.getCursor())) {
                event.setCancelled(true);
                return;
            }
        }

        // Prevent shift-click equip when only the player's own inventory is open
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && BackpackItemFactory.isBackpack(event.getCurrentItem())) {
            InventoryType topType = event.getView().getTopInventory().getType();
            if (topType == InventoryType.CRAFTING || topType == InventoryType.PLAYER) {
                event.setCancelled(true);
                return;
            }
        }

        // --- Backpack GUI context ---
        @SuppressWarnings("deprecation")
        String title = event.getView().getTitle();
        if (!title.startsWith(BackpackGUI.TITLE_PREFIX)) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        // Prevent nesting backpacks
        if (BackpackItemFactory.isBackpack(cursor)) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot put a backpack inside a backpack.");
            return;
        }

        // Prevent shift-clicking backpack items out via shift-click while inside GUI
        if (event.isShiftClick() && BackpackItemFactory.isBackpack(current)) {
            event.setCancelled(true);
            return;
        }

        // Prevent shulker boxes in backpack GUI (if disabled in config)
        if (!plugin.getConfigManager().isShulkerBoxAllowed()) {
            if (isShulkerBox(cursor)) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot put shulker boxes inside a backpack.");
                return;
            }
            if (event.isShiftClick() && isShulkerBox(current)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        @SuppressWarnings("deprecation")
        String title = event.getView().getTitle();
        if (!title.startsWith(BackpackGUI.TITLE_PREFIX)) return;

        if (BackpackItemFactory.isBackpack(event.getOldCursor())) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot put a backpack inside a backpack.");
            return;
        }

        if (!plugin.getConfigManager().isShulkerBoxAllowed() && isShulkerBox(event.getOldCursor())) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot put shulker boxes inside a backpack.");
        }
    }

    private static boolean isShulkerBox(ItemStack item) {
        if (item == null) return false;
        return item.getType().name().endsWith("_SHULKER_BOX");
    }
}
