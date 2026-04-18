package com.example.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SpawnPayload(int entityId, String bossId) implements CustomPayload {
    public static final Id<SpawnPayload> TYPE = new Id<>(Identifier.of("bossframework", "spawn"));
    public static final PacketCodec<PacketByteBuf, SpawnPayload> CODEC = PacketCodec.of(SpawnPayload::write, SpawnPayload::new);

    public SpawnPayload(PacketByteBuf buf) {
        this(buf.readInt(), buf.readString());
    }
    public void write(PacketByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeString(bossId);
    }
    @Override public Id<? extends CustomPayload> getId() { return TYPE; }
}