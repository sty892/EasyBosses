package com.example.boss;

import com.example.BossFrameworkPlugin;
import com.example.network.PluginPacketSender;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class BossManager {
    private final BossFrameworkPlugin plugin;
    private final BossRegistry registry;
    private final PluginPacketSender packetSender;
    private BukkitTask tickTask;

    public final Map<UUID, BossInstance> activeInstances = new HashMap<>();
    public final Map<UUID, BossInstance> interactionToInstance = new HashMap<>();
    private final NamespacedKey bossIdKey;

    public BossManager(BossFrameworkPlugin plugin, BossRegistry registry, PluginPacketSender packetSender) {
        this.plugin = plugin;
        this.registry = registry;
        this.packetSender = packetSender;
        this.bossIdKey = new NamespacedKey(plugin, "boss_id");
    }

    public void start() {
        long interval = plugin.getConfig().getLong("performance.tick-interval", 2L);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, interval);
    }

    public void stop() {
        if (tickTask != null) tickTask.cancel();
        // remove volatile interactions
        for (BossInstance inst : activeInstances.values()) {
            if (inst.cachedInteraction != null) {
                inst.cachedInteraction.remove();
            }
        }
        activeInstances.clear();
        interactionToInstance.clear();
    }

    public BossInstance spawnBoss(ArmorStand stand, String bossId) {
        BossDefinition def = registry.getBoss(bossId);
        if (def == null) return null;

        stand.setGravity(false);
        stand.setArms(false);
        stand.setBasePlate(false);
        stand.getPersistentDataContainer().set(bossIdKey, PersistentDataType.STRING, bossId);

        Location loc = stand.getLocation();
        Interaction interaction = loc.getWorld().spawn(loc, Interaction.class, ent -> {
            ent.setInteractionWidth((float) def.bbWidth);
            ent.setInteractionHeight((float) def.bbHeight);
        });

        BossInstance inst = new BossInstance(def, stand, interaction);
        activeInstances.put(stand.getUniqueId(), inst);
        interactionToInstance.put(interaction.getUniqueId(), inst);

        // Send spawn packet
        Collection<Player> nearby = getNearbyPlayers(stand.getLocation(), 64);
        packetSender.sendSpawn(nearby, stand.getEntityId(), bossId);
        
        // Initial animation
        setAnimation(inst, "animation." + bossId + ".idle");

        return inst;
    }

    public void tick() {
        long tickInterval = plugin.getConfig().getLong("performance.tick-interval", 2L);
        long hitboxInterval = plugin.getConfig().getLong("performance.hitbox-update-interval", 2L);

        for (var iterator = activeInstances.values().iterator(); iterator.hasNext();) {
            BossInstance boss = iterator.next();

            if (boss.cachedStand == null || !boss.cachedStand.isValid()) {
                if (boss.cachedInteraction != null) {
                    interactionToInstance.remove(boss.cachedInteraction.getUniqueId());
                    boss.cachedInteraction.remove();
                }
                Collection<Player> nearby = getNearbyPlayers(boss.cachedStand != null ? boss.cachedStand.getLocation() : null, 64);
                packetSender.sendDespawn(nearby, boss.armorStandEntityId);
                iterator.remove();
                continue;
            }

            boss.animationTick += (int) tickInterval;
            int animLengthTicks = boss.definition.getAnimLength(boss.currentAnimation);
            
            if (boss.animationTick >= animLengthTicks) {
                boss.animationTick = 0; // loop
            }

            // Sync interaction pos based on interval
            if (Bukkit.getCurrentTick() % hitboxInterval < tickInterval) {
                boss.cachedInteraction.teleport(boss.cachedStand);
            }

            // Cooldowns
            boss.attackCooldowns.replaceAll((k, v) -> v > 0 ? v - (int) tickInterval : 0);
        }
    }

    public void setAnimation(BossInstance boss, String newAnim) {
        if (boss.currentAnimation.equals(newAnim)) return;
        boss.currentAnimation = newAnim;
        boss.animationTick = 0;
        
        Collection<Player> nearby = getNearbyPlayers(boss.cachedStand.getLocation(), 64);
        packetSender.sendAnimState(nearby, boss.armorStandEntityId, newAnim, 0);
    }

    public void removeBoss(BossInstance boss) {
        activeInstances.remove(boss.armorStandUUID);
        if (boss.cachedInteraction != null) {
            interactionToInstance.remove(boss.cachedInteraction.getUniqueId());
            boss.cachedInteraction.remove();
        }
        
        Collection<Player> nearby = getNearbyPlayers(boss.cachedStand.getLocation(), 64);
        packetSender.sendDespawn(nearby, boss.armorStandEntityId);
        
        if (boss.cachedStand != null) boss.cachedStand.remove();
    }

    public Collection<Player> getNearbyPlayers(Location loc, double radius) {
        if (loc == null || loc.getWorld() == null) return java.util.Collections.emptyList();
        return loc.getWorld().getNearbyEntities(loc, radius, radius, radius).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .collect(Collectors.toList());
    }
}