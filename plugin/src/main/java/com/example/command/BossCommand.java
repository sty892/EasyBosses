package com.example.command;

import com.example.BossFrameworkPlugin;
import com.example.boss.BossInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public class BossCommand implements CommandExecutor {
    private final BossFrameworkPlugin plugin;

    public BossCommand(BossFrameworkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bossframework.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /boss <assign|reload>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage("§aBossFramework reloaded!");
            return true;
        }

        if (args[0].equalsIgnoreCase("assign")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can assign bosses.");
                return true;
            }
            Player player = (Player) sender;
            if (args.length < 2) {
                player.sendMessage("§cUsage: /boss assign <id>");
                return true;
            }
            String bossId = args[1];
            
            Entity target = player.getTargetEntity(5);
            if (!(target instanceof ArmorStand)) {
                player.sendMessage("§cYou must be looking at an ArmorStand.");
                return true;
            }

            ArmorStand stand = (ArmorStand) target;
            if (stand.getPersistentDataContainer().has(plugin.getBossIdKey(), PersistentDataType.STRING)) {
                player.sendMessage("§cThis ArmorStand is already a boss.");
                return true;
            }

            BossInstance inst = plugin.getBossManager().spawnBoss(stand, bossId);
            if (inst == null) {
                player.sendMessage("§cBoss definition '" + bossId + "' not found.");
            } else {
                player.sendMessage("§aBoss '" + bossId + "' assigned successfully!");
            }
            return true;
        }

        sender.sendMessage("§cUnknown subcommand.");
        return true;
    }
}