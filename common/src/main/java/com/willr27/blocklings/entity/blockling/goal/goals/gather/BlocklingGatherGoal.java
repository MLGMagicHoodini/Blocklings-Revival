package com.willr27.blocklings.entity.blockling.goal.goals.gather;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingHand;
import com.willr27.blocklings.entity.blockling.goal.BlocklingTargetGoal;
import com.willr27.blocklings.entity.blockling.skill.skills.GeneralSkills;
import com.willr27.blocklings.entity.blockling.task.BlocklingTasks;
import com.willr27.blocklings.util.ToolContext;
import com.willr27.blocklings.util.ToolType;
import com.willr27.blocklings.util.ToolUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Contains common behaviour shared between gathering goals.
 */
public abstract class BlocklingGatherGoal extends BlocklingTargetGoal<BlockPos>
{
    /**
     * How far (horizontally, from the owner) a gather target may be while the blockling has an
     * enabled Follow task. Keeps a tamed, following blockling working the area around its owner
     * instead of drifting tree-to-tree (or ore-to-ore) across the map and never coming back.
     */
    private static final double WORK_LEASH_FROM_OWNER = 10.0;

    /**
     * @param id the id associated with the owning task of this goal.
     * @param blockling the blockling the goal is assigned to.
     * @param tasks the associated tasks.
     */
    public BlocklingGatherGoal(@Nonnull UUID id, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(id, blockling, tasks);
    }

    /**
     * @return true if the pos is within the owner work-leash (or no leash applies: wild blockling,
     *         owner offline, or no enabled Follow task — i.e. a stationed worker).
     */
    protected boolean isWithinOwnerWorkLeash(@Nonnull BlockPos pos)
    {
        net.minecraft.world.entity.LivingEntity owner = blockling.getOwner();
        if (owner == null || !blockling.getTasks().hasEnabledFollowTask())
        {
            return true;
        }

        double dx = owner.getX() - (pos.getX() + 0.5);
        double dz = owner.getZ() - (pos.getZ() + 0.5);
        return (dx * dx + dz * dz) <= WORK_LEASH_FROM_OWNER * WORK_LEASH_FROM_OWNER;
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
    }

    @Override
    public void stop()
    {
        super.stop();

        setPathTargetPos(null, null);

        blockling.getActions().gather.stop();
    }

    @Override
    public void tickGoal()
    {
        // Tick to make sure isFinished() is only true for a single tick
        blockling.getActions().gather.tick(0.0f);

        if (isStuck())
        {
            blockling.getActions().gather.stop();

            markEntireTargetBad();
        }
        else if (isInRangeOfPathTargetPos())
        {
            tickGather();
        }
    }

    @Override
    public boolean tryRecalcTarget()
    {
        if (isTargetValid())
        {
            return true;
        }
        else
        {
            markTargetBad();
        }

        return false;
    }

    /**
     * Called every tick when in range of the path target pos.
     */
    protected void tickGather()
    {
        if (!hasMovedSinceLastRecalc())
        {
            blockling.getLookControl().setLookAt(getTarget().getX() + 0.5, getTarget().getY() + 0.5, getTarget().getZ() + 0.5);
        }

        // Equip a suitable tool from inventory before chopping (not only with AUTOSWITCH).
        if (!canHarvestTargetPos() || blockling.getSkills().getSkill(GeneralSkills.AUTOSWITCH).isBought())
        {
            blockling.getEquipment().trySwitchToBestTool(BlocklingHand.BOTH, new ToolContext(getToolType(), getTargetBlockState()));
        }
    }

    /**
     * @return true if the blockling can harvest the block at the target pos.
     */
    public boolean canHarvestTargetPos()
    {
        return canHarvestPos(getTarget());
    }

    /**
     * @param blockPos the block pos to test.
     * @return true if the blockling can harvest the block at the given pos.
     */
    public boolean canHarvestPos(@Nullable BlockPos blockPos)
    {
        if (blockPos == null)
        {
            return false;
        }

        BlockState blockState = world.getBlockState(blockPos);

        if (blockling.getEquipment().canHarvestBlockWithEquippedTools(blockState))
        {
            return true;
        }

        // Peek inventory for a usable tool without swapping (findBestToolsToSwitchTo mutates slots).
        return blockling.getEquipment().hasInventoryToolThatCanHarvest(getToolType(), blockState);
    }

    /**
     * @return the tool type used to harvest the targets.
     */
    @Nonnull
    protected abstract ToolType getToolType();

    @Override
    public boolean isValidTarget(@Nullable BlockPos target)
    {
        return isValidTargetPos(target) && isValidTargetBlock(world.getBlockState(target).getBlock()) && canHarvestPos(target);
    }

    /**
     * @param blockPos the pos to test.
     * @return true if the given pos is a valid target position.
     */
    protected boolean isValidTargetPos(@Nullable BlockPos blockPos)
    {
        return blockPos != null && !badTargets.contains(blockPos) && isWithinOwnerWorkLeash(blockPos);
    }

    /**
     * @param block the block to test.
     * @return true if the given block is a valid block.
     */
    protected abstract boolean isValidTargetBlock(@Nonnull Block block);

    @Override
    public void markBad(@Nonnull BlockPos target)
    {
        super.markBad(target);

        // Any position we have deemed to be bad is one we are no longer gathering
        // So make sure to reset any block break progress
        world.destroyBlockProgress(blockling.getId(), target, -1);
    }

    @Override
    protected void setPreviousTarget(@Nullable BlockPos target)
    {
        if (target != null && (getTarget() == null || !getTarget().equals(target)))
        {
            world.destroyBlockProgress(blockling.getId(), target, -1);
        }

        super.setPreviousTarget(target);
    }

    /**
     * @return the current target block.
     */
    @Nullable
    public Block getTargetBlock()
    {
        BlockState blockState = getTargetBlockState();

        return blockState != null ? blockState.getBlock() : null;
    }

    /**
     * @return the current target block state.
     */
    @Nullable
    public BlockState getTargetBlockState()
    {
        return hasTarget() ? world.getBlockState(getTarget()) : null;
    }
}
