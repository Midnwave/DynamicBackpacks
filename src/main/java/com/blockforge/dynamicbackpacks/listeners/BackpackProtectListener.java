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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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

        String title = event.getView().getTitle();
        if (!title.startsWith(BackpackGUI.TITLE_PREFIX)) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (BackpackItemFactory.isBackpack(cursor)) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot put a backpack inside a backpack.");
            return;
        }

        if (event.isShiftClick() && BackpackItemFactory.isBackpack(current)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        String title = event.getView().getTitle();
        if (!title.startsWith(BackpackGUI.TITLE_PREFIX)) return;

        if (BackpackItemFactory.isBackpack(event.getOldCursor())) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot put a backpack inside a backpack.");
        }
    }
}
