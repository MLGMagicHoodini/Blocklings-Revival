package com.willr27.blocklings.platform;

import com.willr27.blocklings.config.BlocklingAbilityConfig;
import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.entity.blockling.goal.goals.gather.BlocklingWoodcutGoal;
import com.willr27.blocklings.util.WorldUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Binds {@link BlocklingsConfig} suppliers from Fabric-side defaults.
 * NightConfig TOML files can be layered in later; defaults mirror NeoForge.
 */
public final class FabricConfigBridge {
    private FabricConfigBridge() {
    }

    public static void bind() {
        bindCommon();
        bindClient();
    }

    private static void bindCommon() {
        BlocklingsConfig.Common common = BlocklingsConfig.COMMON;

        common.additionalOres = listSupplier();
        common.excludedOres = listSupplier();
        common.defaultMinLeavesToLogRatio = () -> (double) WorldUtil.DEFAULT_MIN_LEAVES_TO_LOGS_RATIO;
        common.customTrees = listSupplier();
        common.additionalCrops = listSupplier();
        common.excludedCrops = listSupplier();

        bindAbilities(common.abilities);
    }

    private static void bindClient() {
        BlocklingsConfig.CLIENT.disableDirtyBlocklings = () -> true;
    }

    private static Supplier<List<? extends String>> listSupplier() {
        return () -> new ArrayList<>();
    }

    private static void bindAbilities(@Nonnull BlocklingAbilityConfig abilities) {
        abilities.enabled = () -> true;
        bindFamily(abilities.grass, 0.33D, 3, 60);
        bindFamily(abilities.dirt, 0.33D, 3, 60);
        bindFamily(abilities.wood, 0.15D, 4, 90);
        bindFamily(abilities.stone, 0.0D, 8, 120);
        bindFamily(abilities.iron, 0.0D, 4, 60);
        bindFamily(abilities.gold, 0.10D, 8, 90);
        bindFamily(abilities.diamond, 0.0D, 8, 120);
        bindFamily(abilities.emerald, 0.05D, 8, 90);
        bindFamily(abilities.lapis, 0.0D, 8, 90);
        bindFamily(abilities.obsidian, 0.0D, 6, 120);
        bindFamily(abilities.glowstone, 0.0D, 8, 60);
        bindFamily(abilities.quartz, 0.0D, 6, 60);
        bindFamily(abilities.netherite, 0.0D, 8, 180);
    }

    private static void bindFamily(
            @Nonnull BlocklingAbilityConfig.FamilyConfig family,
            double passiveChance,
            int radius,
            int cooldown) {
        family.passiveEnabled = () -> true;
        family.activeEnabled = () -> true;
        family.passiveChance = () -> passiveChance;
        family.radius = () -> radius;
        family.activeCooldownSeconds = () -> cooldown;
        family.activeDurationSeconds = () -> 20;
    }
}
