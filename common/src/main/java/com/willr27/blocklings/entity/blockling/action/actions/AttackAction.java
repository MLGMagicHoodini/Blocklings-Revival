package com.willr27.blocklings.entity.blockling.action.actions;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingHand;
import com.willr27.blocklings.entity.blockling.action.BlocklingActions;
import com.willr27.blocklings.entity.blockling.attribute.attributes.EnumAttribute;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * An action used when a blockling attacks a target.
 * The target count for the InteractionHand(s) can be different to that of the attack action itself.
 */
public class AttackAction extends KnownTargetAction
{
    /**
     * The action for the InteractionHand(s).
     */
    @Nonnull
    private final KnownTargetAction handAction;

    /**
     * Which InteractionHand was most recently used to attack.
     */
    @Nonnull
    private final EnumAttribute<BlocklingHand> recentHand;

    /**
     * @param actions the blockling actions.
     * @param blockling the blockling.
     * @param key the key used to identify the action and for the underlying attribute.
     * @param targetCountSupplier the supplier used to get the target count.
     * @param handTargetCountSupplier the supplier used to get the target count for the InteractionHand(s).
     */
    public AttackAction(@Nonnull BlocklingActions actions, @Nonnull BlocklingEntity blockling, @Nonnull String key, @Nonnull Supplier<Float> targetCountSupplier, @Nonnull Supplier<Float> handTargetCountSupplier)
    {
        super(blockling, key, Authority.BOTH, targetCountSupplier);

        // Cap the animation to a minimum of 5 ticks.
        Supplier<Float> supplier = () -> handTargetCountSupplier.get() < 5.0f ? 5.0f : handTargetCountSupplier.get();
        handAction = actions.createAction(key + "_hand", Authority.BOTH, supplier, true);
        handAction.setCount(-1.0f, false);

        blockling.getStats().addAttribute(recentHand = new EnumAttribute<BlocklingHand>(UUID.randomUUID().toString(), key + "_recent_hand", blockling, BlocklingHand.class, BlocklingHand.BOTH, null, null, true));
    }

    /**
     * Starts the action if it is not already being performed.
     *
     * @param InteractionHand the InteractionHand to try start the action for.
     * @return true if the action was started.
     */
    public boolean tryStart(BlocklingHand InteractionHand)
    {
        if (super.tryStart())
        {
            start(InteractionHand);

            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Starts the action whether it's running or not.
     * Also sets the recent InteractionHand to the one given.
     *
     * @param InteractionHand the InteractionHand to start the action for.
     */
    public void start(BlocklingHand InteractionHand)
    {
        super.start();

        if (InteractionHand != BlocklingHand.NONE)
        {
            handAction.start();
        }

        recentHand.setValue(InteractionHand);
    }

    @Override
    public void start()
    {
        super.start();

        start(BlocklingHand.BOTH);
    }

    @Override
    public void stop()
    {
        super.stop();

        handAction.stop();
    }

    /**
     * @param InteractionHand the InteractionHand to check.
     * @return true if the given InteractionHand's action is running.
     */
    public boolean isRunning(BlocklingHand InteractionHand)
    {
        if ((InteractionHand == BlocklingHand.MAIN && (getRecentHand() == BlocklingHand.MAIN || getRecentHand() == BlocklingHand.BOTH))
         || (InteractionHand == BlocklingHand.OFF && (getRecentHand() == BlocklingHand.OFF || getRecentHand() == BlocklingHand.BOTH)))
        {
            return handAction.isRunning();
        }

        return false;
    }

    /**
     * @param InteractionHand the InteractionHand to check.
     * @return true if the given InteractionHand's action has finished.
     */
    public boolean isFinished(BlocklingHand InteractionHand)
    {
        if ((InteractionHand == BlocklingHand.MAIN && (getRecentHand() == BlocklingHand.MAIN || getRecentHand() == BlocklingHand.BOTH))
         || (InteractionHand == BlocklingHand.OFF && (getRecentHand() == BlocklingHand.OFF || getRecentHand() == BlocklingHand.BOTH)))
        {
            return handAction.isFinished();
        }

        return false;
    }

    /**
     * @return the percent through the current InteractionHand's action.
     */
    public float percentThroughHandAction()
    {
        return handAction.percentThroughAction();
    }

    /**
     * @param targetCount the target count.
     * @return the percent through the current InteractionHand's action.
     */
    public float percentThroughHandAction(float targetCount)
    {
        return handAction.percentThroughAction(targetCount);
    }

    /**
     * @return the most recent InteractionHand used to attack.
     */
    @Nonnull
    public BlocklingHand getRecentHand()
    {
        return recentHand.getValue();
    }
}
