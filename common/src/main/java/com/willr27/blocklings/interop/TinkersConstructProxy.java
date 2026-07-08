package com.willr27.blocklings.interop;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

@Proxy(modid = "tconstruct")
public class TinkersConstructProxy extends ModProxy
{
    public static TinkersConstructProxy instance = new TinkersConstructProxy();

    @Nonnull
    public List<Item> findAllWeapons()
    {
        return new ArrayList<>();
    }

    public boolean isTinkersTool(@Nonnull Item item)
    {
        return false;
    }

    public boolean isToolBroken(@Nonnull ItemStack stack)
    {
        return false;
    }

    public boolean canToolHarvest(@Nonnull ItemStack stack, @Nonnull BlockState blockState)
    {
        return false;
    }

    public float getToolHarvestSpeed(@Nonnull ItemStack stack, @Nonnull BlockState blockState)
    {
        return 0.0f;
    }

    public boolean attackEntity(@Nonnull ItemStack stack, @Nonnull LivingEntity attackerLiving, @Nonnull InteractionHand hand,
                                @Nonnull Entity targetEntity, @Nonnull DoubleSupplier cooldownFunction, boolean isExtraAttack)
    {
        return false;
    }

    public boolean damageTool(@Nonnull ItemStack stack, int damage, @Nonnull LivingEntity entity)
    {
        return false;
    }
}
