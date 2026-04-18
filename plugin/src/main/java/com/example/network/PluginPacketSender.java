package com.example.network;

import org.bukkit.entity.Player;
import java.util.Collection;

public class PluginPacketSender {
    public void sendSpawn(Collection<Player> players, int entityId, String bossId) {
        // To be implemented
    }

    public void sendAnimState(Collection<Player> players, int entityId, String animName, int startTick) {
        // To be implemented
    }

    public void sendDespawn(Collection<Player> players, int entityId) {
        // To be implemented
    }
}