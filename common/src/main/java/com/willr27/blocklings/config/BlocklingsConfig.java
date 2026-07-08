package com.willr27.blocklings.config;

import com.willr27.blocklings.entity.blockling.goal.goals.gather.BlocklingWoodcutGoal;
import com.willr27.blocklings.util.WorldUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Loader-populated config accessors. NeoForge binds ModConfigSpec; Fabric binds NightConfig.
 */
public final class BlocklingsConfig
{
    @Nonnull
    public static final Common COMMON = new Common();

    @Nonnull
    public static final Client CLIENT = new Client();

    private BlocklingsConfig()
    {
    }

    public static class Common
    {
        public Supplier<List<? extends String>> additionalOres = ArrayList::new;
        public Supplier<List<? extends String>> excludedOres = ArrayList::new;
        public Supplier<Double> defaultMinLeavesToLogRatio = () -> (double) WorldUtil.DEFAULT_MIN_LEAVES_TO_LOGS_RATIO;
        public Supplier<List<? extends String>> customTrees = ArrayList::new;
        public Supplier<List<? extends String>> additionalCrops = ArrayList::new;
        public Supplier<List<? extends String>> excludedCrops = ArrayList::new;

        @Nonnull
        public final BlocklingAbilityConfig abilities = new BlocklingAbilityConfig();

        public double minLeavesToLogRatio()
        {
            double value = defaultMinLeavesToLogRatio.get();
            return Math.max(BlocklingWoodcutGoal.MIN_MIN_LEAVES_TO_LOGS_RATIO,
                    Math.min(BlocklingWoodcutGoal.MAX_MIN_LEAVES_TO_LOGS_RATIO, value));
        }
    }

    public static class Client
    {
        public Supplier<Boolean> disableDirtyBlocklings = () -> true;
    }
}
