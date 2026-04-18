package com.example.resource;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.function.Consumer;

public class BossCachePackProvider implements ResourcePackProvider {
    @Override
    public void register(Consumer<ResourcePackProfile> profileAdder) {
        Path cacheDir = FabricLoader.getInstance().getGameDir().resolve("bossframework_cache");
        
        ResourcePackProfile profile = ResourcePackProfile.create(
                "bossframework_cache",
                Text.literal("Boss Framework Cache"),
                true,
                (name) -> new FileSystemResourcePack(name, cacheDir),
                ResourceType.CLIENT_RESOURCES,
                ResourcePackProfile.InsertionPosition.TOP,
                net.minecraft.resource.ResourcePackSource.BUILTIN
        );
        
        if (profile != null) {
            profileAdder.accept(profile);
        }
    }
}
