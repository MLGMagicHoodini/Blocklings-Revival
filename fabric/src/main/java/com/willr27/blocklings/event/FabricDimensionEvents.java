package com.willr27.blocklings.event;

import com.willr27.blocklings.entity.blockling.BlocklingDimensionTravel;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;

public final class FabricDimensionEvents {
    private FabricDimensionEvents() {
    }

    public static void register() {
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(FabricDimensionEvents::onPlayerChangedDimension);
        ServerEntityEvents.ENTITY_LOAD.register(FabricDimensionEvents::onEntityLoad);
        EntityTrackingEvents.START_TRACKING.register(FabricDimensionEvents::onStartTracking);
    }

    private static void onPlayerChangedDimension(@Nonnull ServerPlayer player, @Nonnull ServerLevel origin, @Nonnull ServerLevel destination) {
        BlocklingDimensionTravel.followOwnerToDimension(player, origin, destination);
    }

    private static void onEntityLoad(@Nonnull net.minecraft.world.entity.Entity entity, @Nonnull net.minecraft.world.level.Level world) {
        if (!(entity instanceof BlocklingEntity blockling) || world.isClientSide()) {
            return;
        }
        if (world.getServer() == null) {
            return;
        }
        world.getServer().execute(() -> BlocklingDimensionTravel.syncToTrackingPlayers(blockling));
    }

    private static void onStartTracking(@Nonnull net.minecraft.world.entity.Entity trackedEntity, @Nonnull net.minecraft.world.entity.player.Player trackingPlayer) {
        if (!(trackedEntity instanceof BlocklingEntity blockling)) {
            return;
        }
        if (!(trackingPlayer instanceof ServerPlayer player)) {
            return;
        }
        BlocklingDimensionTravel.syncToPlayer(blockling, player);
    }
}
