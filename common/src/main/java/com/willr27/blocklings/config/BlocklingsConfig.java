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

        /** Chance (0–1) that crouch+food evolves the natural type. Default matches original 25%. */
        public Supplier<Double> evolveSuccessChance = () -> 0.25D;

        /** Chance (0–1) that food without crouch changes the primary type. Default 25%. */
        public Supplier<Double> primaryTypeChangeChance = () -> 0.25D;

        @Nonnull
        public final BlocklingAbilityConfig abilities = new BlocklingAbilityConfig();

        @Nonnull
        public final BlocklingSpawnConfig spawn = new BlocklingSpawnConfig();

        public double minLeavesToLogRatio()
        {
            double value = defaultMinLeavesToLogRatio.get();
            return Math.max(BlocklingWoodcutGoal.MIN_MIN_LEAVES_TO_LOGS_RATIO,
                    Math.min(BlocklingWoodcutGoal.MAX_MIN_LEAVES_TO_LOGS_RATIO, value));
        }

        public double evolveChance()
        {
            return clamp01(evolveSuccessChance.get());
        }

        public double primaryChangeChance()
        {
            return clamp01(primaryTypeChangeChance.get());
        }

        private static double clamp01(double value)
        {
            if (Double.isNaN(value))
            {
                return 0.0D;
            }
            return Math.max(0.0D, Math.min(1.0D, value));
        }
    }

    public static class Client
    {
        public Supplier<Boolean> disableDirtyBlocklings = () -> true;
    }
}
