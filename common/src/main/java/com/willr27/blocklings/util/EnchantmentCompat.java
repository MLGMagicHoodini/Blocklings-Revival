package com.willr27.blocklings.util;

import com.willr27.blocklings.Blocklings;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Enchantment helpers that work with vanilla and modded enchants (Apotheosis, etc.).
 * Always prefers EnchantmentHelper APIs so datapack/mod effect components run, and never crashes.
 */
public final class EnchantmentCompat
{
    private EnchantmentCompat()
    {
    }

    /**
     * Extra melee damage from ALL damage-modifying enchantments on the stack.
     */
    public static float modifyAttackDamage(@Nonnull ItemStack stack, @Nonnull LivingEntity attacker, @Nonnull LivingEntity target)
    {
        if (stack.isEmpty() || !(attacker.level() instanceof ServerLevel serverLevel))
        {
            return 0.0f;
        }

        try
        {
            DamageSource source = attacker.damageSources().mobAttack(attacker);
            return Math.max(0.0f, EnchantmentHelper.modifyDamage(serverLevel, stack, target, source, 0.0f));
        }
        catch (Throwable t)
        {
            Blocklings.LOGGER.debug("modifyDamage failed for {}; using Sharpness fallback.", stack.getItem(), t);
            try
            {
                float sharpness = getLevel(stack, Enchantments.SHARPNESS);
                return sharpness > 0.0f ? 1.0f + sharpness * 0.5f : 0.0f;
            }
            catch (Throwable ignored)
            {
                return 0.0f;
            }
        }
    }

    /**
     * Knockback including Knockback enchant and modded knockback effects.
     */
    public static float modifyKnockback(@Nonnull ItemStack stack, @Nonnull LivingEntity attacker, @Nonnull Entity target, float baseKnockback)
    {
        if (stack.isEmpty() || !(attacker.level() instanceof ServerLevel serverLevel))
        {
            return baseKnockback + getLevel(stack, Enchantments.KNOCKBACK);
        }

        try
        {
            DamageSource source = attacker.damageSources().mobAttack(attacker);
            return Math.max(0.0f, EnchantmentHelper.modifyKnockback(serverLevel, stack, target, source, baseKnockback));
        }
        catch (Throwable t)
        {
            return baseKnockback + getLevel(stack, Enchantments.KNOCKBACK);
        }
    }

    /**
     * Applies durability loss with Unbreaking and any modded durability enchantments.
     *
     * @return actual damage applied to the item (>= 0). 0 means fully prevented.
     */
    public static int processDurabilityDamage(@Nonnull ItemStack stack, @Nonnull LivingEntity user, int baseDamage)
    {
        if (stack.isEmpty() || baseDamage <= 0 || !stack.isDamageableItem())
        {
            return 0;
        }

        if (!(user.level() instanceof ServerLevel serverLevel))
        {
            return baseDamage;
        }

        try
        {
            return Math.max(0, EnchantmentHelper.processDurabilityChange(serverLevel, stack, baseDamage));
        }
        catch (Throwable t)
        {
            // Classic Unbreaking fallback.
            int unbreaking = (int) getLevel(stack, Enchantments.UNBREAKING);
            if (unbreaking > 0 && user.getRandom().nextInt(unbreaking + 1) > 0)
            {
                return 0;
            }
            return baseDamage;
        }
    }

    /**
     * Mining/woodcutting/farming speed bonus from Efficiency and any destroy-speed already on the stack.
     * {@link ItemStack#getDestroySpeed} is the base; Efficiency is added (vanilla player does the same).
     */
    public static float enchantmentHarvestSpeedBonus(@Nonnull ItemStack stack)
    {
        try
        {
            int level = (int) getLevel(stack, Enchantments.EFFICIENCY);
            return level > 0 ? level * level + 1.0f : 0.0f;
        }
        catch (Throwable ignored)
        {
            return 0.0f;
        }
    }

    /**
     * Total harvest speed for a block: tool destroy speed + Efficiency-style bonus.
     * Safe for Apotheosis tools that put mining bonuses in item attributes / destroy speed.
     */
    public static float getHarvestSpeed(@Nonnull ItemStack stack, @Nonnull BlockState state)
    {
        try
        {
            float base = stack.getDestroySpeed(state);
            // If destroy speed already looks enchanted (Apotheosis sometimes folds bonuses in), still add
            // Efficiency only when the enchant is present — matches vanilla player dig speed.
            return Math.max(0.0f, base + enchantmentHarvestSpeedBonus(stack));
        }
        catch (Throwable ignored)
        {
            return 1.0f;
        }
    }

