package com.willr27.blocklings.entity.blockling;

import com.willr27.blocklings.loader.BlocklingsRegistries;
import com.willr27.blocklings.network.NetworkHandler;
import com.willr27.blocklings.network.messages.BlocklingSpawnSyncMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Moves tamed blocklings between dimensions without losing inventory, tasks, stats or skills.
 */
public final class BlocklingDimensionTravel
{
    private BlocklingDimensionTravel()
    {
    }

    /**
     * Sends owned tamed blocklings from the player's previous dimension to their current one.
     */
    public static void followOwnerToDimension(@Nonnull ServerPlayer player, @Nonnull ServerLevel fromLevel, @Nonnull ServerLevel toLevel)
    {
        if (fromLevel == toLevel)
        {
            return;
        }

        List<BlocklingEntity> ownedBlocklings = findOwnedBlocklings(fromLevel, player.getUUID());
        Vec3 destination = player.position();

        for (BlocklingEntity blockling : ownedBlocklings)
        {
            if (toLevel.getEntity(blockling.getUUID()) instanceof BlocklingEntity)
            {
                blockling.remove(RemovalReason.DISCARDED);
                continue;
            }

            transferTo(blockling, toLevel, destination.x, destination.y, destination.z);
        }
    }

    /**
     * Finds a blockling by UUID on any loaded server level.
     */
    @Nullable
    public static BlocklingEntity findBlocklingOnServer(@Nonnull ServerLevel anyLevel, @Nonnull UUID blocklingUuid)
    {
        for (ServerLevel level : anyLevel.getServer().getAllLevels())
        {
            Entity entity = level.getEntity(blocklingUuid);
            if (entity instanceof BlocklingEntity blockling)
            {
                return blockling;
            }
        }

        return null;
    }

    /**
     * Transfers a blockling to another dimension, preserving all saved data and UUID.
     */
    public static void transferTo(@Nonnull BlocklingEntity blockling, @Nonnull ServerLevel targetLevel, double x, double y, double z)
    {
        if (blockling.level() == targetLevel)
        {
            blockling.teleportTo(x, y, z);
            syncToTrackingPlayers(blockling);
            return;
        }

        if (!(blockling.level() instanceof ServerLevel sourceLevel))
        {
            return;
        }

        UUID blocklingUuid = blockling.getUUID();
        if (targetLevel.getEntity(blocklingUuid) instanceof BlocklingEntity existing)
        {
            existing.teleportTo(x, y, z);
            syncToTrackingPlayers(existing);
            blockling.remove(RemovalReason.DISCARDED);
            return;
        }

        CompoundTag entityTag = new CompoundTag();
        blockling.saveWithoutId(entityTag);

        float yRot = blockling.getYRot();
        float xRot = blockling.getXRot();

        blockling.remove(RemovalReason.CHANGED_DIMENSION);

        BlocklingEntity recreated = BlocklingsRegistries.blocklingEntity().create(targetLevel);
        if (recreated == null)
        {
            return;
        }

        recreated.load(entityTag);
        recreated.setUUID(blocklingUuid);
        recreated.moveTo(x, y, z, yRot, xRot);
        targetLevel.addFreshEntity(recreated);

        syncToTrackingPlayers(recreated);
    }

    public static void syncToTrackingPlayers(@Nonnull BlocklingEntity blockling)
    {
        if (!(blockling.level() instanceof ServerLevel serverLevel))
        {
            return;
        }

        BlocklingSpawnSyncMessage message = new BlocklingSpawnSyncMessage(blockling);
        for (ServerPlayer trackingPlayer : serverLevel.players())
        {
            if (trackingPlayer.distanceToSqr(blockling) <= 128.0 * 128.0)
            {
                NetworkHandler.sendToClient(trackingPlayer, message);
            }
        }
    }

    public static void syncToPlayer(@Nonnull BlocklingEntity blockling, @Nonnull ServerPlayer player)
    {
        NetworkHandler.sendToClient(player, new BlocklingSpawnSyncMessage(blockling));
    }

    @Nonnull
    private static List<BlocklingEntity> findOwnedBlocklings(@Nonnull ServerLevel level, @Nonnull UUID ownerUuid)
    {
        List<BlocklingEntity> owned = new ArrayList<>();
        AABB searchArea = new AABB(
                -3.0E7, level.getMinBuildHeight(), -3.0E7,
                3.0E7, level.getMaxBuildHeight(), 3.0E7);

        for (BlocklingEntity blockling : level.getEntitiesOfClass(BlocklingEntity.class, searchArea))
        {
            if (blockling.isTame() && ownerUuid.equals(blockling.getOwnerUUID()))
            {
                owned.add(blockling);
            }
        }

        return owned;
    }
}
