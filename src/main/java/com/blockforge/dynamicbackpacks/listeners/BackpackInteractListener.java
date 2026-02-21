package com.blockforge.dynamicbackpacks.listeners;

import com.blockforge.dynamicbackpacks.DynamicBackpacks;
import com.blockforge.dynamicbackpacks.item.BackpackItemFactory;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;


import java.util.UUID;

public class BackpackInteractListener implements Listener {

    private final DynamicBackpacks plugin;

    public BackpackInteractListener(DynamicBackpacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        switch (event.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                break;
            default:
                return;
        }

        ItemStack item = event.getItem();
        if (!BackpackItemFactory.isBackpack(item)) return;

        // prevent any block interaction (open chest, place block, etc.)
        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);

        if (!plugin.getConfigManager().isItemEnabled()) {
            event.getPlayer().sendMessage(ChatColor.RED + "Item backpacks are disabled on this server.");
            return;
        }

        Player player = event.getPlayer();
        UUID backpackUUID = BackpackItemFactory.getBackpackUUID(item);
        int tier = BackpackItemFactory.getBackpackTier(item);

        if (backpackUUID == null || tier < 1) return;

        if (!player.hasPermission("dynamicbackpacks.use.item.tier." + tier)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this backpack.");
            return;
        }

        @SuppressWarnings("deprecation")
        String displayName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName() : "Backpack";

        plugin.getBackpackManager().openItemBackpack(player, backpackUUID, tier, displayName);
    }
}
