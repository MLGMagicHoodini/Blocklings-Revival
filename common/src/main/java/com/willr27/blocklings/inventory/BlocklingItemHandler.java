package com.willr27.blocklings.inventory;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * Loader-neutral item handler (NeoForge {@code IItemHandler} / Fabric Transfer API adapter).
 */
public interface BlocklingItemHandler
{
    int getSlots();

    @Nonnull
    ItemStack getStackInSlot(int slot);

    @Nonnull
    ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate);

    @Nonnull
    ItemStack extractItem(int slot, int amount, boolean simulate);
}
