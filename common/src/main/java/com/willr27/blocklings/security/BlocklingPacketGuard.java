package com.willr27.blocklings.security;

import com.willr27.blocklings.BlocklingsConstants;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative guard against packet abuse (item duping, remote inventory edits).
 * Resolves blocklings by persistent UUID first — network entity IDs desync on CurseForge /
 * integrated server and caused false "not owner" rejects.
 */
public final class BlocklingPacketGuard {

    private static final int MAX_PACKETS_PER_SECOND = 40;
    /** Allow configuring a following pet from a short distance away (was 8 — too tight when stuck). */
    private static final double MAX_INTERACT_DISTANCE_SQ = 24.0D * 24.0D;
    private static final long WARN_COOLDOWN_MS = 30_000L;

    private static final Map<UUID, RateBucket> RATE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_DISTANCE_WARN_MS = new ConcurrentHashMap<>();

    private BlocklingPacketGuard() {
    }

    /**
     * @return the owned blockling if the packet is allowed, otherwise {@code null}.
     */
    @Nullable
    public static BlocklingEntity allowAndResolve(@Nonnull ServerPlayer player,
                                                  @Nullable UUID blocklingUuid,
                                                  int fallbackEntityId) {
        if (player == null) {
            return null;
        }
        RateBucket bucket = RATE.computeIfAbsent(player.getUUID(), ignored -> new RateBucket());
        if (!bucket.tryConsume()) {
            BlocklingsConstants.LOG.warn("Blocklings: rate limit — player {} packet flood", player.getGameProfile().getName());
            return null;
        }

        BlocklingEntity blockling = findBlockling(player, blocklingUuid, fallbackEntityId);
        if (blockling == null) {
            BlocklingsConstants.LOG.warn(
                    "Blocklings: rejected packet — {} no blockling for uuid={} id={}",
                    player.getGameProfile().getName(), blocklingUuid, fallbackEntityId);
            return null;
        }

        if (!isOwner(player, blockling)) {
            BlocklingsConstants.LOG.warn(
                    "Blocklings: rejected packet — {} is not owner of blockling {} (netId={})",
                    player.getGameProfile().getName(), blockling.getUUID(), blockling.getId());
            return null;
        }

        if (player.distanceToSqr(blockling) > MAX_INTERACT_DISTANCE_SQ) {
            long now = System.currentTimeMillis();
            Long last = LAST_DISTANCE_WARN_MS.get(player.getUUID());
            if (last == null || now - last >= WARN_COOLDOWN_MS) {
                LAST_DISTANCE_WARN_MS.put(player.getUUID(), now);
                BlocklingsConstants.LOG.warn(
                        "Blocklings: rejected packet — {} too far from blockling {}",
                        player.getGameProfile().getName(), blockling.getUUID());
            }
            return null;
        }

        return blockling;
    }

    /**
     * @deprecated Prefer {@link #allowAndResolve}; kept for call sites that only have a net id.
     */
    @Deprecated
    public static boolean allowPacket(ServerPlayer player, int blocklingEntityId) {
        return allowAndResolve(player, null, blocklingEntityId) != null;
    }

    @Nullable
    public static BlocklingEntity findBlockling(@Nonnull ServerPlayer player,
                                                @Nullable UUID blocklingUuid,
                                                int fallbackEntityId) {
        if (blocklingUuid != null && !isNil(blocklingUuid)) {
            Entity byUuid = player.serverLevel().getEntity(blocklingUuid);
            if (byUuid instanceof BlocklingEntity blockling) {
                return blockling;
            }
            for (ServerLevel level : player.server.getAllLevels()) {
                Entity elsewhere = level.getEntity(blocklingUuid);
                if (elsewhere instanceof BlocklingEntity blockling) {
                    return blockling;
                }
            }
        }

        Entity byId = player.serverLevel().getEntity(fallbackEntityId);
        return byId instanceof BlocklingEntity blockling ? blockling : null;
    }

    private static boolean isNil(@Nonnull UUID uuid) {
        return uuid.getMostSignificantBits() == 0L && uuid.getLeastSignificantBits() == 0L;
    }

    public static boolean isOwner(@Nonnull ServerPlayer player, @Nonnull BlocklingEntity blockling) {
        if (blockling.isOwnedBy(player)) {
            return true;
        }
        UUID ownerId = blockling.getOwnerUUID();
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    public static void clearPlayer(UUID playerId) {
        RATE.remove(playerId);
        LAST_DISTANCE_WARN_MS.remove(playerId);
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
