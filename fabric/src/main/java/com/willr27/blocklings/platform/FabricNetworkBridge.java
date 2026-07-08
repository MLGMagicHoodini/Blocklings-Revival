package com.willr27.blocklings.platform;

import com.willr27.blocklings.BlocklingsConstants;
import com.willr27.blocklings.network.BlocklingNetworkPayload;
import com.willr27.blocklings.network.MessageRegistry;
import com.willr27.blocklings.network.NetworkHandler;
import com.willr27.blocklings.platform.services.INetworkBridge;
import com.willr27.blocklings.security.BlocklingPacketGuard;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric 1.21.1 payloads — server validates every client request before applying state.
 */
public final class FabricNetworkBridge implements INetworkBridge {

    public static final ResourceLocation GUARD_PING_ID =
            ResourceLocation.fromNamespaceAndPath(BlocklingsConstants.MODID, "guard_ping");

    public record GuardPingPayload(int blocklingEntityId) implements CustomPacketPayload {
        public static final Type<GuardPingPayload> TYPE = new Type<>(GUARD_PING_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, GuardPingPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> buf.writeVarInt(payload.blocklingEntityId()),
                        buf -> new GuardPingPayload(buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static boolean payloadsRegistered;

    @Override
    public void registerPackets() {
        registerPayloadTypes();
        registerServerReceivers();
        BlocklingsConstants.LOG.info("Blocklings: Fabric network bridge ready");
    }

    public static void registerClientReceivers() {
        registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(
                BlocklingNetworkPayload.TYPE,
                (payload, context) -> context.client().execute(
                        () -> NetworkHandler.handlePayload(payload, context.player())));
    }

    private static void registerPayloadTypes() {
        if (payloadsRegistered) {
            return;
        }
        payloadsRegistered = true;

        PayloadTypeRegistry.playC2S().register(GuardPingPayload.TYPE, GuardPingPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(BlocklingNetworkPayload.TYPE, BlocklingNetworkPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(BlocklingNetworkPayload.TYPE, BlocklingNetworkPayload.STREAM_CODEC);
    }

    private static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(
                GuardPingPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        BlocklingPacketGuard.allowPacket(player, payload.blocklingEntityId());
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(
                BlocklingNetworkPayload.TYPE,
                (payload, context) -> context.server().execute(
                        () -> NetworkHandler.handlePayload(payload, context.player())));
    }

    @Override
    public void sendToServer(Object message) {
        if (message instanceof com.willr27.blocklings.network.Message blocklingMessage) {
            ClientPlayNetworking.send(MessageRegistry.encode(blocklingMessage));
        }
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Object message) {
        if (message instanceof com.willr27.blocklings.network.Message blocklingMessage) {
            ServerPlayNetworking.send(player, MessageRegistry.encode(blocklingMessage));
        }
    }
}
