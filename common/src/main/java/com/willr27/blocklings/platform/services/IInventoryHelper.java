package com.willr27.blocklings.platform.services;

import com.willr27.blocklings.inventory.BlocklingItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IInventoryHelper
{
    @Nullable
    BlocklingItemHandler getItemHandler(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockEntity blockEntity, @Nonnull Direction direction);
}
