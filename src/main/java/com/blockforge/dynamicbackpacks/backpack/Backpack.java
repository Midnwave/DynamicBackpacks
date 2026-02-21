package com.blockforge.dynamicbackpacks.backpack;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Backpack {

    private final UUID uuid;
    private final UUID ownerUUID;
    private final int tier;
    private final BackpackType type;
    // For COMMAND type, this is the slot number (1-10); for ITEM type it's unused
    private final int commandSlot;
    private ItemStack[] contents;

    // Item backpack constructor
    public Backpack(UUID uuid, UUID ownerUUID, int tier, ItemStack[] contents) {
        this.uuid = uuid;
        this.ownerUUID = ownerUUID;
        this.tier = tier;
        this.type = BackpackType.ITEM;
        this.commandSlot = -1;
        this.contents = contents;
    }

    // Command backpack constructor
    public Backpack(UUID ownerUUID, int commandSlot, int tier, ItemStack[] contents) {
        this.uuid = null;
        this.ownerUUID = ownerUUID;
        this.tier = tier;
        this.type = BackpackType.COMMAND;
        this.commandSlot = commandSlot;
        this.contents = contents;
    }

    public UUID getUuid() { return uuid; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public int getTier() { return tier; }
    public BackpackType getType() { return type; }
    public int getCommandSlot() { return commandSlot; }
    public ItemStack[] getContents() { return contents; }
    public void setContents(ItemStack[] contents) { this.contents = contents; }
}
