package com.willr27.blocklings.platform;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.config.BlocklingAbilityConfig;
import com.willr27.blocklings.config.BlocklingSpawnConfig;
import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.entity.blockling.BlocklingType;
import com.willr27.blocklings.loader.LoaderEnvironment;
import com.willr27.blocklings.util.WorldUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Binds {@link BlocklingsConfig} from {@code config/blocklings-common.toml} on Fabric.
 */
public final class FabricConfigBridge {
    private static final String FILE_NAME = "blocklings-common.toml";

    @Nullable
    private static CommentedFileConfig fileConfig;

    private FabricConfigBridge() {
    }

    public static void bind() {
        Path path = LoaderEnvironment.getConfigDir().resolve(FILE_NAME);
        fileConfig = CommentedFileConfig.builder(path)
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build();
        fileConfig.load();
        ensureDefaults(fileConfig);
        fileConfig.save();

        bindCommon(fileConfig);
        bindClient(fileConfig);
        Blocklings.LOGGER.info("Loaded Fabric config from {}", path.toAbsolutePath());
    }

    private static void ensureDefaults(@Nonnull CommentedConfig config) {
        setComment(config, "Upgrade", "Food-based type upgrade. evolveRequiredFeeds: crouch+food feeds needed to evolve (higher = more resources). Default 16.");
        putIfAbsent(config, "Upgrade.evolveSuccessChance", 0.10D);
        putIfAbsent(config, "Upgrade.evolveRequiredFeeds", 16);
        putIfAbsent(config, "Upgrade.allowFeedEvolution", true);
        putIfAbsent(config, "Upgrade.tameSuccessChance", 0.10D);
        putIfAbsent(config, "Upgrade.primaryTypeChangeChance", 0.25D);

        setComment(config, "Spawn", "Natural/chunk spawn tuning.");
        putIfAbsent(config, "Spawn.enabled", true);
        putIfAbsent(config, "Spawn.nearbyCap", 6);
        putIfAbsent(config, "Spawn.nearbyRadius", 40.0D);
        putIfAbsent(config, "Spawn.preventDuplicateNearbyType", false);
        putIfAbsent(config, "Spawn.starterSpawnEnabled", false);
        putIfAbsent(config, "Spawn.starterSpawnCount", 5);
        putIfAbsent(config, "Spawn.starterSpawnRadius", 12);
        putIfAbsent(config, "Spawn.starterSpawnDelayTicks", 40);

        for (BlocklingType type : BlocklingType.TYPES) {
            String prefix = "Spawn.types." + type.key;
            putIfAbsent(config, prefix + ".enabled", true);
            putIfAbsent(config, prefix + ".spawnWeight", BlocklingSpawnConfig.defaultWeightFor(type));
            putIfAbsent(config, prefix + ".extraBiomes", new ArrayList<String>());
            putIfAbsent(config, prefix + ".biomeMode", "AND");
        }

        putIfAbsent(config, "Mining.additionalOres", new ArrayList<String>());
        putIfAbsent(config, "Mining.excludedOres", new ArrayList<String>());
        putIfAbsent(config, "Woodcutting.defaultMinLeavesToLogRatio", (double) WorldUtil.DEFAULT_MIN_LEAVES_TO_LOGS_RATIO);
        putIfAbsent(config, "Woodcutting.customTrees", new ArrayList<String>());
        putIfAbsent(config, "Farming.additionalCrops", new ArrayList<String>());
        putIfAbsent(config, "Farming.excludedCrops", new ArrayList<String>());

        putIfAbsent(config, "Abilities.enabled", true);
        ensureAbilityFamily(config, "grass", 0.33D, 3, 60);
        ensureAbilityFamily(config, "dirt", 0.33D, 3, 60);
        ensureAbilityFamily(config, "wood", 0.15D, 4, 90);
        ensureAbilityFamily(config, "stone", 0.0D, 8, 120);
        ensureAbilityFamily(config, "iron", 0.0D, 4, 60);
        ensureAbilityFamily(config, "gold", 0.10D, 8, 90);
        ensureAbilityFamily(config, "diamond", 0.0D, 8, 120);
        ensureAbilityFamily(config, "emerald", 0.05D, 8, 90);
        ensureAbilityFamily(config, "lapis", 0.0D, 8, 90);
        ensureAbilityFamily(config, "obsidian", 0.0D, 6, 120);
        ensureAbilityFamily(config, "glowstone", 0.0D, 8, 60);
        ensureAbilityFamily(config, "quartz", 0.0D, 6, 60);
        ensureAbilityFamily(config, "netherite", 0.0D, 8, 180);

        putIfAbsent(config, "Misc.disableDirtyBlocklings", true);
    }

    private static void ensureAbilityFamily(
            @Nonnull CommentedConfig config,
            @Nonnull String key,
            double passiveChance,
            int radius,
            int cooldown) {
        String prefix = "Abilities." + key;
        putIfAbsent(config, prefix + ".passiveEnabled", true);
        putIfAbsent(config, prefix + ".activeEnabled", true);
        putIfAbsent(config, prefix + ".passiveChance", passiveChance);
        putIfAbsent(config, prefix + ".radius", radius);
        putIfAbsent(config, prefix + ".activeCooldownSeconds", cooldown);
        putIfAbsent(config, prefix + ".activeDurationSeconds", 20);
    }

