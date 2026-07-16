package com.willr27.blocklings.entity.blockling.goal.goals.misc;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.goal.BlocklingTargetGoal;
import com.willr27.blocklings.entity.blockling.task.BlocklingTasks;
import com.willr27.blocklings.util.EntityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Finds nearby untamed blocklings whose natural type matches this blockling's primary type,
 * then walks toward them so the owner can follow.
 */
public class BlocklingFindBlocklingsGoal extends BlocklingTargetGoal<BlocklingEntity>
{
    /**
     * Chunks to search in each direction from the blockling (~6 chunks ≈ 96 blocks).
     */
    private static final int CHUNK_RANGE = 6;

    /**
     * Owner must stay this close (blocks) for the goal to run.
     */
    private static final double OWNER_RANGE = 16.0;

    /**
     * @param id the id associated with the goal's task.
     * @param blockling the blockling.
     * @param tasks the blockling tasks.
     */
    public BlocklingFindBlocklingsGoal(@Nonnull UUID id, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(id, blockling, tasks);

        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse()
    {
        LivingEntity owner = blockling.getOwner();

        if (owner == null || owner.distanceToSqr(blockling) > OWNER_RANGE * OWNER_RANGE)
        {
            return false;
        }

        return super.canUse();
    }

    @Override
    public boolean tryRecalcTarget()
    {
        int chunkX = blockling.blockPosition().getX() >> 4;
        int chunkZ = blockling.blockPosition().getZ() >> 4;
        int worldHeight = world.getHeight();
        AABB baseBB = new AABB(0, 0, 0, 16, worldHeight, 16);

        BlocklingEntity closestBlockling = null;
        double closestDistanceSq = Double.MAX_VALUE;

        for (int i = chunkX - CHUNK_RANGE; i <= chunkX + CHUNK_RANGE; i++)
        {
            for (int j = chunkZ - CHUNK_RANGE; j <= chunkZ + CHUNK_RANGE; j++)
            {
                List<BlocklingEntity> blocklingsInChunk = world.getEntitiesOfClass(
                        BlocklingEntity.class,
                        baseBB.move(i * 16, 0, j * 16),
                        this::isValidTarget);

                for (BlocklingEntity chunkBlockling : blocklingsInChunk)
                {
                    double distanceSq = blockling.distanceToSqr(chunkBlockling);

                    if (distanceSq < closestDistanceSq)
                    {
                        closestBlockling = chunkBlockling;
                        closestDistanceSq = distanceSq;
                    }
                }
            }
        }

        setTarget(closestBlockling);

        return closestBlockling != null;
    }

    @Override
    protected void checkForAndHandleInvalidTargets()
    {
        if (hasTarget() && !isValidTarget(getTarget()))
        {
            markTargetBad();
        }
    }

    @Override
    public void markEntireTargetBad()
    {
        markTargetBad();
    }

    @Override
    public boolean isValidTarget(@Nullable BlocklingEntity target)
    {
        if (target == null || target == blockling || target.isDeadOrDying())
        {
            return false;
        }

        if (badTargets.contains(target))
        {
            return false;
        }

        // Only lead to wild blocklings — already owned pets are ignored.
        if (target.isTame())
        {
            return false;
        }

        // Match wild natural type to this blockling's current primary type.
        if (target.getNaturalBlocklingType() != blockling.getBlocklingType())
        {
            return false;
        }

        return true;
    }

    @Override
    protected void tickGoal()
    {
        BlocklingEntity target = getTarget();

        if (target == null)
        {
            return;
        }

        // Face the wild blockling so the owner can see where it is heading.
        blockling.getLookControl().setLookAt(target, 30.0f, 30.0f);
    }

    @Override
    protected boolean recalcPath(boolean force)
    {
        BlocklingEntity target = getTarget();

        if (target == null)
        {
            setPathTargetPos(null, null);
            return false;
        }

        BlockPos targetPos = target.blockPosition();

        // Already close enough — stand near them and keep looking.
        if (blockling.distanceToSqr(target) <= getRangeSq())
        {
            setPathTargetPos(targetPos, null);
            return true;
        }

        if (!force && isBadPathTargetPos(targetPos))
        {
            setPathTargetPos(null, null);
            return false;
        }

        Path path = EntityUtil.createPathTo(blockling, targetPos, getRangeSq());

        if (path == null)
        {
            // Fallback: vanilla entity pathing (better for moving targets).
            path = blockling.getNavigation().createPath(target, 0);
        }

        setPathTargetPos(targetPos, path);

        return path != null;
    }

    @Override
    protected boolean isValidPathTargetPos(@Nonnull BlockPos blockPos)
    {
        return true;
    }

    @Override
    public int getRecalcInterval()
    {
        return 40;
    }

    @Override
    public int getPathRecalcInterval()
    {
        return 20;
    }

    @Override
    public float getRangeSq()
    {
        // ~4 blocks — close enough to "show" the wild blockling to the owner.
        return 16.0f;
    }
}
