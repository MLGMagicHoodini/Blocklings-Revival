package com.willr27.blocklings.inventory;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Vanilla {@link Container} / {@link WorldlyContainer} adapter used when a loader capability is missing.
 */
public final class ContainerItemHandlerAdapter implements BlocklingItemHandler
{
    @Nonnull
    private final Container container;

    @Nullable
    private final Direction direction;

    public ContainerItemHandlerAdapter(@Nonnull Container container, @Nullable Direction direction)
    {
        this.container = container;
        this.direction = direction;
    }

    @Override
    public int getSlots()
    {
        return container.getContainerSize();
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot)
    {
        if (slot < 0 || slot >= container.getContainerSize())
        {
            return ItemStack.EMPTY;
        }

        return container.getItem(slot);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate)
    {
        if (stack.isEmpty() || slot < 0 || slot >= container.getContainerSize())
        {
            return stack;
        }

        if (container instanceof WorldlyContainer worldly && direction != null && !worldly.canPlaceItemThroughFace(slot, stack, direction))
        {
            return stack;
        }

        if (!container.canPlaceItem(slot, stack))
        {
            return stack;
        }

        ItemStack existing = container.getItem(slot);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack))
        {
            return stack;
        }

        int max = Math.min(stack.getMaxStackSize(), container.getMaxStackSize());
        int canInsert = max - existing.getCount();
        if (canInsert <= 0)
        {
            return stack;
        }

        int toInsert = Math.min(canInsert, stack.getCount());
        if (!simulate)
        {
            if (existing.isEmpty())
            {
                ItemStack placed = stack.copy();
                placed.setCount(toInsert);
                container.setItem(slot, placed);
            }
            else
            {
                existing.grow(toInsert);
                container.setItem(slot, existing);
            }
            container.setChanged();
        }

        if (toInsert >= stack.getCount())
        {
            return ItemStack.EMPTY;
        }

        ItemStack remainder = stack.copy();
        remainder.shrink(toInsert);
        return remainder;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (amount <= 0 || slot < 0 || slot >= container.getContainerSize())
        {
            return ItemStack.EMPTY;
        }

        ItemStack existing = container.getItem(slot);
        if (existing.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        if (container instanceof WorldlyContainer worldly && direction != null && !worldly.canTakeItemThroughFace(slot, existing, direction))
        {
            return ItemStack.EMPTY;
        }

        int toExtract = Math.min(amount, existing.getCount());
        ItemStack result = existing.copy();
        result.setCount(toExtract);

        if (!simulate)
        {
            existing.shrink(toExtract);
            container.setItem(slot, existing);
            container.setChanged();
        }

        return result;
    }
}
