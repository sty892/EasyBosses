package com.example.network;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class PluginPacketSender {

    private byte[] createHelloData(int resourcePort) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeInt(resourcePort);
        return baos.toByteArray();
    }

    public void sendHello(Player player, int resourcePort) {
        try {
            WrapperPlayServerPluginMessage packet = new WrapperPlayServerPluginMessage("bossframework:hello", createHelloData(resourcePort));
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private byte[] createSpawnData(int entityId, String bossId) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeInt(entityId);
        writeString(out, bossId);
        return baos.toByteArray();
    }

    public void sendSpawn(Collection<Player> players, int entityId, String bossId) {
        try {
            WrapperPlayServerPluginMessage packet = new WrapperPlayServerPluginMessage("bossframework:spawn", createSpawnData(entityId, bossId));
            for (Player p : players) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(p, packet);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private byte[] createAnimStateData(int entityId, String animName, int startTick) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeInt(entityId);
        writeString(out, animName);
        out.writeInt(startTick);
        return baos.toByteArray();
    }

    public void sendAnimState(Collection<Player> players, int entityId, String animName, int startTick) {
        try {
            WrapperPlayServerPluginMessage packet = new WrapperPlayServerPluginMessage("bossframework:anim_state", createAnimStateData(entityId, animName, startTick));
            for (Player p : players) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(p, packet);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private byte[] createDespawnData(int entityId) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        out.writeInt(entityId);
        return baos.toByteArray();
    }

    public void sendDespawn(Collection<Player> players, int entityId) {
        try {
            WrapperPlayServerPluginMessage packet = new WrapperPlayServerPluginMessage("bossframework:despawn", createDespawnData(entityId));
            for (Player p : players) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(p, packet);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void writeString(DataOutputStream out, String s) throws Exception {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private void writeVarInt(DataOutputStream out, int value) throws Exception {
        while ((value & -128) != 0) {
            out.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        out.writeByte(value);
    }
}