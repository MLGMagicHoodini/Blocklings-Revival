package com.willr27.blocklings.platform;

import com.willr27.blocklings.config.BlocklingAbilityConfig;
import com.willr27.blocklings.config.BlocklingSpawnConfig;
import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.config.NeoForgeBlocklingAbilityConfig;
import com.willr27.blocklings.config.NeoForgeBlocklingsConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Supplier;

public final class NeoForgeConfigBridge
{
    private NeoForgeConfigBridge()
    {
    }

    public static void bind()
    {
        bindCommon();
        bindClient();
    }

    private static void bindCommon()
    {
        NeoForgeBlocklingsConfig.Common neo = NeoForgeBlocklingsConfig.COMMON;
        BlocklingsConfig.Common common = BlocklingsConfig.COMMON;

        common.additionalOres = asSupplier(neo.additionalOres);
        common.excludedOres = asSupplier(neo.excludedOres);
        common.defaultMinLeavesToLogRatio = asSupplier(neo.defaultMinLeavesToLogRatio);
        common.customTrees = asSupplier(neo.customTrees);
        common.additionalCrops = asSupplier(neo.additionalCrops);
        common.excludedCrops = asSupplier(neo.excludedCrops);

        common.evolveSuccessChance = asSupplier(neo.evolveSuccessChance);
        common.primaryTypeChangeChance = asSupplier(neo.primaryTypeChangeChance);

        bindSpawn(neo, common.spawn);
        bindAbilities(neo.abilities, common.abilities);
    }

    private static void bindSpawn(@Nonnull NeoForgeBlocklingsConfig.Common neo, @Nonnull BlocklingSpawnConfig spawn)
    {
        spawn.enabled = asSupplier(neo.spawnEnabled);
        spawn.nearbyCap = asSupplier(neo.nearbyCap);
        spawn.nearbyRadius = asSupplier(neo.nearbyRadius);
        spawn.preventDuplicateNearbyType = asSupplier(neo.preventDuplicateNearbyType);
        spawn.starterSpawnEnabled = asSupplier(neo.starterSpawnEnabled);
        spawn.starterSpawnCount = asSupplier(neo.starterSpawnCount);
        spawn.starterSpawnRadius = asSupplier(neo.starterSpawnRadius);
        spawn.starterSpawnDelayTicks = asSupplier(neo.starterSpawnDelayTicks);

        for (Map.Entry<String, NeoForgeBlocklingsConfig.Common.TypeSpawnConfig> entry : neo.typeSpawns.entrySet())
        {
            BlocklingSpawnConfig.TypeConfig typeConfig = spawn.forKey(entry.getKey());
            NeoForgeBlocklingsConfig.Common.TypeSpawnConfig neoType = entry.getValue();
            typeConfig.enabled = asSupplier(neoType.enabled);
            typeConfig.spawnWeight = asSupplier(neoType.spawnWeight);
            typeConfig.extraBiomes = asSupplier(neoType.extraBiomes);
            typeConfig.biomeMode = asSupplier(neoType.biomeMode);
        }
    }

    private static void bindClient()
    {
        BlocklingsConfig.CLIENT.disableDirtyBlocklings =
                asSupplier(NeoForgeBlocklingsConfig.CLIENT.disableDirtyBlocklings);
    }

    private static void bindAbilities(@Nonnull NeoForgeBlocklingAbilityConfig neo,
                                      @Nonnull BlocklingAbilityConfig common)
    {
        common.enabled = asSupplier(neo.enabled);
        bindFamily(neo.grass, common.grass);
        bindFamily(neo.dirt, common.dirt);
        bindFamily(neo.wood, common.wood);
        bindFamily(neo.stone, common.stone);
        bindFamily(neo.iron, common.iron);
        bindFamily(neo.gold, common.gold);
        bindFamily(neo.diamond, common.diamond);
        bindFamily(neo.emerald, common.emerald);
        bindFamily(neo.lapis, common.lapis);
        bindFamily(neo.obsidian, common.obsidian);
        bindFamily(neo.glowstone, common.glowstone);
        bindFamily(neo.quartz, common.quartz);
        bindFamily(neo.netherite, common.netherite);
    }

    private static void bindFamily(@Nonnull NeoForgeBlocklingAbilityConfig.FamilyConfig neo,
                                   @Nonnull BlocklingAbilityConfig.FamilyConfig common)
    {
        common.passiveEnabled = asSupplier(neo.passiveEnabled);
        common.activeEnabled = asSupplier(neo.activeEnabled);
        common.passiveChance = asSupplier(neo.passiveChance);
        common.radius = asSupplier(neo.radius);
        common.activeCooldownSeconds = asSupplier(neo.activeCooldownSeconds);
        common.activeDurationSeconds = asSupplier(neo.activeDurationSeconds);
    }

    private static <T> Supplier<T> asSupplier(@Nonnull ModConfigSpec.ConfigValue<T> value)
    {
        return value::get;
    }

    private static Supplier<Boolean> asSupplier(@Nonnull ModConfigSpec.BooleanValue value)
    {
        return value::get;
    }

    private static Supplier<Double> asSupplier(@Nonnull ModConfigSpec.DoubleValue value)
    {
        return value::get;
    }

    private static Supplier<Integer> asSupplier(@Nonnull ModConfigSpec.IntValue value)
    {
        return value::get;
    }
}
