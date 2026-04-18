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

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage("§6Active Bosses:");
            for (BossInstance inst : plugin.getBossManager().activeInstances.values()) {
                sender.sendMessage(String.format("§7- §e%s §7(UUID: %s) §cHP: %.1f/%.1f",
                        inst.definition.displayName, inst.armorStandUUID, inst.currentHealth, inst.definition.baseHealth));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("kill")) {
            if (args.length > 1) {
                try {
                    java.util.UUID uuid = java.util.UUID.fromString(args[1]);
                    BossInstance inst = plugin.getBossManager().activeInstances.get(uuid);
                    if (inst != null) {
                        plugin.getBossManager().removeBoss(inst);
                        sender.sendMessage("§aBoss killed.");
                    } else {
                        sender.sendMessage("§cBoss with UUID " + uuid + " not found.");
                    }
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§cInvalid UUID.");
                }
            } else {
                int count = plugin.getBossManager().activeInstances.size();
                new java.util.ArrayList<>(plugin.getBossManager().activeInstances.values()).forEach(plugin.getBossManager()::removeBoss);
                sender.sendMessage("§aKilled " + count + " bosses.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("debug")) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;
            Entity target = player.getTargetEntity(10);
            
            BossInstance targetBoss = null;
            if (target != null) {
                for (BossInstance inst : plugin.getBossManager().activeInstances.values()) {
                    if (inst.hitboxEntityUUID.equals(target.getUniqueId()) || inst.armorStandUUID.equals(target.getUniqueId())) {
                        targetBoss = inst;
                        break;
                    }
                }
            }

            if (targetBoss == null) {
                player.sendMessage("§cNo boss targeted.");
                return true;
            }

            player.sendMessage("§aDebugging hitboxes for: " + targetBoss.definition.displayName);
            
            BossInstance finalBoss = targetBoss;
            new org.bukkit.scheduler.BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks++ > 100 || !finalBoss.active || !finalBoss.cachedStand.isValid()) {
                        this.cancel();
                        return;
                    }
                    
                    com.example.hitbox.HitboxTrack track = finalBoss.definition.hitboxTracks.get(finalBoss.currentAnimation);
                    if (track == null) return;
                    int animTick = finalBoss.animationTick;
                    if (animTick < 0 || animTick >= track.frames.length) return;
                    
                    com.example.hitbox.HitboxTrack.Frame frame = track.frames[animTick];
                    float yaw = (float) Math.toRadians(finalBoss.cachedStand.getLocation().getYaw());
                    double cosYaw = Math.cos(yaw);
                    double sinYaw = Math.sin(yaw);
                    org.bukkit.Location bossLoc = finalBoss.cachedStand.getLocation();

                    for (com.example.hitbox.HitboxTrack.ZoneSnapshot zone : frame.zones) {
                        double rotX = zone.worldOffset.x * cosYaw - zone.worldOffset.z * sinYaw;
                        double rotZ = zone.worldOffset.x * sinYaw + zone.worldOffset.z * cosYaw;
                        org.bukkit.Location center = bossLoc.clone().add(rotX, zone.worldOffset.y, rotZ);
                        
                        drawBox(player, center, zone.width, zone.height, zone.depth);
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
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

    private void drawBox(Player player, org.bukkit.Location center, double w, double h, double d) {
        for (double x = -w / 2; x <= w / 2; x += 0.5) {
            for (double y = -h / 2; y <= h / 2; y += 0.5) {
                for (double z = -d / 2; z <= d / 2; z += 0.5) {
                    int edgeCount = 0;
                    if (Math.abs(x) >= w / 2 - 0.1) edgeCount++;
                    if (Math.abs(y) >= h / 2 - 0.1) edgeCount++;
                    if (Math.abs(z) >= d / 2 - 0.1) edgeCount++;
                    if (edgeCount >= 2) {
                        player.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }
}