    /**
     * Fire Aspect level (for manual ignition). Modded fire-on-hit often also runs via {@link #doPostAttackEffects}.
     */
    public static int getFireAspectLevel(@Nonnull ItemStack stack)
    {
        return (int) getLevel(stack, Enchantments.FIRE_ASPECT);
    }

    /**
     * Runs post-attack enchantment effects (vanilla Fire Aspect hooks, Apotheosis on-hit, etc.).
     * Uses EnchantmentHelper when available; reflection fallback for mapping differences.
     */
    public static void doPostAttackEffects(@Nonnull LivingEntity attacker, @Nonnull Entity target, @Nonnull ItemStack weapon)
    {
        if (!(attacker.level() instanceof ServerLevel serverLevel) || weapon.isEmpty())
        {
            return;
        }

        DamageSource source = attacker.damageSources().mobAttack(attacker);

        // Try known 1.21 method names without hard compile dependency on every mapping.
        if (invokeStatic("doPostAttackEffects", serverLevel, target, source)
                || invokeStatic("doPostAttackEffectsWithItemSource", serverLevel, target, source, weapon)
                || invokeStatic("onTargetDamaged", serverLevel, target, source, weapon)
                || invokeStatic("doPostHurtEffects", serverLevel, attacker, target))
        {
            return;
        }

        // Minimal vanilla-compatible fallback already handled by caller (Fire Aspect ignite).
    }

    /**
     * Copy of {@code base} (keeps Apotheosis gems/affixes/components) with higher enchant levels from {@code other}.
     */
    @Nonnull
    public static ItemStack mergeEnchantmentsPreservingComponents(@Nonnull ItemStack base, @Nonnull ItemStack other)
    {
        if (base.isEmpty())
        {
            return other.isEmpty() ? ItemStack.EMPTY : other.copy();
        }
        if (other.isEmpty())
        {
            return base.copy();
        }

        ItemStack merged = base.copy();
        try
        {
            ItemEnchantments otherEnchants = other.getEnchantments();
            List<Holder<Enchantment>> holders = new ArrayList<>();
            for (Holder<Enchantment> holder : otherEnchants.keySet())
            {
                if (holder != null && (holder.isBound() || holder.unwrapKey().isPresent()))
                {
                    holders.add(holder);
                }
            }

            for (Holder<Enchantment> holder : holders)
            {
                try
                {
                    int baseLevel = merged.getEnchantments().getLevel(holder);
                    int otherLevel = otherEnchants.getLevel(holder);
                    int best = Math.max(baseLevel, otherLevel);
                    if (best > baseLevel)
                    {
                        EnchantmentHelper.updateEnchantments(merged, map -> map.set(holder, best));
                    }
                }
                catch (Throwable ignored)
                {
                    // Skip a single bad enchant (unbound Apotheosis/datapack holder).
                }
            }
        }
        catch (Throwable t)
        {
            Blocklings.LOGGER.debug("Could not merge enchantments; keeping base tool components.", t);
        }

        return merged;
    }

    public static float getLevel(@Nonnull ItemStack stack, @Nonnull net.minecraft.resources.ResourceKey<Enchantment> enchantment)
    {
        if (stack.isEmpty())
        {
            return 0.0f;
        }

        try
        {
            ItemEnchantments enchants = stack.getEnchantments();
            if (enchants.isEmpty())
            {
                return 0.0f;
            }

            for (Holder<Enchantment> holder : enchants.keySet())
            {
                if (holder != null && holder.is(enchantment))
                {
                    return enchants.getLevel(holder);
                }
            }
        }
        catch (Throwable ignored)
        {
        }

        return 0.0f;
    }

    private static boolean invokeStatic(@Nonnull String name, Object... args)
    {
        try
        {
            for (Method method : EnchantmentHelper.class.getMethods())
            {
                if (!method.getName().equals(name) || method.getParameterCount() != args.length)
                {
                    continue;
                }
                method.invoke(null, args);
                return true;
            }
        }
        catch (Throwable t)
        {
            Blocklings.LOGGER.debug("EnchantmentHelper.{} failed", name, t);
        }
        return false;
    }
}
