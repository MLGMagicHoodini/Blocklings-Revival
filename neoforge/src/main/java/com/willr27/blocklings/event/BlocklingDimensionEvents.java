package com.willr27.blocklings.event;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.blockling.BlocklingDimensionTravel;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = Blocklings.MODID)
public class BlocklingDimensionEvents
{
    private static final Set<UUID> PENDING_DIMENSION_SYNC = new HashSet<>();

    @SubscribeEvent
    public static void onPlayerChangedDimension(@Nonnull PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }

        if (!(player.serverLevel() instanceof ServerLevel toLevel))
        {
            return;
        }

        ServerLevel fromLevel = player.server.getLevel(event.getFrom());
        if (fromLevel == null)
        {
            return;
        }

        BlocklingDimensionTravel.followOwnerToDimension(player, fromLevel, toLevel);
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(@Nonnull EntityJoinLevelEvent event)
    {
        if (!(event.getEntity() instanceof BlocklingEntity blockling) || blockling.level().isClientSide())
        {
            return;
        }

        PENDING_DIMENSION_SYNC.add(blockling.getUUID());
    }

    @SubscribeEvent
    public static void onBlocklingTick(@Nonnull EntityTickEvent.Post event)
    {
        if (!(event.getEntity() instanceof BlocklingEntity blockling))
        {
            return;
        }

        if (!PENDING_DIMENSION_SYNC.remove(blockling.getUUID()))
        {
            return;
        }

        if (blockling.level().isClientSide())
        {
            return;
        }

        BlocklingDimensionTravel.syncToTrackingPlayers(blockling);
    }

    @SubscribeEvent
    public static void onStartTracking(@Nonnull PlayerEvent.StartTracking event)
    {
        if (!(event.getTarget() instanceof BlocklingEntity blockling))
        {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }

        BlocklingDimensionTravel.syncToPlayer(blockling, player);
    }
}
