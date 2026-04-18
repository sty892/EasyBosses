package com.example.anim;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BossAnimController {
    public static final Map<Integer, String> bossIds = new ConcurrentHashMap<>();
    public static final Map<Integer, AnimState> states = new ConcurrentHashMap<>();

    public record AnimState(String animName, int startTick, long receivedAtMs) {}

    public static void setAnimation(int entityId, String animName, int startTick) {
        states.put(entityId, new AnimState(animName, startTick, System.currentTimeMillis()));
    }
}