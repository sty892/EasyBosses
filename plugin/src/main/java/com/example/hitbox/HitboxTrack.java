package com.example.hitbox;

import org.joml.Vector3f;

public class HitboxTrack {
    public final String animationName;
    public final int lengthTicks;
    public final Frame[] frames;

    public HitboxTrack(String animationName, int lengthTicks) {
        this.animationName = animationName;
        this.lengthTicks = lengthTicks;
        this.frames = new Frame[lengthTicks + 1];
        for (int i = 0; i <= lengthTicks; i++) {
            this.frames[i] = new Frame();
        }
    }

    public static class Frame {
        public final java.util.List<ZoneSnapshot> zones = new java.util.ArrayList<>();
    }

    public static class ZoneSnapshot {
        public final String zone;
        public final Vector3f worldOffset;
        public final double width, height, depth;

        public ZoneSnapshot(String zone, Vector3f worldOffset, double width, double height, double depth) {
            this.zone = zone;
            this.worldOffset = worldOffset;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }
    }
}