package com.blockforge.dynamicbackpacks.commands;

import com.blockforge.dynamicbackpacks.DynamicBackpacks;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BackpackCommand implements CommandExecutor, TabCompleter {

    private final DynamicBackpacks plugin;

    public BackpackCommand(DynamicBackpacks plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        // /bp view <player> <slot> — admin view
        if (args[0].equalsIgnoreCase("view")) {
            return handleAdminView(sender, args);
        }

        // /bp <slot> — open own command backpack
        return handleOpenOwn(sender, args[0]);
    }

    private boolean handleOpenOwn(CommandSender sender, String slotArg) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can open backpacks.");
            return true;
        }

        if (!plugin.getConfigManager().isCommandEnabled()) {
            player.sendMessage(ChatColor.RED + "Command backpacks are disabled on this server.");
            return true;
        }

        int slot;
        try {
            slot = Integer.parseInt(slotArg);
        } catch (NumberFormatException e) {
            sendUsage(sender);
            return true;
        }

        if (slot < 1 || slot > 10) {
            player.sendMessage(ChatColor.RED + "Backpack slot must be between 1 and 10.");
            return true;
        }

        if (!player.hasPermission("dynamicbackpacks.command.bp." + slot)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to access backpack slot " + slot + ".");
            return true;
        }

        plugin.getBackpackManager().openCommandBackpack(player, slot);
        return true;
    }

    private boolean handleAdminView(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dynamicbackpacks.admin.view")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view other players' backpacks.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /bp view <player> <1-10>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' is not online.");
            return true;
        }

        int slot;
        try {
            slot = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Usage: /bp view <player> <1-10>");
            return true;
        }

        if (slot < 1 || slot > 10) {
            sender.sendMessage(ChatColor.RED + "Backpack slot must be between 1 and 10.");
            return true;
        }

        if (!(sender instanceof Player admin)) {
            sender.sendMessage(ChatColor.RED + "Only players can view backpacks.");
            return true;
        }

        plugin.getBackpackManager().adminViewCommandBackpack(admin, target, slot);
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Usage: /bp <1-10> | /bp view <player> <1-10>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if ("view".startsWith(partial) && sender.hasPermission("dynamicbackpacks.admin.view")) {
                completions.add("view");
            }
            for (int i = 1; i <= 10; i++) {
                String s = String.valueOf(i);
                if (s.startsWith(partial) && sender.hasPermission("dynamicbackpacks.command.bp." + i)) {
                    completions.add(s);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("view")) {
            String partial = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("view")) {
            String partial = args[2];
            for (int i = 1; i <= 10; i++) {
                String s = String.valueOf(i);
                if (s.startsWith(partial)) completions.add(s);
            }
        }
        return completions;
    }
}
