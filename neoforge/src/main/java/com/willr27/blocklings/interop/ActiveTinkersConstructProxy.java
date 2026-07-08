package com.willr27.blocklings.interop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * Runtime-loaded when Tinkers Construct is present.
 * Stub implementation keeps compilation working without a compile-time TConstruct dependency.
 */
public class ActiveTinkersConstructProxy extends TinkersConstructProxy {
    @Nonnull
    @Override
    public List<Item> findAllWeapons() {
        return Collections.emptyList();
    }

    @Override
    public boolean isTinkersTool(@Nonnull Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("tconstruct");
    }

    @Override
    public boolean isToolBroken(@Nonnull ItemStack stack) {
        return stack.isEmpty() || stack.getDamageValue() >= stack.getMaxDamage();
    }

    @Override
    public boolean canToolHarvest(@Nonnull ItemStack stack, @Nonnull BlockState blockState) {
        return stack.isCorrectToolForDrops(blockState);
    }

    @Override
    public float getToolHarvestSpeed(@Nonnull ItemStack stack, @Nonnull BlockState blockState) {
        return stack.getDestroySpeed(blockState);
    }

    @Override
    public boolean attackEntity(@Nonnull ItemStack stack, @Nonnull LivingEntity attackerLiving, @Nonnull InteractionHand hand,
                                @Nonnull Entity targetEntity, @Nonnull DoubleSupplier cooldownFunction, boolean isExtraAttack) {
        return false;
    }

    @Override
    public boolean damageTool(@Nonnull ItemStack stack, int damage, @Nonnull LivingEntity entity) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            stack.hurtAndBreak(damage, serverLevel, entity, item -> {
            });
        }
        return stack.isEmpty();
    }
}
