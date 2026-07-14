package com.willr27.blocklings.network;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.security.BlocklingPacketGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.willr27.blocklings.platform.Services;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Legacy message dispatch bridged to NeoForge payloads.
 * Server validates every client packet via {@link BlocklingPacketGuard}.
 */
public final class NetworkHandler {
    private NetworkHandler() {
    }

    public static void init() {
        MessageRegistry.init();
        Blocklings.LOGGER.info("Blocklings network initialized (server-authoritative, anti-dupe guard active)");
    }

    public static void sendToServer(@Nonnull Message message) {
        // Drop client-originated control packets for blocklings the local player does not own.
        // These would only be rejected server-side (and spam the log).
        if (message instanceof BlocklingMessage<?> blocklingMessage && !blocklingMessage.canSendFromCurrentSide()) {
            return;
        }
        Services.NETWORK.sendToServer(message);
    }

    public static void sendToClient(@Nonnull Player player, @Nonnull Message message) {
        if (player instanceof ServerPlayer serverPlayer) {
            Services.NETWORK.sendToPlayer(serverPlayer, message);
        }
    }

    public static void sendToAllClients(@Nonnull net.minecraft.world.level.Level world, @Nonnull Message message, @Nonnull List<Player> ignore) {
        for (Player player : world.players()) {
            if (!ignore.contains(player)) {
                sendToClient(player, message);
            }
        }
    }

    public static void sync(@Nonnull net.minecraft.world.level.Level world, @Nonnull Message message) {
        if (world.isClientSide()) {
            sendToServer(message);
        } else {
            sendToAllClients(world, message, new ArrayList<>());
        }
    }

    public static boolean validateBlocklingPacket(@Nonnull ServerPlayer player, int blocklingEntityId) {
        return BlocklingPacketGuard.allowPacket(player, blocklingEntityId);
    }

    public static void handlePayload(@Nonnull BlocklingNetworkPayload payload, @Nonnull Player player) {
        if (player == null) {
            return;
        }
        Message message = MessageRegistry.decode(payload);
        if (message == null) {
            return;
        }
        message.handle(player);
    }
}
