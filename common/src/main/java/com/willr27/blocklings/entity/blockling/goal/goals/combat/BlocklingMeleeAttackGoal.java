package com.willr27.blocklings.entity.blockling.goal.goals.combat;

import com.willr27.blocklings.loader.BlocklingsRegistries;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingHand;
import com.willr27.blocklings.entity.blockling.goal.BlocklingTargetGoal;
import com.willr27.blocklings.entity.blockling.skill.skills.CombatSkills;
import com.willr27.blocklings.entity.blockling.skill.skills.GeneralSkills;
import com.willr27.blocklings.entity.blockling.task.BlocklingTasks;
import com.willr27.blocklings.entity.blockling.goal.config.whitelist.GoalWhitelist;
import com.willr27.blocklings.entity.blockling.goal.config.whitelist.Whitelist;
import com.willr27.blocklings.util.BlockUtil;
import com.willr27.blocklings.util.EntityUtil;
import com.willr27.blocklings.util.ToolContext;
import com.willr27.blocklings.util.ToolType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Contains common behaviour shared between melee attack goals.
 */
public abstract class BlocklingMeleeAttackGoal extends BlocklingTargetGoal<LivingEntity>
{
    /**
     * @param id the id associated with the goal's task.
     * @param blockling the blockling.
     * @param tasks the blockling tasks.
     */
    public BlocklingMeleeAttackGoal(@Nonnull UUID id, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(id, blockling, tasks);

        GoalWhitelist whitelist = new GoalWhitelist("540241cd-085a-4c1f-9e90-8aea973568a8", "targets", Whitelist.Type.ENTITY, this);
        whitelist.setIsUnlocked(blockling.getSkills().getSkill(CombatSkills.WHITELIST).isBought(), false);
        EntityUtil.VALID_ATTACK_TARGETS.get().keySet().forEach(type -> whitelist.put(type, true));
        whitelist.put(BlocklingsRegistries.blocklingEntity().builtInRegistryHolder().key().location(), false);
        whitelists.add(whitelist);
    }

    @Override
    public boolean canUse()
    {
        if (!super.canUse())
        {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse()
    {
        if (!super.canContinueToUse())
        {
            return false;
        }

        return true;
    }

    @Override
    public void start()
    {
        super.start();

        blockling.setAggressive(true);
        blockling.setTarget(getTarget());
    }

    @Override
    public void stop()
    {
        super.stop();

        blockling.setAggressive(false);
        blockling.setTarget(null);
    }

    @Override
    public void tickGoal()
    {
        if (isStuck())
        {
            markEntireTargetBad();

            return;
        }

        if (blockling.getSkills().getSkill(GeneralSkills.AUTOSWITCH).isBought())
        {
            blockling.getEquipment().trySwitchToBestTool(BlocklingHand.BOTH, new ToolContext(ToolType.WEAPON, getTarget()));
        }

        LivingEntity target = getTarget();

        if (isInRange(target))
        {
            BlocklingHand attackingHand = blockling.getEquipment().findAttackingHand();
            attackingHand = attackingHand == BlocklingHand.BOTH ? blockling.getActions().attack.getRecentHand() == BlocklingHand.OFF ? BlocklingHand.MAIN : BlocklingHand.OFF : attackingHand;

            if (blockling.getActions().attack.tryStart(attackingHand))
            {
                attack(target, attackingHand);
            }
        }
    }

    @Override
    protected boolean recalcPath(boolean force)
    {
        LivingEntity target = getTarget();
        if (target == null)
        {
            setPathTargetPos(null, null);
            return false;
        }

        if (isInRange(target))
        {
            // Already in melee range — keep standing and swinging; do not null the path as "stuck".
            setPathTargetPos(target.blockPosition(), null);
            return true;
        }

        if (isBadPathTargetPos(target.blockPosition()))
        {
            setPathTargetPos(null, null);
            return false;
        }

        Path path = blockling.getNavigation().createPath(target, 0);
        setPathTargetPos(target.blockPosition(), path);
        return path != null;
    }

    /**
     * Performs the necessary actions that occur when a blockling attacks its target.
     *
     * @param target the attack target.
     * @param attackingHand the attacking InteractionHand.
     */
    protected void attack(@Nonnull LivingEntity target, @Nonnull BlocklingHand attackingHand)
    {
        blockling.doHurtTarget(target);

        // Do not recalcPath here — a failed path after a hit used to mark the target bad
        // (skeletons/spiders), so combat stopped after one swing.
        blockling.wasLastAttackHunt = false;
    }

    /**
     * Keeps attacking the current target until it is no longer valid.
     */
    protected boolean retainCurrentTargetIfValid()
    {
        LivingEntity current = getTarget();
        if (current == null)
        {
            return false;
        }

        // Allow re-engaging a target that was only marked bad due to a transient path fail.
        badTargets.remove(current);
        return isValidTarget(current);
    }

    @Override
    protected void checkForAndHandleInvalidTargets()
    {
        if (hasTarget() && !isTargetValid())
        {
            markTargetBad();
        }
    }

    @Override
    public void markEntireTargetBad()
    {
        if (hasTarget())
        {
            markTargetBad();
        }
    }

    @Override
    protected boolean isValidPathTargetPos(@Nonnull BlockPos blockPos)
    {
        return true;
    }

    @Override
    public boolean isValidTarget(@Nullable LivingEntity entity)
    {
        if (entity == null)
        {
            return false;
        }

        if (entity == blockling)
        {
            return false;
        }

        if (entity == blockling.getOwner())
        {
            return false;
        }

        // Never attack other blocklings — even when the combat whitelist skill is locked
        // (a locked whitelist otherwise treats every entity as a valid target).
        if (entity instanceof BlocklingEntity)
        {
            return false;
        }

        // Never attack the owner's own tamed pets (their dog, horse, other companions, etc.).
        if (entity instanceof net.minecraft.world.entity.OwnableEntity ownable
                && blockling.getOwner() != null
                && blockling.getOwner().getUUID().equals(ownable.getOwnerUUID()))
        {
            return false;
        }

        if (entity.isDeadOrDying())
        {
            return false;
        }

        if (badTargets.contains(entity))
        {
            return false;
        }

        for (GoalWhitelist whitelist : whitelists)
        {
            // Keep unlock flag aligned with the combat whitelist skill.
            boolean skillBought = blockling.getSkills().getSkill(CombatSkills.WHITELIST).isBought();
            if (whitelist.isUnlocked() != skillBought)
            {
                whitelist.setIsUnlocked(skillBought, false);
            }

            if (whitelist.isEntryBlacklisted(entity))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    protected void setTarget(@Nullable LivingEntity target)
    {
        super.setTarget(target);

        blockling.setTarget(target, false);
    }

    /**
     * @return the attack range squared.
     */
    @Override
    public float getRangeSq()
    {
        return 2.5f * 2.5f;
    }

    private boolean isInRange(@Nonnull LivingEntity target)
    {
        return blockling.distanceToSqr(target.getX(), target.getY() + target.getBbHeight() / 2.0f, target.getZ()) < getRangeSq();
    }
}
