package com.example.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DespawnPayload(int entityId) implements CustomPayload {
    public static final Id<DespawnPayload> TYPE = new Id<>(Identifier.of("bossframework", "despawn"));
    public static final PacketCodec<PacketByteBuf, DespawnPayload> CODEC = PacketCodec.of(DespawnPayload::write, DespawnPayload::new);

    public DespawnPayload(PacketByteBuf buf) {
        this(buf.readInt());
    }
    public void write(PacketByteBuf buf) {
        buf.writeInt(entityId);
    }
    @Override public Id<? extends CustomPayload> getId() { return TYPE; }
}