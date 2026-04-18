package com.example.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AnimStatePayload(int entityId, String animName, int startTick) implements CustomPayload {
    public static final Id<AnimStatePayload> TYPE = new Id<>(Identifier.of("bossframework", "anim_state"));
    public static final PacketCodec<PacketByteBuf, AnimStatePayload> CODEC = PacketCodec.of(AnimStatePayload::write, AnimStatePayload::new);

    public AnimStatePayload(PacketByteBuf buf) {
        this(buf.readInt(), buf.readString(), buf.readInt());
    }
    public void write(PacketByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeString(animName);
        buf.writeInt(startTick);
    }
    @Override public Id<? extends CustomPayload> getId() { return TYPE; }
}