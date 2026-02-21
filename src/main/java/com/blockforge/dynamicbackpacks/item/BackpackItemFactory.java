package com.blockforge.dynamicbackpacks.item;

import com.blockforge.dynamicbackpacks.DynamicBackpacks;
import com.blockforge.dynamicbackpacks.config.BackpackTierConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

public class BackpackItemFactory {

    public static final NamespacedKey KEY_ID   = new NamespacedKey(DynamicBackpacks.getInstance(), "id");
    public static final NamespacedKey KEY_TIER = new NamespacedKey(DynamicBackpacks.getInstance(), "tier");

    private BackpackItemFactory() {}

    public static ItemStack create(BackpackTierConfig tierConfig, UUID ownerUUID) {
        UUID backpackUUID = UUID.randomUUID();
        return buildItem(tierConfig, backpackUUID);
    }

    public static ItemStack createForUUID(BackpackTierConfig tierConfig, UUID backpackUUID) {
        return buildItem(tierConfig, backpackUUID);
    }

    private static ItemStack buildItem(BackpackTierConfig tierConfig, UUID backpackUUID) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;

        String name = ChatColor.translateAlternateColorCodes('&', tierConfig.getDisplayName());
        meta.setDisplayName(name);

        applyTexture(meta, tierConfig.getTexture(), tierConfig.getTier());

        meta.getPersistentDataContainer().set(KEY_ID, PersistentDataType.STRING, backpackUUID.toString());
        meta.getPersistentDataContainer().set(KEY_TIER, PersistentDataType.INTEGER, tierConfig.getTier());

        item.setItemMeta(meta);
        item.setAmount(1);
        return item;
    }

    public static UUID getBackpackUUID(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) return null;
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return null;
        String uuidStr = meta.getPersistentDataContainer().get(KEY_ID, PersistentDataType.STRING);
        if (uuidStr == null) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static int getBackpackTier(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) return -1;
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return -1;
        Integer tier = meta.getPersistentDataContainer().get(KEY_TIER, PersistentDataType.INTEGER);
        return tier != null ? tier : -1;
    }

    public static boolean isBackpack(ItemStack item) {
        return getBackpackUUID(item) != null;
    }

    // Bukkit PlayerProfile API — deprecated in Paper 1.21+ but kept for Spigot compat
    @SuppressWarnings("deprecation")
    private static void applyTexture(SkullMeta meta, String base64Texture, int tier) {
        if (base64Texture == null || base64Texture.isEmpty()) return;
        try {
            String skinUrl = decodeSkinUrl(base64Texture);
            if (skinUrl == null) return;

            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "DB_Tier" + tier);
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create(skinUrl).toURL());
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (MalformedURLException e) {
            DynamicBackpacks.getInstance().getLogger()
                    .warning("Invalid texture URL for tier " + tier + ": " + e.getMessage());
        } catch (Exception e) {
            DynamicBackpacks.getInstance().getLogger()
                    .warning("Failed to apply texture for tier " + tier + ": " + e.getMessage());
        }
    }

    // decodes minecraft-heads.com Base64 value to the skin URL
    private static String decodeSkinUrl(String base64) {
        try {
            String json = new String(Base64.getDecoder().decode(base64));
            int urlStart = json.indexOf("\"url\":\"") + 7;
            if (urlStart < 7) return null;
            int urlEnd = json.indexOf("\"", urlStart);
            if (urlEnd < 0) return null;
            return json.substring(urlStart, urlEnd);
        } catch (Exception e) {
            return null;
        }
    }
}
