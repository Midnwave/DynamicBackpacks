package com.blockforge.dynamicbackpacks.recipe;

import com.blockforge.dynamicbackpacks.DynamicBackpacks;
import com.blockforge.dynamicbackpacks.backpack.Backpack;
import com.blockforge.dynamicbackpacks.config.BackpackTierConfig;
import com.blockforge.dynamicbackpacks.item.BackpackItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.*;
import java.util.logging.Logger;

public class RecipeManager implements Listener {

    private static final String TIER_KEY_PREFIX = "TIER_";
    private static final String TIER_KEY_SUFFIX = "_BACKPACK";

    private final DynamicBackpacks plugin;
    private final Logger log;
    // tier → recipe key
    private final Map<Integer, NamespacedKey> registeredKeys = new HashMap<>();

    public RecipeManager(DynamicBackpacks plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void registerAll() {
        unregisterAll();
        for (Map.Entry<Integer, BackpackTierConfig> entry : plugin.getConfigManager().getAllTiers().entrySet()) {
            int tier = entry.getKey();
            BackpackTierConfig config = entry.getValue();
            if (config.isCraftingEnabled()) {
                registerRecipe(tier, config);
            }
        }
    }

    private void registerRecipe(int tier, BackpackTierConfig config) {
        List<String> shape = config.getCraftingShape();
        if (shape == null || shape.size() != 3) {
            log.warning("Tier " + tier + " has an invalid crafting shape in backpacks.yml — skipping recipe.");
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, "backpack_tier_" + tier);
        ShapedRecipe recipe = new ShapedRecipe(key, placeholderResult(tier));
        recipe.shape(shape.get(0), shape.get(1), shape.get(2));

        Map<Character, String> ingredients = config.getCraftingIngredients();
        Set<Character> usedChars = new HashSet<>();
        for (String row : shape) {
            for (char c : row.toCharArray()) {
                if (c != ' ') usedChars.add(c);
            }
        }

        boolean valid = true;
        for (char c : usedChars) {
            String value = ingredients.get(c);
            if (value == null) {
                log.warning("Tier " + tier + " recipe: no ingredient defined for '" + c + "'.");
                valid = false;
                continue;
            }

            if (isBackpackKey(value)) {
                // MaterialChoice(PLAYER_HEAD) is used so any player head (including backpacks)
                // satisfies the slot; the correct tier is validated in PrepareItemCraftEvent.
                recipe.setIngredient(c, new RecipeChoice.MaterialChoice(Material.PLAYER_HEAD));
            } else {
                try {
                    Material mat = Material.valueOf(value.toUpperCase());
                    recipe.setIngredient(c, mat);
                } catch (IllegalArgumentException e) {
                    log.warning("Tier " + tier + " recipe: unknown material '" + value + "' for '" + c + "'.");
                    valid = false;
                }
            }
        }

        if (!valid) {
            log.warning("Tier " + tier + " recipe has errors and was not registered.");
            return;
        }

        Bukkit.addRecipe(recipe);
        registeredKeys.put(tier, key);
        log.info("Registered crafting recipe for Tier " + tier + " backpack.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) return;

        Integer tier = getTierForRecipe(recipe);
        if (tier == null) return;

        BackpackTierConfig config = plugin.getConfigManager().getTier(tier);
        if (config == null) return;

        // MaterialChoice(PLAYER_HEAD) matches any player head — validate the correct backpack tier
        Integer requiredTier = getRequiredIngredientTier(config);
        if (requiredTier != null) {
            boolean found = false;
            for (ItemStack item : event.getInventory().getMatrix()) {
                if (BackpackItemFactory.getBackpackTier(item) == requiredTier) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                event.getInventory().setResult(null);
                return;
            }
        }

        event.getInventory().setResult(placeholderResult(tier));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getRecipe().getResult();
        int tier = BackpackItemFactory.getBackpackTier(result);
        if (tier < 1) return;

        BackpackTierConfig config = plugin.getConfigManager().getTier(tier);
        if (config == null) return;

        if (!player.hasPermission("dynamicbackpacks.craft.tier." + tier)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You don't have permission to craft a Tier " + tier + " backpack.");
            return;
        }

        if (!plugin.getConfigManager().isItemEnabled()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Item backpacks are disabled on this server.");
            return;
        }

        // Transfer contents from the ingredient backpack (if upgrading)
        ItemStack[] transferredContents = new ItemStack[0];
        UUID ingredientUUID = null;
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient == null) continue;
            UUID uuid = BackpackItemFactory.getBackpackUUID(ingredient);
            if (uuid != null) {
                ingredientUUID = uuid;
                Backpack existing = plugin.getDatabaseManager().loadItemBackpack(uuid);
                if (existing != null) {
                    transferredContents = existing.getContents();
                }
                break;
            }
        }

        // Generate real item with fresh UUID
        ItemStack realItem = BackpackItemFactory.create(config, player.getUniqueId());
        UUID backpackUUID = BackpackItemFactory.getBackpackUUID(realItem);

        Backpack bp = new Backpack(backpackUUID, player.getUniqueId(), tier, transferredContents);
        plugin.getDatabaseManager().saveItemBackpack(bp);

        // Remove the consumed ingredient backpack from the database
        if (ingredientUUID != null) {
            plugin.getDatabaseManager().deleteItemBackpack(ingredientUUID);
        }

        event.getInventory().setResult(realItem);
    }

    public void unregisterAll() {
        for (NamespacedKey key : registeredKeys.values()) {
            Bukkit.removeRecipe(key);
        }
        registeredKeys.clear();
    }

    // carries the tier tag so CraftItemEvent can identify which tier was crafted
    private ItemStack placeholderResult(int tier) {
        BackpackTierConfig config = plugin.getConfigManager().getTier(tier);
        if (config != null) {
            return BackpackItemFactory.create(config, null);
        }
        return new ItemStack(Material.PLAYER_HEAD);
    }

    private Integer getTierForRecipe(Recipe recipe) {
        if (!(recipe instanceof ShapedRecipe shaped)) return null;
        for (Map.Entry<Integer, NamespacedKey> entry : registeredKeys.entrySet()) {
            if (shaped.getKey().equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean isBackpackKey(String value) {
        return value != null && value.startsWith(TIER_KEY_PREFIX) && value.endsWith(TIER_KEY_SUFFIX);
    }

    private Integer getRequiredIngredientTier(BackpackTierConfig config) {
        for (String value : config.getCraftingIngredients().values()) {
            if (isBackpackKey(value)) {
                int t = parseTierFromKey(value);
                return t >= 0 ? t : null;
            }
        }
        return null;
    }

    private int parseTierFromKey(String value) {
        try {
            String numPart = value.substring(TIER_KEY_PREFIX.length(), value.length() - TIER_KEY_SUFFIX.length());
            return Integer.parseInt(numPart);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return -1;
        }
    }
}
