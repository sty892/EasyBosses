package com.example.boss;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;

public class BossDefinition {
    public final String id;
    public final String displayName;
    public final double baseHealth;
    public final double bbWidth;
    public final double bbHeight;

    public final Map<String, Double> hitboxMultipliers = new HashMap<>();

    public byte[] texturePng;
    public String geoJson;
    public String animationsJson;
    
    public Map<String, com.example.hitbox.HitboxTrack> hitboxTracks = new HashMap<>();

    public BossDefinition(String id, YamlConfiguration config) {
        this.id = id;
        this.displayName = config.getString("display_name", id);
        this.baseHealth = config.getDouble("base_health", 100.0);
        this.bbWidth = config.getDouble("bounding_box.width", 1.0);
        this.bbHeight = config.getDouble("bounding_box.height", 2.0);

        if (config.contains("hitbox_zones")) {
            for (String key : config.getConfigurationSection("hitbox_zones").getKeys(false)) {
                hitboxMultipliers.put(key, config.getDouble("hitbox_zones." + key + ".damage_multiplier", 1.0));
            }
        }
    }
    
    public int getAnimLength(String animName) {
        com.example.hitbox.HitboxTrack track = hitboxTracks.get(animName);
        if (track != null) {
            return track.lengthTicks / 20; // returns seconds conceptually? wait, lengthTicks is in ticks. The plan says: "int animLength = definition.getAnimLength(currentAnimation) * 20" - wait, if it multiplies by 20, then getAnimLength must return seconds!
        }
        return 1;
    }
}
