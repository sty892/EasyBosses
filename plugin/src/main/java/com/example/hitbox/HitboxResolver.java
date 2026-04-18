package com.example.hitbox;

import com.example.boss.BossInstance;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.List;

public class HitboxResolver {

    public static String resolve(Player player, BossInstance boss) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        Vector origin = eyeLoc.toVector();

        HitboxTrack track = boss.definition.hitboxTracks.get(boss.currentAnimation);
        if (track == null) return null;

        int animTick = boss.animationTick;
        if (animTick < 0 || animTick >= track.frames.length) return null;

        HitboxTrack.Frame frame = track.frames[animTick];
        if (frame == null) return null;

        float yaw = (float) Math.toRadians(boss.cachedStand.getLocation().getYaw());
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);

        Location bossLoc = boss.cachedStand.getLocation();

        String bestZone = null;
        double minDistance = Double.MAX_VALUE;

        for (HitboxTrack.ZoneSnapshot zone : frame.zones) {
            double rotX = zone.worldOffset.x * cosYaw - zone.worldOffset.z * sinYaw;
            double rotZ = zone.worldOffset.x * sinYaw + zone.worldOffset.z * cosYaw;

            Location center = bossLoc.clone().add(rotX, zone.worldOffset.y, rotZ);

            BoundingBox aabb = BoundingBox.of(
                    center,
                    zone.width / 2.0,
                    zone.height / 2.0,
                    zone.depth / 2.0
            );

            // Ray-AABB intersection using Bukkit's built-in rayTrace method
            org.bukkit.util.RayTraceResult result = aabb.rayTrace(origin, direction, 6.0); // max distance 6 blocks
            
            if (result != null) {
                double dist = result.getHitPosition().distance(origin);
                if (dist < minDistance) {
                    minDistance = dist;
                    bestZone = zone.zone;
                }
            }
        }

        return bestZone;
    }
}