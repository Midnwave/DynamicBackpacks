package com.blockforge.dynamicbackpacks.backpack;

import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackpackSession {

    // key → who has it open
    private final Map<String, UUID> openBy = new HashMap<>();
    // key → live inventory object
    private final Map<String, Inventory> openInventories = new HashMap<>();
    // player → key (reverse lookup)
    private final Map<UUID, String> playerOpenKey = new HashMap<>();

    public static String itemKey(UUID backpackUUID) {
        return "item:" + backpackUUID;
    }

    public static String commandKey(UUID playerUUID, int slot) {
        return "cmd:" + playerUUID + ":" + slot;
    }

    public boolean isOpen(String key) {
        return openBy.containsKey(key);
    }

    public boolean isOpenByPlayer(UUID playerUUID) {
        return playerOpenKey.containsKey(playerUUID);
    }

    public UUID getOpenBy(String key) {
        return openBy.get(key);
    }

    public String getKeyOpenByPlayer(UUID playerUUID) {
        return playerOpenKey.get(playerUUID);
    }

    public Inventory getOpenInventory(String key) {
        return openInventories.get(key);
    }

    public void open(String key, UUID playerUUID, Inventory inventory) {
        openBy.put(key, playerUUID);
        openInventories.put(key, inventory);
        playerOpenKey.put(playerUUID, key);
    }

    public void close(String key) {
        UUID player = openBy.remove(key);
        openInventories.remove(key);
        if (player != null) {
            playerOpenKey.remove(player);
        }
    }

    public void closeByPlayer(UUID playerUUID) {
        String key = playerOpenKey.remove(playerUUID);
        if (key != null) {
            openBy.remove(key);
            openInventories.remove(key);
        }
    }

    public Map<String, Inventory> getAllOpenInventories() {
        return openInventories;
    }

    public Map<String, UUID> getAllOpenSessions() {
        return openBy;
    }
}
