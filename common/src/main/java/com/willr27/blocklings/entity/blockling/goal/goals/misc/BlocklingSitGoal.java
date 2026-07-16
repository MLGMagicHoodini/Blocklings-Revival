package com.willr27.blocklings.entity.blockling.goal.goals.misc;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.goal.BlocklingGoal;
import com.willr27.blocklings.entity.blockling.task.BlocklingTasks;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Sets the blockling to sit while the Sit task is enabled.
 * Disable the Sit task to stand up and allow Follow / other MOVE goals again.
 * <p>
 * Registered at GoalSelector priority 1 (after Float) so MOVE/JUMP flags stay locked
 * regardless of Sit's position in the task list.
 */
public class BlocklingSitGoal extends BlocklingGoal
{
    /**
     * @param id the id associated with the goal's task.
     * @param blockling the blockling.
     * @param tasks the blockling tasks.
     */
    public BlocklingSitGoal(@Nonnull UUID id, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(id, blockling, tasks);

        setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse()
    {
        // Stay sitting for as long as the Sit task remains enabled.
        return super.canContinueToUse();
    }

    @Override
    public boolean isInterruptable()
    {
        return false;
    }

    @Override
    public void start()
    {
        super.start();
        forceSit();
    }

    @Override
    public void stop()
    {
        super.stop();

        blockling.setInSittingPose(false);
        blockling.setOrderedToSit(false);
        blockling.getNavigation().stop();
    }

    @Override
    public void tick()
    {
        super.tick();
        forceSit();
    }

    private void forceSit()
    {
        blockling.getNavigation().stop();
        blockling.getMoveControl().setWantedPosition(blockling.getX(), blockling.getY(), blockling.getZ(), 0.0);
        blockling.setDeltaMovement(0.0, blockling.getDeltaMovement().y, 0.0);

        if (!blockling.isInSittingPose())
        {
            blockling.setInSittingPose(true);
        }

        if (!blockling.isOrderedToSit())
        {
            blockling.setOrderedToSit(true);
        }
    }
}
