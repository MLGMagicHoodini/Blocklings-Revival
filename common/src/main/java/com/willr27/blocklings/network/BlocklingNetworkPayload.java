package com.willr27.blocklings.network;

import com.willr27.blocklings.BlocklingsConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BlocklingNetworkPayload(byte[] data) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BlocklingsConstants.MODID, "message");
    public static final Type<BlocklingNetworkPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, BlocklingNetworkPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.data.length);
                buf.writeBytes(payload.data);
            },
            buf -> {
                byte[] data = new byte[buf.readVarInt()];
                buf.readBytes(data);
                return new BlocklingNetworkPayload(data);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
