package com.willr27.blocklings.security;

import com.willr27.blocklings.BlocklingsConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative guard against packet abuse (item duping, remote inventory edits).
 * Inspired by FTB Lib-style client-trust mistakes: every mutation is validated on the dedicated server.
 */
public final class BlocklingPacketGuard {

    private static final int MAX_PACKETS_PER_SECOND = 40;
    private static final double MAX_INTERACT_DISTANCE_SQ = 64.0D; // 8 blocks

    private static final Map<UUID, RateBucket> RATE = new ConcurrentHashMap<>();

    private BlocklingPacketGuard() {
    }

    public static boolean allowPacket(ServerPlayer player, int blocklingEntityId) {
        if (player == null) {
            return false;
        }
        RateBucket bucket = RATE.computeIfAbsent(player.getUUID(), ignored -> new RateBucket());
        if (!bucket.tryConsume()) {
            BlocklingsConstants.LOG.warn("Blocklings: rate limit — player {} packet flood", player.getGameProfile().getName());
            return false;
        }
        Entity entity = player.serverLevel().getEntity(blocklingEntityId);
        if (!(entity instanceof TamableAnimal tamable)) {
            return false;
        }
        if (!isOwner(player, tamable)) {
            BlocklingsConstants.LOG.warn("Blocklings: rejected packet — {} is not owner of entity {}", player.getGameProfile().getName(), blocklingEntityId);
            return false;
        }
        if (player.distanceToSqr(entity) > MAX_INTERACT_DISTANCE_SQ) {
            BlocklingsConstants.LOG.warn("Blocklings: rejected packet — {} too far from blockling {}", player.getGameProfile().getName(), blocklingEntityId);
            return false;
        }
        return true;
    }

    public static boolean isOwner(ServerPlayer player, TamableAnimal tamable) {
        return tamable.isOwnedBy(player);
    }

    public static void clearPlayer(UUID playerId) {
        RATE.remove(playerId);
    }

    private static final class RateBucket {
        private long windowStartMs = System.currentTimeMillis();
        private int count;

        boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStartMs >= 1000L) {
                windowStartMs = now;
                count = 0;
            }
            count++;
            return count <= MAX_PACKETS_PER_SECOND;
        }
    }
}