    private static void bindCommon(@Nonnull CommentedConfig config) {
        BlocklingsConfig.Common common = BlocklingsConfig.COMMON;

        common.additionalOres = listSupplier(config, "Mining.additionalOres");
        common.excludedOres = listSupplier(config, "Mining.excludedOres");
        common.defaultMinLeavesToLogRatio = () -> config.getOrElse("Woodcutting.defaultMinLeavesToLogRatio",
                (double) WorldUtil.DEFAULT_MIN_LEAVES_TO_LOGS_RATIO);
        common.customTrees = listSupplier(config, "Woodcutting.customTrees");
        common.additionalCrops = listSupplier(config, "Farming.additionalCrops");
        common.excludedCrops = listSupplier(config, "Farming.excludedCrops");

        common.evolveSuccessChance = () -> config.getOrElse("Upgrade.evolveSuccessChance", 0.10D);
        common.evolveRequiredFeeds = () -> config.getOrElse("Upgrade.evolveRequiredFeeds", 16);
        common.allowFeedEvolution = () -> config.getOrElse("Upgrade.allowFeedEvolution", true);
        common.tameSuccessChance = () -> config.getOrElse("Upgrade.tameSuccessChance", 0.10D);
        common.primaryTypeChangeChance = () -> config.getOrElse("Upgrade.primaryTypeChangeChance", 0.25D);

        BlocklingSpawnConfig spawn = common.spawn;
        spawn.enabled = () -> config.getOrElse("Spawn.enabled", true);
        spawn.nearbyCap = () -> config.getOrElse("Spawn.nearbyCap", 6);
        spawn.nearbyRadius = () -> config.getOrElse("Spawn.nearbyRadius", 40.0D);
        spawn.preventDuplicateNearbyType = () -> config.getOrElse("Spawn.preventDuplicateNearbyType", false);
        spawn.starterSpawnEnabled = () -> config.getOrElse("Spawn.starterSpawnEnabled", false);
        spawn.starterSpawnCount = () -> config.getOrElse("Spawn.starterSpawnCount", 5);
        spawn.starterSpawnRadius = () -> config.getOrElse("Spawn.starterSpawnRadius", 12);
        spawn.starterSpawnDelayTicks = () -> config.getOrElse("Spawn.starterSpawnDelayTicks", 40);

        for (BlocklingType type : BlocklingType.TYPES) {
            BlocklingSpawnConfig.TypeConfig typeConfig = spawn.forType(type);
            String prefix = "Spawn.types." + type.key;
            int defaultWeight = BlocklingSpawnConfig.defaultWeightFor(type);
            typeConfig.enabled = () -> config.getOrElse(prefix + ".enabled", true);
            typeConfig.spawnWeight = () -> config.getOrElse(prefix + ".spawnWeight", defaultWeight);
            typeConfig.extraBiomes = listSupplier(config, prefix + ".extraBiomes");
            typeConfig.biomeMode = () -> config.getOrElse(prefix + ".biomeMode", "AND");
        }

        bindAbilities(config, common.abilities);
    }

    private static void bindClient(@Nonnull CommentedConfig config) {
        BlocklingsConfig.CLIENT.disableDirtyBlocklings = () -> config.getOrElse("Misc.disableDirtyBlocklings", true);
    }

    private static void bindAbilities(@Nonnull CommentedConfig config, @Nonnull BlocklingAbilityConfig abilities) {
        abilities.enabled = () -> config.getOrElse("Abilities.enabled", true);
        bindFamily(config, abilities.grass, "grass", 0.33D, 3, 60);
        bindFamily(config, abilities.dirt, "dirt", 0.33D, 3, 60);
        bindFamily(config, abilities.wood, "wood", 0.15D, 4, 90);
        bindFamily(config, abilities.stone, "stone", 0.0D, 8, 120);
        bindFamily(config, abilities.iron, "iron", 0.0D, 4, 60);
        bindFamily(config, abilities.gold, "gold", 0.10D, 8, 90);
        bindFamily(config, abilities.diamond, "diamond", 0.0D, 8, 120);
        bindFamily(config, abilities.emerald, "emerald", 0.05D, 8, 90);
        bindFamily(config, abilities.lapis, "lapis", 0.0D, 8, 90);
        bindFamily(config, abilities.obsidian, "obsidian", 0.0D, 6, 120);
        bindFamily(config, abilities.glowstone, "glowstone", 0.0D, 8, 60);
        bindFamily(config, abilities.quartz, "quartz", 0.0D, 6, 60);
        bindFamily(config, abilities.netherite, "netherite", 0.0D, 8, 180);
    }

    private static void bindFamily(
            @Nonnull CommentedConfig config,
            @Nonnull BlocklingAbilityConfig.FamilyConfig family,
            @Nonnull String key,
            double passiveChance,
            int radius,
            int cooldown) {
        String prefix = "Abilities." + key;
        family.passiveEnabled = () -> config.getOrElse(prefix + ".passiveEnabled", true);
        family.activeEnabled = () -> config.getOrElse(prefix + ".activeEnabled", true);
        family.passiveChance = () -> config.getOrElse(prefix + ".passiveChance", passiveChance);
        family.radius = () -> config.getOrElse(prefix + ".radius", radius);
        family.activeCooldownSeconds = () -> config.getOrElse(prefix + ".activeCooldownSeconds", cooldown);
        family.activeDurationSeconds = () -> config.getOrElse(prefix + ".activeDurationSeconds", 20);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private static Supplier<List<? extends String>> listSupplier(@Nonnull CommentedConfig config, @Nonnull String path) {
        return () -> {
            Object value = config.get(path);
            if (value instanceof List<?> list) {
                return (List<? extends String>) list;
            }
            return new ArrayList<>();
        };
    }

    private static void putIfAbsent(@Nonnull CommentedConfig config, @Nonnull String path, @Nonnull Object value) {
        if (!config.contains(path)) {
            config.set(path, value);
        }
    }

    private static void setComment(@Nonnull CommentedConfig config, @Nonnull String path, @Nonnull String comment) {
        if (config instanceof CommentedConfig commented) {
            commented.setComment(path, comment);
        }
    }
}
