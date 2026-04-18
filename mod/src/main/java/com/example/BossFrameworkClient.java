package com.example;

import com.example.network.ClientPacketHandler;
import com.example.render.BossReplacedRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.entity.EntityType;

public class BossFrameworkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("BossFramework client initialized");
        
        EntityRendererRegistry.register(EntityType.ARMOR_STAND, BossReplacedRenderer::new);
        
        ClientPacketHandler.register();
    }
}