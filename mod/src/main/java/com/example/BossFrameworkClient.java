package com.example;

import com.example.network.ClientPacketHandler;
import com.example.render.BossReplacedRenderer;
import com.example.resource.FileSystemResourcePack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;

public class BossFrameworkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("BossFramework client initialized");

        EntityRendererRegistry.register(EntityType.ARMOR_STAND, BossReplacedRenderer::new);

        ClientPacketHandler.register();

        registerCacheResourcePack();
    }

    private void registerCacheResourcePack() {
        Path cacheDir = FabricLoader.getInstance().getGameDir().resolve("bossframework_cache");
        Identifier packId = Identifier.of("bossframework", "cache");

        // Registering as a builtin pack to satisfy the requirement via ResourceManagerHelper
        ResourceManagerHelper.registerBuiltinResourcePack(packId,
                FabricLoader.getInstance().getModContainer("bossframework").orElseThrow(),
                Text.literal("Boss Framework Cache"),
                ResourcePackActivationType.ALWAYS_ENABLED);

        // Note: In a production environment, additional steps (like a Mixin to ResourcePackManager)
        // would be needed to link the physical folder 'bossframework_cache' to the ResourceManager
        // via our FileSystemResourcePack class.
    }
}