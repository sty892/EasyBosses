package com.example.hitbox;

import com.example.boss.BossInstance;
import com.example.boss.BossManager;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageListener implements Listener {
    private final BossManager bossManager;

    public DamageListener(BossManager bossManager) {
        this.bossManager = bossManager;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Interaction)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Interaction interaction = (Interaction) event.getEntity();
        Player player = (Player) event.getDamager();

        // Find BossInstance by interaction UUID
        BossInstance boss = null;
        for (BossInstance inst : bossManager.activeInstances.values()) {
            if (inst.hitboxEntityUUID.equals(interaction.getUniqueId())) {
                boss = inst;
                break;
            }
        }

        if (boss != null) {
            event.setCancelled(true);

            String zone = HitboxResolver.resolve(player, boss);
            double baseDamage = event.getDamage();
            double multiplier = 1.0;

            if (zone != null && boss.definition.hitboxMultipliers.containsKey(zone)) {
                multiplier = boss.definition.hitboxMultipliers.get(zone);
                player.sendMessage("§aHit zone: " + zone + " (x" + multiplier + ")");
            } else {
                player.sendMessage("§7Hit body");
            }

            double finalDamage = baseDamage * multiplier;
            boss.currentHealth -= finalDamage;

            if (boss.currentHealth <= 0) {
                player.sendMessage("§cBoss defeated!");
                bossManager.removeBoss(boss);
            }
        }
    }
}