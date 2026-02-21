package com.blockforge.dynamicbackpacks.backpack;

import com.blockforge.dynamicbackpacks.DynamicBackpacks;
import com.blockforge.dynamicbackpacks.config.BackpackTierConfig;
import com.blockforge.dynamicbackpacks.database.DatabaseManager;
import com.blockforge.dynamicbackpacks.gui.BackpackGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackpackManager {

    private final DynamicBackpacks plugin;
    private final DatabaseManager db;
    private final BackpackSession session;

    // key → backpack, cleared on close
    private final Map<String, Backpack> cache = new HashMap<>();

    public BackpackManager(DynamicBackpacks plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
        this.session = plugin.getBackpackSession();
    }

    public void openItemBackpack(Player player, UUID backpackUUID) {
        String key = BackpackSession.itemKey(backpackUUID);

        if (session.isOpen(key)) {
            UUID openBy = session.getOpenBy(key);
            if (player.getUniqueId().equals(openBy)) {
                player.sendMessage(ChatColor.RED + "This backpack is already open.");
            } else {
                player.sendMessage(ChatColor.RED + "This backpack is currently being viewed by an admin.");
            }
            return;
        }

        closePlayerBackpack(player);

        Backpack backpack;
        if (cache.containsKey(key)) {
            backpack = cache.get(key);
        } else {
            // only shown on DB fetch, not cache hit
            player.sendMessage(ChatColor.GRAY + "Loading backpack...");
            backpack = db.loadItemBackpack(backpackUUID);
            if (backpack == null) {
                player.sendMessage(ChatColor.RED + "Backpack data could not be found.");
                return;
            }
            cache.put(key, backpack);
        }

        BackpackTierConfig tierConfig = plugin.getConfigManager().getTier(backpack.getTier());
        if (tierConfig == null) {
            player.sendMessage(ChatColor.RED + "Invalid backpack tier configuration.");
            return;
        }

        Inventory inv = BackpackGUI.build(backpack, tierConfig, false);
        session.open(key, player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    public void openCommandBackpack(Player player, int slot) {
        String key = BackpackSession.commandKey(player.getUniqueId(), slot);

        if (session.isOpen(key)) {
            player.sendMessage(ChatColor.RED + "This backpack is already open.");
            return;
        }

        closePlayerBackpack(player);

        int defaultTier = plugin.getConfigManager().getCommandBackpackDefaultTier();
        Backpack backpack;
        if (cache.containsKey(key)) {
            backpack = cache.get(key);
        } else {
            player.sendMessage(ChatColor.GRAY + "Loading backpack...");
            backpack = db.loadCommandBackpack(player.getUniqueId(), slot, defaultTier);
            cache.put(key, backpack);
        }

        BackpackTierConfig tierConfig = plugin.getConfigManager().getTier(backpack.getTier());
        if (tierConfig == null) {
            player.sendMessage(ChatColor.RED + "Invalid backpack tier configuration.");
            return;
        }

        Inventory inv = BackpackGUI.build(backpack, tierConfig, false);
        session.open(key, player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    public void adminViewCommandBackpack(Player admin, Player target, int slot) {
        String key = BackpackSession.commandKey(target.getUniqueId(), slot);

        if (session.isOpen(key)) {
            admin.sendMessage(ChatColor.RED + "That backpack is currently open by its owner.");
            return;
        }

        closePlayerBackpack(admin);

        int defaultTier = plugin.getConfigManager().getCommandBackpackDefaultTier();
        Backpack backpack;
        if (cache.containsKey(key)) {
            backpack = cache.get(key);
        } else {
            admin.sendMessage(ChatColor.GRAY + "Loading backpack...");
            backpack = db.loadCommandBackpack(target.getUniqueId(), slot, defaultTier);
            cache.put(key, backpack);
        }

        BackpackTierConfig tierConfig = plugin.getConfigManager().getTier(backpack.getTier());
        if (tierConfig == null) {
            admin.sendMessage(ChatColor.RED + "Invalid backpack tier configuration.");
            return;
        }

        Inventory inv = BackpackGUI.build(backpack, tierConfig, true);
        session.open(key, admin.getUniqueId(), inv);
        admin.openInventory(inv);

        if (target.isOnline()) {
            target.sendMessage(ChatColor.YELLOW + "An admin is currently viewing your backpack (slot " + slot + ").");
        }
    }

    public void saveAndClose(String key) {
        Inventory inv = session.getOpenInventory(key);
        Backpack backpack = cache.get(key);
        if (backpack == null || inv == null) {
            session.close(key);
            return;
        }

        backpack.setContents(inv.getContents());

        if (backpack.getType() == BackpackType.ITEM) {
            db.saveItemBackpack(backpack);
        } else {
            db.saveCommandBackpack(backpack);
        }

        session.close(key);
        cache.remove(key);
    }

    public void saveAll() {
        for (String key : session.getAllOpenSessions().keySet().toArray(new String[0])) {
            saveAndClose(key);
        }
    }

    public void closePlayerBackpack(Player player) {
        String key = session.getKeyOpenByPlayer(player.getUniqueId());
        if (key != null) {
            saveAndClose(key);
        }
    }

    public BackpackSession getSession() { return session; }
}
