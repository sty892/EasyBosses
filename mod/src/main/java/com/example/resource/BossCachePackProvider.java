package com.example.resource;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackPosition;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public class BossCachePackProvider implements ResourcePackProvider {
    @Override
    public void register(Consumer<ResourcePackProfile> profileAdder) {
        Path cacheDir = FabricLoader.getInstance().getGameDir().resolve("bossframework_cache");
        ResourcePackInfo info = new ResourcePackInfo(
                "bossframework_cache",
                Text.literal("Boss Framework Cache"),
                ResourcePackSource.BUILTIN,
                Optional.empty()
        );

        ResourcePackProfile profile = ResourcePackProfile.create(
                info,
                new ResourcePackProfile.PackFactory() {
                    @Override
                    public net.minecraft.resource.ResourcePack open(ResourcePackInfo ignored) {
                        return new FileSystemResourcePack(info.id(), cacheDir);
                    }

                    @Override
                    public net.minecraft.resource.ResourcePack openWithOverlays(ResourcePackInfo ignored, ResourcePackProfile.Metadata metadata) {
                        return new FileSystemResourcePack(info.id(), cacheDir);
                    }
                },
                ResourceType.CLIENT_RESOURCES,
                new ResourcePackPosition(true, ResourcePackProfile.InsertionPosition.TOP, false)
        );
        
        if (profile != null) {
            profileAdder.accept(profile);
        }
    }
}
