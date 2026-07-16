package com.willr27.blocklings.mixin;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Thread-local hunt-loot capture state shared by Fabric mixins.
 */
final class HuntLootCapture
{
    private static final ThreadLocal<List<ItemStack>> DROPS = new ThreadLocal<>();
    private static final ThreadLocal<BlocklingEntity> KILLER = new ThreadLocal<>();

    private HuntLootCapture()
    {
    }

    static void begin(@Nonnull BlocklingEntity blockling, @Nonnull List<ItemStack> drops)
    {
        KILLER.set(blockling);
        DROPS.set(drops);
    }

    static void clear()
    {
        DROPS.remove();
        KILLER.remove();
    }

    @Nullable
    static List<ItemStack> drops()
    {
        return DROPS.get();
    }

    @Nullable
    static BlocklingEntity killer()
    {
        return KILLER.get();
    }
}
