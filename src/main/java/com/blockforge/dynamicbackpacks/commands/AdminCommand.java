package com.blockforge.dynamicbackpacks.commands;

import com.blockforge.dynamicbackpacks.DynamicBackpacks;
import com.blockforge.dynamicbackpacks.backpack.Backpack;
import com.blockforge.dynamicbackpacks.config.BackpackTierConfig;
import com.blockforge.dynamicbackpacks.item.BackpackItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final DynamicBackpacks plugin;

    public AdminCommand(DynamicBackpacks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload"  -> handleReload(sender);
            case "give"    -> handleGive(sender, args);
            case "restore" -> handleRestore(sender, args);
            case "update"  -> handleUpdate(sender);
            default        -> sendUsage(sender);
        }
        return true;
    }

    // /dbp reload
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("dynamicbackpacks.admin.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to reload the plugin.");
            return;
        }
        plugin.reload();
        sender.sendMessage(ChatColor.GREEN + "DynamicBackpacks reloaded successfully.");
    }

    // /dbp give <player> <tier>
    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dynamicbackpacks.admin.give")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to give backpacks.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /dbp give <player> <tier>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' is not online.");
            return;
        }

        int tier;
        try {
            tier = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid tier number.");
            return;
        }

        BackpackTierConfig config = plugin.getConfigManager().getTier(tier);
        if (config == null) {
            sender.sendMessage(ChatColor.RED + "Tier " + tier + " is not configured.");
            return;
        }

        ItemStack backpackItem = BackpackItemFactory.create(config, target.getUniqueId());
        UUID backpackUUID = BackpackItemFactory.getBackpackUUID(backpackItem);

        // Create empty DB entry immediately
        Backpack bp = new Backpack(backpackUUID, target.getUniqueId(), tier, new ItemStack[0]);
        plugin.getDatabaseManager().saveItemBackpack(bp);

        // Give the item
        target.getInventory().addItem(backpackItem);
        target.sendMessage(ChatColor.GREEN + "You received a Tier " + tier + " backpack!");
        sender.sendMessage(ChatColor.GREEN + "Gave " + target.getName() + " a Tier " + tier + " backpack.");
    }

    // /dbp restore <uuid> <player>
    private void handleRestore(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dynamicbackpacks.admin.restore")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to restore backpacks.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /dbp restore <backpack-uuid> <player>");
            return;
        }

        UUID backpackUUID;
        try {
            backpackUUID = UUID.fromString(args[1]);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid UUID: " + args[1]);
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[2] + "' is not online.");
            return;
        }

        Backpack bp = plugin.getDatabaseManager().loadItemBackpack(backpackUUID);
        if (bp == null) {
            sender.sendMessage(ChatColor.RED + "No backpack found with UUID: " + backpackUUID);
            return;
        }

        BackpackTierConfig config = plugin.getConfigManager().getTier(bp.getTier());
        if (config == null) {
            sender.sendMessage(ChatColor.RED + "Backpack tier " + bp.getTier() + " is not configured.");
            return;
        }

        ItemStack item = BackpackItemFactory.createForUUID(config, backpackUUID);
        target.getInventory().addItem(item);
        target.sendMessage(ChatColor.GREEN + "Your backpack has been restored!");
        sender.sendMessage(ChatColor.GREEN + "Restored backpack " + backpackUUID + " to " + target.getName() + ".");
    }

    private void handleUpdate(CommandSender sender) {
        if (!sender.hasPermission("dynamicbackpacks.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return;
        }

        int current = plugin.getUpdateChecker().getCurrentBuild();
        if (current == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Running a local build — no version to compare.");
            return;
        }

        sender.sendMessage(ChatColor.GRAY + "Checking for updates...");
        plugin.getUpdateChecker().checkAsync(latest -> {
            if (latest == null) {
                sender.sendMessage(ChatColor.RED + "Could not reach GitHub to check for updates.");
            } else if (latest > current) {
                sender.sendMessage(ChatColor.GREEN + "Update available! "
                        + ChatColor.YELLOW + "dev-" + current
                        + ChatColor.GREEN + " \u2192 "
                        + ChatColor.AQUA + "dev-" + latest);
                sender.sendMessage(ChatColor.GRAY + "https://github.com/Midnwave/DynamicBackpacks/releases/latest");
            } else {
                sender.sendMessage(ChatColor.GREEN + "You are on the latest build "
                        + ChatColor.AQUA + "(dev-" + current + ")" + ChatColor.GREEN + ".");
            }
        });
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "/dbp reload");
        sender.sendMessage(ChatColor.YELLOW + "/dbp give <player> <tier>");
        sender.sendMessage(ChatColor.YELLOW + "/dbp restore <uuid> <player>");
        sender.sendMessage(ChatColor.YELLOW + "/dbp update");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String sub : Arrays.asList("reload", "give", "restore", "update")) {
                if (sub.startsWith(partial)) completions.add(sub);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give") || sub.equals("restore")) {
                String partial = args[1].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(partial)) completions.add(p.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (int i = 1; i <= plugin.getConfigManager().getMaxTier(); i++) {
                completions.add(String.valueOf(i));
            }
        }
        return completions;
    }
}
