package com.willr27.blocklings.entity.blockling;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import javax.annotation.Nonnull;

/**
 * Ground navigation that treats grass, ferns, and flowers as open
 * so small blocklings do not get stuck pathing through vegetation.
 */
public class BlocklingGroundPathNavigation extends GroundPathNavigation
{
    public BlocklingGroundPathNavigation(@Nonnull Mob mob, @Nonnull Level level)
    {
        super(mob, level);
    }

    @Override
    protected @Nonnull PathFinder createPathFinder(int maxVisitedNodes)
    {
        this.nodeEvaluator = new BlocklingWalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    private static final class BlocklingWalkNodeEvaluator extends WalkNodeEvaluator
    {
        @Override
        public @Nonnull PathType getPathType(@Nonnull PathfindingContext context, int x, int y, int z)
        {
            BlockState state = context.getBlockState(new BlockPos(x, y, z));

            if (isPassableVegetation(state))
            {
                return PathType.OPEN;
            }

            return super.getPathType(context, x, y, z);
        }

        private static boolean isPassableVegetation(@Nonnull BlockState state)
        {
            return state.is(Blocks.SHORT_GRASS)
                    || state.is(Blocks.TALL_GRASS)
                    || state.is(Blocks.FERN)
                    || state.is(Blocks.LARGE_FERN)
                    || state.is(BlockTags.FLOWERS);
        }
    }
}
