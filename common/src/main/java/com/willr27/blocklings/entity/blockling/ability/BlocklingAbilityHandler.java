package com.willr27.blocklings.entity.blockling.ability;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.world.damagesource.DamageSource;

import javax.annotation.Nonnull;

/**
 * Server-side ability logic for one {@link TypeFamily}.
 * Register implementations in {@link BlocklingAbilityRegistry}.
 */
public interface BlocklingAbilityHandler
{
    @Nonnull
    TypeFamily family();

    @Nonnull
    BlocklingTypeProfile profile();

    /** Called every second while this family is active on the blockling. */
    default void tickPassive(@Nonnull BlocklingEntity blockling)
    {
    }

    /** Called every tick while this family is active (light, lava bridge, etc.). */
    default void tickEnvironmental(@Nonnull BlocklingEntity blockling)
    {
    }

    /** Slow tick for expensive passives (wood saplings, etc.). */
    default void tickPassiveSlow(@Nonnull BlocklingEntity blockling)
    {
    }

    default float onHurt(@Nonnull BlocklingEntity blockling, @Nonnull DamageSource source, float damage)
    {
        return damage;
    }

    default boolean tryNegateDamage(@Nonnull BlocklingEntity blockling, @Nonnull DamageSource source)
    {
        return false;
    }

    default boolean isFireImmune(@Nonnull BlocklingEntity blockling)
    {
        return false;
    }

    default boolean isKnockbackImmune(@Nonnull BlocklingEntity blockling)
    {
        return false;
    }

    /** @return true if the ability was activated. */
    default boolean activate(@Nonnull BlocklingEntity blockling)
    {
        return false;
    }

    default int activeCooldownTicks(@Nonnull BlocklingEntity blockling)
    {
        return BlocklingAbilityRegistry.defaultActiveCooldownTicks(family());
    }

    default void onRemove(@Nonnull BlocklingEntity blockling)
    {
    }
}
