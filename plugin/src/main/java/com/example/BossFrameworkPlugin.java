package com.example;

import com.example.boss.BossManager;
import com.example.boss.BossRegistry;
import com.example.command.BossCommand;
import com.example.hitbox.DamageListener;
import com.example.http.ResourceServer;
import com.example.network.PluginPacketSender;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BossFrameworkPlugin extends JavaPlugin implements Listener {

    private BossRegistry registry;
    private BossManager manager;
    private ResourceServer resourceServer;
    private PluginPacketSender packetSender;
    private NamespacedKey bossIdKey;
    private int resourcePort = 25566;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        resourcePort = getConfig().getInt("resource-server.port", 25566);
        bossIdKey = new NamespacedKey(this, "boss_id");

        packetSender = new PluginPacketSender();
        registry = new BossRegistry(this);
        manager = new BossManager(this, registry, packetSender);
        resourceServer = new ResourceServer(this, registry);

        PacketEvents.getAPI().init();
        
        // Command
        getCommand("boss").setExecutor(new BossCommand(this));
        
        // Events
        getServer().getPluginManager().registerEvents(new DamageListener(manager), this);
        getServer().getPluginManager().registerEvents(this, this);

        reloadPlugin();
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.stop();
        if (resourceServer != null) resourceServer.stop();
        PacketEvents.getAPI().terminate();
    }

    public void reloadPlugin() {
        if (manager != null) manager.stop();
        if (resourceServer != null) resourceServer.stop();
        
        registry.loadAll();
        
        if (getConfig().getBoolean("resource-server.enabled", true)) {
            resourceServer.start(resourcePort);
        }
        
        manager.start();
        
        restoreBosses();
    }

    private void restoreBosses() {
        Bukkit.getWorlds().forEach(world -> {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (org.bukkit.entity.Entity entity : chunk.getEntities()) {
                    if (entity instanceof org.bukkit.entity.ArmorStand stand) {
                        String bossId = stand.getPersistentDataContainer().get(bossIdKey, org.bukkit.persistence.PersistentDataType.STRING);
                        if (bossId != null) {
                            manager.spawnBoss(stand, bossId);
                        }
                    }
                }
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (getConfig().getBoolean("resource-server.enabled", true)) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                packetSender.sendHello(player, resourcePort);
            }, 20L); // wait 1s
        }
    }

    public BossManager getBossManager() {
        return manager;
    }

    public NamespacedKey getBossIdKey() {
        return bossIdKey;
    }
}