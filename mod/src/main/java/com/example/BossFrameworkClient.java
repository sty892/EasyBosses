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
    }
}