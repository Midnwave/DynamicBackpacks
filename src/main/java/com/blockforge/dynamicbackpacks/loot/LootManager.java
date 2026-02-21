package com.blockforge.dynamicbackpacks.loot;

import com.blockforge.dynamicbackpacks.DynamicBackpacks;
import com.blockforge.dynamicbackpacks.config.BackpackTierConfig;
import com.blockforge.dynamicbackpacks.item.BackpackItemFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LootManager implements Listener {

    // friendly config key -> minecraft loot table key
    private static final Map<String, NamespacedKey> LOOT_TABLE_MAP = new LinkedHashMap<>();

    static {
        // Overworld structures
        r("dungeon",                     "chests/simple_dungeon");
        r("mineshaft",                   "chests/abandoned_mineshaft");
        r("desert_temple",               "chests/desert_pyramid");
        r("jungle_temple",               "chests/jungle_temple");
        r("stronghold_corridor",         "chests/stronghold_corridor");
        r("stronghold_library",          "chests/stronghold_library");
        r("stronghold_crossing",         "chests/stronghold_crossing");
        r("woodland_mansion",            "chests/woodland_mansion");
        r("igloo",                       "chests/igloo_chest");
        r("pillager_outpost",            "chests/pillager_outpost");
        r("shipwreck_map",               "chests/shipwreck_map");
        r("shipwreck_supply",            "chests/shipwreck_supply");
        r("shipwreck_treasure",          "chests/shipwreck_treasure");
        r("ocean_ruin_big",              "chests/underwater_ruin_big");
        r("ocean_ruin_small",            "chests/underwater_ruin_small");
        r("buried_treasure",             "chests/buried_treasure");
        r("ruined_portal",               "chests/ruined_portal");
        r("ancient_city",                "chests/ancient_city");
        r("ancient_city_ice_box",        "chests/ancient_city_ice_box");
        // Villages
        r("village_armorer",             "chests/village/village_armorer");
        r("village_butcher",             "chests/village/village_butcher");
        r("village_cartographer",        "chests/village/village_cartographer");
        r("village_desert_house",        "chests/village/village_desert_house");
        r("village_fisher",              "chests/village/village_fisher");
        r("village_fletcher",            "chests/village/village_fletcher");
        r("village_mason",               "chests/village/village_mason");
        r("village_plains_house",        "chests/village/village_plains_house");
        r("village_savanna_house",       "chests/village/village_savanna_house");
        r("village_shepherd",            "chests/village/village_shepherd");
        r("village_snowy_house",         "chests/village/village_snowy_house");
        r("village_taiga_house",         "chests/village/village_taiga_house");
        r("village_tannery",             "chests/village/village_tannery");
        r("village_temple",              "chests/village/village_temple");
        r("village_toolsmith",           "chests/village/village_toolsmith");
        r("village_weaponsmith",         "chests/village/village_weaponsmith");
        // Nether
        r("nether_fortress",             "chests/nether_bridge");
        r("bastion_bridge",              "chests/bastion_bridge");
        r("bastion_hoglin_stable",       "chests/bastion_hoglin_stable");
        r("bastion_other",               "chests/bastion_other");
        r("bastion_treasure",            "chests/bastion_treasure");
        // End
        r("end_city",                    "chests/end_city_treasure");
        // Trial Chambers (1.21)
        r("trial_chambers_supply",       "chests/trial_chambers/supply");
        r("trial_chambers_corridor",     "chests/trial_chambers/corridor");
        r("trial_chambers_entrance",     "chests/trial_chambers/entrance");
        r("trial_chambers_intersection", "chests/trial_chambers/intersection");
        r("trial_chambers_reward",       "chests/trial_chambers/reward");
        r("trial_chambers_reward_rare",  "chests/trial_chambers/reward_rare");
    }

    private static void r(String friendly, String key) {
        LOOT_TABLE_MAP.put(friendly, NamespacedKey.minecraft(key));
    }

    private final DynamicBackpacks plugin;
    private final Random rng = new Random();

    public LootManager(DynamicBackpacks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        if (!plugin.getConfigManager().isLootEnabled()) return;

        NamespacedKey tableKey = event.getLootTable().getKey();

        String location = null;
        for (Map.Entry<String, NamespacedKey> entry : LOOT_TABLE_MAP.entrySet()) {
            if (entry.getValue().equals(tableKey)) {
                location = entry.getKey();
                break;
            }
        }
        if (location == null) return;

        // try tiers from highest to lowest; add the first one that rolls successfully
        List<Map.Entry<Integer, BackpackTierConfig>> sorted =
                new ArrayList<>(plugin.getConfigManager().getAllTiers().entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getKey(), a.getKey()));

        for (Map.Entry<Integer, BackpackTierConfig> entry : sorted) {
            BackpackTierConfig tierConfig = entry.getValue();
            if (!tierConfig.isLootEnabled(location)) continue;
            int chance = tierConfig.getLootChance(location);
            if (chance <= 0) continue;
            if (rng.nextInt(100) < chance) {
                ItemStack backpackItem = BackpackItemFactory.create(tierConfig, null);
                event.getLoot().add(backpackItem);
                break;
            }
        }
    }
}
