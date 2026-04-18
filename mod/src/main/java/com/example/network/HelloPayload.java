package com.example.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HelloPayload(int resourcePort) implements CustomPayload {
    public static final Id<HelloPayload> TYPE = new Id<>(Identifier.of("bossframework", "hello"));
    public static final PacketCodec<PacketByteBuf, HelloPayload> CODEC = PacketCodec.of(HelloPayload::write, HelloPayload::new);

    public HelloPayload(PacketByteBuf buf) {
        this(buf.readInt());
    }
    public void write(PacketByteBuf buf) {
        buf.writeInt(resourcePort);
    }
    @Override public Id<? extends CustomPayload> getId() { return TYPE; }
}