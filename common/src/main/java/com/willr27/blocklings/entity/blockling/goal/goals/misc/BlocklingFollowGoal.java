package com.willr27.blocklings.entity.blockling.goal.goals.misc;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.goal.BlocklingGoal;
import com.willr27.blocklings.entity.blockling.task.BlocklingTasks;
import com.willr27.blocklings.entity.blockling.task.config.range.IntRangeProperty;
import com.willr27.blocklings.util.BlocklingsTranslationTextComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Follows the blockling's owner when out of range.
 * Teleports when far (vanilla wolf behaviour) so vegetation / bad paths cannot strand it.
 */
public class BlocklingFollowGoal extends BlocklingGoal
{
    private static final float TELEPORT_DISTANCE = 12.0f;

    /**
     * Extra blocks beyond {@link #startDistance} before Follow starts.
     * Without this, work tasks (hunt/mine/…) at ~start range fight Follow and stutter
     * (same defaults/behaviour as upstream 1.18).
     */
    private static final int START_RANGE_SLACK = 1;

    private final double speedModifier = 1.0;

    @Nonnull
    private final IntRangeProperty stopDistance;

    @Nonnull
    private final IntRangeProperty startDistance;

    @Nonnull
    private final PathNavigation navigation;

    private LivingEntity owner;

    private int timeToRecalcPath;

    private float oldWaterCost;

    public BlocklingFollowGoal(@Nonnull UUID id, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(id, blockling, tasks);

        this.navigation = blockling.getNavigation();

        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));

        properties.add(startDistance = new IntRangeProperty(
                "590fb919-6ac7-4af7-98ec-6e01919782c1", this,
                BlocklingsTranslationTextComponent.of("task.property.follow_start_range.name"),
                BlocklingsTranslationTextComponent.of("task.property.follow_start_range.desc"),
                1, 20, 4));
        properties.add(stopDistance = new IntRangeProperty(
                "99d39a22-3abe-4109-b493-dcb922f0c08a", this,
                BlocklingsTranslationTextComponent.of("task.property.follow_stop_range.name"),
                BlocklingsTranslationTextComponent.of("task.property.follow_stop_range.desc"),
                1, 20, 2));
    }

    @Override
    public boolean canUse()
    {
        if (!super.canUse())
        {
            return false;
        }

        if (blockling.isOrderedToSit())
        {
            return false;
        }

        LivingEntity owner = blockling.getOwner();

        if (owner == null)
        {
            return false;
        }
        else if (owner.isSpectator())
        {
            return false;
        }
        else if (blockling.distanceToSqr(owner) < (double) (getEffectiveStartDistance() * getEffectiveStartDistance()))
        {
            return false;
        }
        else
        {
            this.owner = owner;

            return true;
        }
    }

    /**
     * Start following only after start range + slack so nearby work targets are reachable.
     */
    private int getEffectiveStartDistance()
    {
        return startDistance.getValue() + START_RANGE_SLACK;
    }

    @Override
    public boolean canContinueToUse()
    {
        if (!super.canContinueToUse())
        {
            return false;
        }

        if (owner == null || !owner.isAlive() || owner.isSpectator() || blockling.isOrderedToSit())
        {
            return false;
        }

        return blockling.distanceToSqr(owner) > (double) (stopDistance.getValue() * stopDistance.getValue());
    }

    @Override
    public void start()
    {
        super.start();

        timeToRecalcPath = 0;
        oldWaterCost = blockling.getPathfindingMalus(PathType.WATER);
        blockling.setPathfindingMalus(PathType.WATER, 0.0f);
    }

    @Override
    public void stop()
    {
        super.stop();

        owner = null;
        navigation.stop();
        blockling.setPathfindingMalus(PathType.WATER, oldWaterCost);
    }

    @Override
    public void tick()
    {
        super.tick();

        if (owner == null)
        {
            return;
        }

        blockling.getLookControl().setLookAt(owner, 10.0f, (float) blockling.getMaxHeadXRot());

        if (--timeToRecalcPath <= 0)
        {
            timeToRecalcPath = 10;

            if (!blockling.isLeashed() && !blockling.isPassenger())
            {
                if (blockling.distanceToSqr(owner) >= (double) (TELEPORT_DISTANCE * TELEPORT_DISTANCE))
                {
                    teleportToOwner();
                }
                else
                {
                    navigation.moveTo(owner, speedModifier);
                }
            }
        }
    }

    private void teleportToOwner()
    {
        BlockPos ownerPos = owner.blockPosition();

        for (int i = 0; i < 10; i++)
        {
            int dx = blockling.getRandom().nextInt(7) - 3;
            int dy = blockling.getRandom().nextInt(3) - 1;
            int dz = blockling.getRandom().nextInt(7) - 3;
            if (maybeTeleportTo(ownerPos.getX() + dx, ownerPos.getY() + dy, ownerPos.getZ() + dz))
            {
                return;
            }
        }
    }

    private boolean maybeTeleportTo(int x, int y, int z)
    {
        if (Math.abs(x - owner.getX()) < 2.0 && Math.abs(z - owner.getZ()) < 2.0)
        {
            return false;
        }

        if (!canTeleportTo(new BlockPos(x, y, z)))
        {
            return false;
        }

        blockling.moveTo(x + 0.5, y, z + 0.5, blockling.getYRot(), blockling.getXRot());
        navigation.stop();
        return true;
    }

    private boolean canTeleportTo(@Nonnull BlockPos pos)
    {
        BlockState below = blockling.level().getBlockState(pos.below());
        if (!below.isSolid())
        {
            return false;
        }

        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < 2; i++)
        {
            BlockState state = blockling.level().getBlockState(cursor);
            if (!state.getCollisionShape(blockling.level(), cursor).isEmpty())
            {
                return false;
            }
            cursor.move(0, 1, 0);
        }

        return true;
    }
}
