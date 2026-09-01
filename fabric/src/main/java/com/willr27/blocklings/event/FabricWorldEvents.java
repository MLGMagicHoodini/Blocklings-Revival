package com.willr27.blocklings.event;

import com.willr27.blocklings.world.PlayerPlacedLogs;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class FabricWorldEvents
{
    private FabricWorldEvents()
    {
    }

    public static void register()
    {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) ->
                PlayerPlacedLogs.unmark(world, pos));

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
        {
            if (world.isClientSide() || player.isSpectator())
            {
                return InteractionResult.PASS;
            }

            var server = world.getServer();
            if (server == null)
            {
                return InteractionResult.PASS;
            }

            BlockPos placed = hitResult.getBlockPos().relative(hitResult.getDirection());
            server.execute(() ->
            {
                BlockState state = world.getBlockState(placed);
                Block block = state.getBlock();
                PlayerPlacedLogs.markIfPlayerLog(world, placed, block, player);
            });
            return InteractionResult.PASS;
        });
    }
}
