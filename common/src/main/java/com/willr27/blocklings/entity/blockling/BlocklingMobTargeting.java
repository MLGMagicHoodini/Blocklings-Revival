package com.willr27.blocklings.entity.blockling;

import com.willr27.blocklings.util.ReflectionHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.EnderMan;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Makes hostile mobs treat blocklings like players for targeting.
 * Vanilla monsters only target {@link net.minecraft.world.entity.player.Player} by default,
 * so pets are ignored — except incidental hits (e.g. skeleton arrows aimed at the owner).
 */
public final class BlocklingMobTargeting
{
    private BlocklingMobTargeting()
    {
    }

    /**
     * Adds a blockling target goal to a hostile mob if it does not already have one.
     */
    public static void tryAddBlocklingTargetGoal(@Nonnull Entity entity)
    {
        if (entity.level().isClientSide())
        {
            return;
        }

        if (!(entity instanceof Mob mob) || !(entity instanceof Enemy))
        {
            return;
        }

        // Endermen use stare/look mechanics; forcing a target goal breaks that.
        if (entity instanceof EnderMan || entity instanceof BlocklingEntity)
        {
            return;
        }

        GoalSelector targetSelector = getTargetSelector(mob);
        if (targetSelector == null)
        {
            return;
        }

        if (alreadyHasBlocklingTargetGoal(targetSelector))
        {
            return;
        }

        // Priority 2 matches Player targeting on most monsters (zombie, skeleton, etc.).
        targetSelector.addGoal(2, new BlocklingAttackableTargetGoal(mob));
    }

    @Nullable
    private static GoalSelector getTargetSelector(@Nonnull Mob mob)
    {
        // Protected on Mob; the common module cannot access it directly (AT/AW are loader-only).
        // Prefer the exact named field (NeoForge/mojmap). On Fabric the intermediary name differs
        // by version — and a hard-coded one previously pointed at the wrong field (a control),
        // causing a ClassCastException — so fall back to locating it by TYPE: Mob declares two
        // GoalSelector fields, goalSelector (index 0) then targetSelector (index 1).
        try
        {
            return ReflectionHelper.getPrivateValue(Mob.class, mob, "targetSelector");
        }
        catch (RuntimeException ignored)
        {
            return ReflectionHelper.getFieldByType(Mob.class, mob, GoalSelector.class, 1);
        }
    }

    private static boolean alreadyHasBlocklingTargetGoal(@Nonnull GoalSelector targetSelector)
    {
        return targetSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof BlocklingAttackableTargetGoal);
    }

    /**
     * Marker subclass so we can detect duplicate registration on entity reload.
     */
    public static final class BlocklingAttackableTargetGoal extends NearestAttackableTargetGoal<BlocklingEntity>
    {
        public BlocklingAttackableTargetGoal(@Nonnull Mob mob)
        {
            super(mob, BlocklingEntity.class, true);
        }
    }
}
