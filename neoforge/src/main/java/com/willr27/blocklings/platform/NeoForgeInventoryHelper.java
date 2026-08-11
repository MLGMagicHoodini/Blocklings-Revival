package com.willr27.blocklings.platform;

import com.willr27.blocklings.inventory.BlocklingItemHandler;
import com.willr27.blocklings.inventory.ContainerItemHandlerAdapter;
import com.willr27.blocklings.platform.services.IInventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Resolves inventories through the NeoForge item handler capability, so any mod exposing one works
 * without Blocklings depending on it. Falls back to the vanilla {@link Container} interface.
 */
public final class NeoForgeInventoryHelper implements IInventoryHelper
{
    @Override
    @Nullable
    public BlocklingItemHandler getItemHandler(@Nonnull Level level, @Nonnull BlockPos pos, @Nullable BlockEntity blockEntity, @Nonnull Direction direction)
    {
        // Prefer the simple 3-arg query documented by NeoForge, then fall back.
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction);
        if (handler == null)
        {
            handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        }
        if (handler == null && blockEntity != null)
        {
            handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, blockEntity.getBlockState(), blockEntity, direction);
        }
        if (handler == null && blockEntity != null)
        {
            handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, blockEntity.getBlockState(), blockEntity, null);
        }

        if (handler != null)
        {
            return new NeoForgeItemHandlerAdapter(handler);
        }

        if (blockEntity instanceof Container container)
        {
            return new ContainerItemHandlerAdapter(container, direction);
        }

        return null;
    }

    private record NeoForgeItemHandlerAdapter(IItemHandler handler) implements BlocklingItemHandler
    {
        @Override
        public int getSlots()
        {
            return handler.getSlots();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return handler.getStackInSlot(slot);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate)
        {
            return handler.insertItem(slot, stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return handler.extractItem(slot, amount, simulate);
        }
    }
}
