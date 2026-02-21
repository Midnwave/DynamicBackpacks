package com.blockforge.dynamicbackpacks.listeners;

import com.blockforge.dynamicbackpacks.DynamicBackpacks;
import com.blockforge.dynamicbackpacks.backpack.BackpackSession;
import com.blockforge.dynamicbackpacks.gui.BackpackGUI;
import com.blockforge.dynamicbackpacks.item.BackpackItemFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BackpackInventoryListener implements Listener {

    private final DynamicBackpacks plugin;

    public BackpackInventoryListener(DynamicBackpacks plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (!BackpackGUI.isBackpackTitle(title)) return;

        UUID playerUUID = event.getPlayer().getUniqueId();
        String key = plugin.getBackpackSession().getKeyOpenByPlayer(playerUUID);
        if (key == null) return;

        plugin.getBackpackManager().saveAndClose(key);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        String key = plugin.getBackpackSession().getKeyOpenByPlayer(playerUUID);
        if (key == null) return;

        plugin.getBackpackManager().saveAndClose(key);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        // block stacking exploit via double-click collect
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            if (BackpackItemFactory.isBackpack(event.getCursor())) {
                event.setCancelled(true);
                return;
            }
        }

        // block hotbar swap with a backpack item inside a backpack GUI
        if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            @SuppressWarnings("deprecation")
            String title = event.getView().getTitle();
            if (BackpackGUI.isBackpackTitle(title)) {
                Player player = (Player) event.getWhoClicked();
                int hotbarSlot = event.getHotbarButton();
                if (hotbarSlot >= 0) {
                    ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
                    if (BackpackItemFactory.isBackpack(hotbarItem)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        // always enforce stack size of 1 on backpack items
        ItemStack cursor = event.getCursor();
        if (BackpackItemFactory.isBackpack(cursor) && cursor.getAmount() > 1) {
            cursor.setAmount(1);
        }

        ItemStack current = event.getCurrentItem();
        if (BackpackItemFactory.isBackpack(current) && current.getAmount() > 1) {
            current.setAmount(1);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!BackpackItemFactory.isBackpack(event.getOldCursor())) return;

        // prevent splitting a backpack into multiple slots
        if (event.getNewItems().size() > 1) {
            event.setCancelled(true);
        }
    }
}
