package com.blockforge.dynamicbackpacks.database;

import com.blockforge.dynamicbackpacks.backpack.Backpack;
import com.blockforge.dynamicbackpacks.DynamicBackpacks;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Logger;

public class SQLiteDatabase implements DatabaseManager {

    private final DynamicBackpacks plugin;
    private final Logger log;
    private final String dbPath;
    private Connection connection;

    private static final String CREATE_ITEM_TABLE =
            "CREATE TABLE IF NOT EXISTS item_backpacks (" +
            "  uuid        TEXT PRIMARY KEY," +
            "  owner_uuid  TEXT NOT NULL," +
            "  tier        INTEGER NOT NULL," +
            "  contents    TEXT NOT NULL," +
            "  created_at  INTEGER NOT NULL," +
            "  updated_at  INTEGER NOT NULL" +
            ")";

    private static final String CREATE_COMMAND_TABLE =
            "CREATE TABLE IF NOT EXISTS command_backpacks (" +
            "  player_uuid TEXT NOT NULL," +
            "  slot        INTEGER NOT NULL," +
            "  tier        INTEGER NOT NULL," +
            "  contents    TEXT NOT NULL," +
            "  updated_at  INTEGER NOT NULL," +
            "  PRIMARY KEY (player_uuid, slot)" +
            ")";

    public SQLiteDatabase(DynamicBackpacks plugin, String dbFileName) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.dbPath = dataFolder.getAbsolutePath() + File.separator + dbFileName;
    }

    @Override
    public void initialize() throws Exception {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(CREATE_ITEM_TABLE);
            stmt.execute(CREATE_COMMAND_TABLE);
        }
        log.info("SQLite database initialized at: " + dbPath);
    }

    @Override
    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.warning("Error closing database: " + e.getMessage());
            }
        }
    }

    @Override
    public void saveItemBackpack(Backpack backpack) {
        // COALESCE preserves the original created_at on upsert
        String sql = "INSERT OR REPLACE INTO item_backpacks " +
                     "(uuid, owner_uuid, tier, contents, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, COALESCE((SELECT created_at FROM item_backpacks WHERE uuid=?), ?), ?)";
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, backpack.getUuid().toString());
            ps.setString(2, backpack.getOwnerUUID().toString());
            ps.setInt(3, backpack.getTier());
            ps.setString(4, serializeContents(backpack.getContents()));
            ps.setString(5, backpack.getUuid().toString());
            ps.setLong(6, now);
            ps.setLong(7, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.severe("Failed to save item backpack " + backpack.getUuid() + ": " + e.getMessage());
        }
    }

    @Override
    public Backpack loadItemBackpack(UUID uuid) {
        String sql = "SELECT owner_uuid, tier, contents FROM item_backpacks WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UUID ownerUUID = UUID.fromString(rs.getString("owner_uuid"));
                    int tier = rs.getInt("tier");
                    ItemStack[] contents = deserializeContents(rs.getString("contents"), tier);
                    return new Backpack(uuid, ownerUUID, tier, contents);
                }
            }
        } catch (SQLException e) {
            log.severe("Failed to load item backpack " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public void deleteItemBackpack(UUID uuid) {
        String sql = "DELETE FROM item_backpacks WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.severe("Failed to delete item backpack " + uuid + ": " + e.getMessage());
        }
    }

    @Override
    public void saveCommandBackpack(Backpack backpack) {
        String sql = "INSERT OR REPLACE INTO command_backpacks " +
                     "(player_uuid, slot, tier, contents, updated_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, backpack.getOwnerUUID().toString());
            ps.setInt(2, backpack.getCommandSlot());
            ps.setInt(3, backpack.getTier());
            ps.setString(4, serializeContents(backpack.getContents()));
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.severe("Failed to save command backpack for " + backpack.getOwnerUUID()
                    + " slot " + backpack.getCommandSlot() + ": " + e.getMessage());
        }
    }

    @Override
    public Backpack loadCommandBackpack(UUID playerUUID, int slot, int defaultTier) {
        String sql = "SELECT tier, contents FROM command_backpacks WHERE player_uuid = ? AND slot = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUUID.toString());
            ps.setInt(2, slot);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int tier = rs.getInt("tier");
                    ItemStack[] contents = deserializeContents(rs.getString("contents"), tier);
                    return new Backpack(playerUUID, slot, tier, contents);
                }
            }
        } catch (SQLException e) {
            log.severe("Failed to load command backpack for " + playerUUID
                    + " slot " + slot + ": " + e.getMessage());
        }
        return new Backpack(playerUUID, slot, defaultTier, new ItemStack[0]);
    }

    private String serializeContents(ItemStack[] contents) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("size", contents.length);
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                config.set("i" + i, contents[i]);
            }
        }
        return config.saveToString();
    }

    private ItemStack[] deserializeContents(String yaml, int tier) {
        if (yaml == null || yaml.isEmpty()) {
            return new ItemStack[0];
        }
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);
            int size = config.getInt("size", 0);
            ItemStack[] contents = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                contents[i] = config.getItemStack("i" + i, null);
            }
            return contents;
        } catch (Exception e) {
            log.severe("Failed to deserialize backpack contents: " + e.getMessage());
            return new ItemStack[0];
        }
    }
}
