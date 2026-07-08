package com.willr27.blocklings.security;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Validates item-moving actions server-side. Clients may request; only the server mutates inventories.
 */
public final class BlocklingInventoryGuard {

    private BlocklingInventoryGuard() {
    }

    public static boolean canMoveItem(ServerPlayer player, ItemStack stack, int requestedCount) {
        if (stack.isEmpty() || requestedCount <= 0) {
            return false;
        }
        if (requestedCount > stack.getCount()) {
            return false;
        }
        if (requestedCount > stack.getMaxStackSize()) {
            return false;
        }
        return !player.isSpectator();
    }

    /**
     * Rejects impossible stack growth (classic duping signal).
     */
    public static boolean isPlausibleStackCount(ItemStack before, ItemStack after) {
        if (after.isEmpty()) {
            return true;
        }
        if (after.getCount() > after.getMaxStackSize()) {
            return false;
        }
        return after.getCount() <= before.getCount() + before.getMaxStackSize();
    }
}
