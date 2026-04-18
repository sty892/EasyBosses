package com.example.network;

import com.example.anim.BossAnimController;
import com.example.resource.BossResourceLoader;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ClientPacketHandler {

    public static void register() {
        PayloadTypeRegistry.playS2C().register(HelloPayload.TYPE, HelloPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SpawnPayload.TYPE, SpawnPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnimStatePayload.TYPE, AnimStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DespawnPayload.TYPE, DespawnPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(HelloPayload.TYPE, (payload, ctx) -> {
            ctx.client().execute(() -> {
                String serverIp = "127.0.0.1";
                if (ctx.client().getCurrentServerEntry() != null) {
                    serverIp = ctx.client().getCurrentServerEntry().address;
                }
                BossResourceLoader.loadFromServer(serverIp, payload.resourcePort());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SpawnPayload.TYPE, (payload, ctx) -> {
            ctx.client().execute(() -> {
                // Boss spawned, maybe mark its ID for the renderer
                BossAnimController.bossIds.put(payload.entityId(), payload.bossId());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(AnimStatePayload.TYPE, (payload, ctx) -> {
            ctx.client().execute(() -> {
                BossAnimController.setAnimation(payload.entityId(), payload.animName(), payload.startTick());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(DespawnPayload.TYPE, (payload, ctx) -> {
            ctx.client().execute(() -> {
                BossAnimController.states.remove(payload.entityId());
                BossAnimController.bossIds.remove(payload.entityId());
            });
        });
    }
}