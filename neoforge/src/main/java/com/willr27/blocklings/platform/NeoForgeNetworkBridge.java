package com.willr27.blocklings.platform;

import com.willr27.blocklings.BlocklingsConstants;
import com.willr27.blocklings.network.BlocklingNetworkPayload;
import com.willr27.blocklings.network.NetworkHandler;
import com.willr27.blocklings.network.MessageRegistry;
import net.neoforged.neoforge.network.PacketDistributor;
import com.willr27.blocklings.platform.services.INetworkBridge;
import com.willr27.blocklings.security.BlocklingPacketGuard;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge 1.21.1 payloads — server validates every client request before applying state.
 */
public final class NeoForgeNetworkBridge implements INetworkBridge {

    public static final ResourceLocation GUARD_PING_ID = ResourceLocation.fromNamespaceAndPath(BlocklingsConstants.MODID, "guard_ping");

    public record GuardPingPayload(int blocklingEntityId) implements CustomPacketPayload {
        public static final Type<GuardPingPayload> TYPE = new Type<>(GUARD_PING_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, GuardPingPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> buf.writeVarInt(payload.blocklingEntityId()),
                        buf -> new GuardPingPayload(buf.readVarInt())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @Override
    public void registerPackets() {
        BlocklingsConstants.LOG.info("Blocklings: NeoForge network bridge ready (payload registrar on mod bus)");
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(BlocklingsConstants.MODID).versioned("1");

        registrar.playToServer(GuardPingPayload.TYPE, GuardPingPayload.STREAM_CODEC, NeoForgeNetworkBridge::handleGuardPing);

        registrar.playBidirectional(
                BlocklingNetworkPayload.TYPE,
                BlocklingNetworkPayload.STREAM_CODEC,
                NeoForgeNetworkBridge::handleBlocklingMessage
        );
    }

    private static void handleGuardPing(GuardPingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlocklingPacketGuard.allowPacket(player, payload.blocklingEntityId());
        });
    }

    private static void handleBlocklingMessage(BlocklingNetworkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> NetworkHandler.handlePayload(payload, context.player()));
    }

    @Override
    public void sendToServer(Object message) {
        if (message instanceof com.willr27.blocklings.network.Message blocklingMessage) {
            PacketDistributor.sendToServer(MessageRegistry.encode(blocklingMessage));
        }
    }

    @Override
    public void sendToPlayer(ServerPlayer player, Object message) {
        if (message instanceof com.willr27.blocklings.network.Message blocklingMessage) {
            PacketDistributor.sendToPlayer(player, MessageRegistry.encode(blocklingMessage));
        }
    }
}
