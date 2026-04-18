package com.example.boss;

import com.example.BossFrameworkPlugin;
import com.example.loader.BbmodelParser;
import com.example.loader.GeckoExporter;
import com.example.loader.HitboxTrackBuilder;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class BossRegistry {
    private final Map<String, BossDefinition> registry = new HashMap<>();
    private final BossFrameworkPlugin plugin;

    public BossRegistry(BossFrameworkPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        registry.clear();
        String folderName = plugin.getConfig().getString("bosses-folder", "bosses");
        File bossesDir = new File(plugin.getDataFolder(), folderName);
        if (!bossesDir.exists()) {
            bossesDir.mkdirs();
            return;
        }

        File[] subdirs = bossesDir.listFiles(File::isDirectory);
        if (subdirs == null) return;

        for (File dir : subdirs) {
            String bossId = dir.getName();
            File bossYml = new File(dir, "boss.yml");
            File bbModel = new File(dir, bossId + ".bbmodel");

            if (!bossYml.exists() || !bbModel.exists()) {
                plugin.getLogger().warning("Skipping boss " + bossId + ": missing boss.yml or " + bossId + ".bbmodel");
                continue;
            }

            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(bossYml);
                BossDefinition definition = new BossDefinition(bossId, config);

                String bbModelJson = Files.readString(bbModel.toPath());
                BbmodelParser.ParsedBbmodel parsed = BbmodelParser.parse(bbModelJson);

                definition.texturePng = parsed.textureBytes;
                definition.geoJson = GeckoExporter.exportGeo(parsed, bossId);
                definition.animationsJson = GeckoExporter.exportAnimations(parsed);
                definition.hitboxTracks = HitboxTrackBuilder.buildTracks(parsed);

                registry.put(bossId, definition);
                plugin.getLogger().info("Loaded boss: " + bossId);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load boss " + bossId);
                e.printStackTrace();
            }
        }
    }

    public BossDefinition getBoss(String id) {
        return registry.get(id);
    }
    
    public Map<String, BossDefinition> getAll() {
        return registry;
    }
}