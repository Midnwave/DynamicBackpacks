package com.blockforge.dynamicbackpacks.database;

import com.blockforge.dynamicbackpacks.backpack.Backpack;

import java.util.UUID;

public interface DatabaseManager {

    void initialize() throws Exception;
    void shutdown();

    // Item backpacks (keyed by UUID)
    void saveItemBackpack(Backpack backpack);
    Backpack loadItemBackpack(UUID uuid);

    // Command backpacks (keyed by player UUID + slot)
    void saveCommandBackpack(Backpack backpack);
    Backpack loadCommandBackpack(UUID playerUUID, int slot, int defaultTier);
}
