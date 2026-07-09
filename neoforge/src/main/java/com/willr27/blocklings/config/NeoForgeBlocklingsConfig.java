package com.willr27.blocklings.config;

import com.electronwill.nightconfig.core.Config;
import com.willr27.blocklings.entity.blockling.BlocklingType;
import com.willr27.blocklings.entity.blockling.goal.goals.gather.BlocklingWoodcutGoal;
import com.willr27.blocklings.util.WorldUtil;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The class used to handle the Blocklings' config.
 */
public class NeoForgeBlocklingsConfig
{
    /**
     * The instance of the common config to access values from.
     */
    @Nonnull
    public static final Common COMMON;

    /**
     * The common config spec.
     */
    @Nonnull
    public static final ModConfigSpec COMMON_SPEC;

    /**
     * The instance of the common config to access values from.
     */
    @Nonnull
    public static final Client CLIENT;

    /**
     * The common config spec.
     */
    @Nonnull
    public static final ModConfigSpec CLIENT_SPEC;

    /**
     * Static constructor to initialize the config.
     */
    static
    {
        Pair<Common, ModConfigSpec> commonSpecPair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = commonSpecPair.getLeft();
        COMMON_SPEC = commonSpecPair.getRight();

        Pair<Client, ModConfigSpec> clientSpecPair = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = clientSpecPair.getLeft();
        CLIENT_SPEC = clientSpecPair.getRight();
    }

    /**
     * Initialises the configs.
     */
    public static void init(@Nonnull ModContainer modContainer)
    {
        modContainer.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    /**
     * Config options shared by both the client and server.
     */
    public static class Common
    {
        /**
         * The blocks to ensure are added to the list of blocks that are regarded as ores.
         * This should only include blocks that are not tagged as ores.
         * The final list will also include any block with the ores tag (disjoint with the excluded ores).
         */
        @Nonnull
        public final ModConfigSpec.ConfigValue<List<? extends String>> additionalOres;

        /**
         * The blocks to ensure are excluded from the list of blocks that are regarded as ores.
         */
        @Nonnull
        public final ModConfigSpec.ConfigValue<List<? extends String>> excludedOres;

        /**
         * The minimum number of leaves blocks for each log block to classify a tree as valid.
         */
        @Nonnull
        public final ModConfigSpec.ConfigValue<Double> defaultMinLeavesToLogRatio;

        /**
         * The list of tuples of blocks that the user wants to add as trees.
         * Format: ["[x:y; a:b; j:k]", "[x:y; a:b; j:k]"] (log, leaf, sapling).
         */
        @Nonnull
        public final ModConfigSpec.ConfigValue<List<? extends String>> customTrees;

        /**
         * The blocks to ensure are added to the list of blocks that are regarded as crops.
         * This should only include blocks that are not added by default.
         */
        @Nonnull
        public final ModConfigSpec.ConfigValue<List<? extends String>> additionalCrops;

        /**
         * The blocks to ensure are excluded from the list of blocks that are regarded as crops.
         */
        @Nonnull
        public final ModConfigSpec.ConfigValue<List<? extends String>> excludedCrops;

        /**
         * Modular type ability tuning.
         */
        @Nonnull
        public final NeoForgeBlocklingAbilityConfig abilities;

        @Nonnull
        public final ModConfigSpec.DoubleValue evolveSuccessChance;

        @Nonnull
        public final ModConfigSpec.DoubleValue primaryTypeChangeChance;

        @Nonnull
        public final ModConfigSpec.BooleanValue spawnEnabled;

        @Nonnull
        public final ModConfigSpec.IntValue nearbyCap;

        @Nonnull
        public final ModConfigSpec.DoubleValue nearbyRadius;

        @Nonnull
        public final ModConfigSpec.BooleanValue preventDuplicateNearbyType;

        @Nonnull
        public final Map<String, TypeSpawnConfig> typeSpawns = new LinkedHashMap<>();

        /**
         * Per-type spawn tuning.
         */
        public static final class TypeSpawnConfig
        {
            @Nonnull
            public final ModConfigSpec.BooleanValue enabled;
            @Nonnull
            public final ModConfigSpec.IntValue spawnWeight;
            @Nonnull
            public final ModConfigSpec.ConfigValue<List<? extends String>> extraBiomes;
            @Nonnull
            public final ModConfigSpec.ConfigValue<String> biomeMode;

            private TypeSpawnConfig(@Nonnull ModConfigSpec.Builder builder, @Nonnull BlocklingType type)
            {
                builder.push(type.key);

                enabled = builder
                        .comment("Whether this blockling type can be chosen for natural/chunk spawns.")
                        .define("enabled", true);

                spawnWeight = builder
                        .comment("Relative weight among valid candidates at a spawn location. Higher = more common.")
                        .defineInRange("spawnWeight", BlocklingSpawnConfig.defaultWeightFor(type), 0, 10000);

                extraBiomes = builder
                        .comment("Extra biome ids (minecraft:plains) or tags (#minecraft:is_forest).",
                                "With biomeMode AND: must also match Java spawn predicates.",
                                "With biomeMode OVERRIDE: if non-empty, only this list is used (predicates ignored).")
                        .defineList("extraBiomes", () -> new ArrayList<>(), s -> true);

                biomeMode = builder
                        .comment("AND = predicates + extraBiomes (if any). OVERRIDE = extraBiomes only when non-empty.")
                        .define("biomeMode", "AND");

                builder.pop();
            }
        }

        /**
         * @param builder the builder used to create the config.
         */
        public Common(@Nonnull ModConfigSpec.Builder builder)
        {
            Config.setInsertionOrderPreserved(true);

            abilities = new NeoForgeBlocklingAbilityConfig(builder);

            builder.push("Upgrade");

            evolveSuccessChance = builder
                    .comment("Chance (0.0–1.0) that crouching + food evolves the natural blockling type. Original default: 0.25.")
                    .defineInRange("evolveSuccessChance", 0.25D, 0.0D, 1.0D);

            primaryTypeChangeChance = builder
                    .comment("Chance (0.0–1.0) that food without crouching changes the primary type. Original default: 0.25.")
                    .defineInRange("primaryTypeChangeChance", 0.25D, 0.0D, 1.0D);

            builder.pop();

            builder.push("Spawn");

            spawnEnabled = builder
                    .comment("Master switch for natural/chunk blockling spawning.")
                    .define("enabled", true);

            nearbyCap = builder
                    .comment("Max untamed blocklings allowed within nearbyRadius (0 = unlimited).")
                    .defineInRange("nearbyCap", 3, 0, 64);

            nearbyRadius = builder
                    .comment("Horizontal radius used for nearbyCap and duplicate-type checks.")
                    .defineInRange("nearbyRadius", 64.0D, 1.0D, 256.0D);

            preventDuplicateNearbyType = builder
                    .comment("If true, reject a spawn when another nearby blockling already has the same primary type.")
                    .define("preventDuplicateNearbyType", true);

            builder.push("types");
            for (BlocklingType type : BlocklingType.TYPES)
            {
                typeSpawns.put(type.key, new TypeSpawnConfig(builder, type));
            }
            builder.pop();

            builder.pop();

            builder.push("Mining");

            additionalOres = builder
                    .comment("The list of blocks (as registry names) to ensure are included in the list of blocks that are regarded as ores.",
                            "Any block with an ores tag will automatically be added, so only include ores without that tag here.",
                            "NOT ALL BLOCKS ARE GUARANTEED TO WORK.",
                            "Example: [\"minecraft:stone\", \"minecraft:obsidian\"]")
                    .worldRestart()
                    .defineList("additionalOres", () -> new ArrayList<>(), s -> true);

            excludedOres = builder
                    .comment("The list of blocks (as registry names) to ensure are excluded from the list of blocks that regarded as ores.",
                            "Any block with an ores tag will automatically be added unless specified here.",
                            "This is useful if you notice modded blocks that are tagged as ores that you don't want/think should be.",
                            "Example: [\"minecraft:coal_ore\", \"minecraft:diamond_ore\"]")
                    .worldRestart()
                    .defineList("excludedOres", () -> new ArrayList<>(), s -> true);

            builder.pop();

            builder.push("Woodcutting");

            defaultMinLeavesToLogRatio = builder
                    .comment("The default minimum number of leaves blocks for each log block to classify a tree as valid.",
                             "E.g. a ratio of 2.0 means that 5 connected logs and more than 10 connected leaves would classify as a tree.",
                             "This can be changed on a per task basis using the slider when configuring a woodcutting task.",
                             "This is also used as the ratio to find trees for log blocklings' passive abilities.")
                    .worldRestart()
                    .defineInRange("defaultMinLeavesToLogRatio", WorldUtil.DEFAULT_MIN_LEAVES_TO_LOGS_RATIO, BlocklingWoodcutGoal.MIN_MIN_LEAVES_TO_LOGS_RATIO, BlocklingWoodcutGoal.MAX_MIN_LEAVES_TO_LOGS_RATIO);

            customTrees = builder
                    .comment("The list of tuples of blocks that you want to additionally add as trees",
                            "This is useful for modded trees that don't already have support.",
                            "NOT ALL BLOCKS ARE GUARANTEED TO WORK.",
                            "Example: [\"[minecraft:oak_log; minecraft:oak_leaves; minecraft:oak_sapling]\", \"[...]\"]",
                            "This would add oak trees as a custom tree (but they already have support so you don't need to add them here).")
                    .worldRestart()
                    .defineList("customTrees", () -> new ArrayList<>(), s -> true);

            builder.pop();

            builder.push("Farming");

            additionalCrops = builder
                    .comment("The list of blocks (as registry names) to ensure are included in the list of blocks that are regarded as crops.",
                            "NOT ALL BLOCKS ARE GUARANTEED TO WORK.",
                            "Example: [\"minecraft:wheat\", \"minecraft:melon\"]")
                    .worldRestart()
                    .defineList("additionalCrops", () -> new ArrayList<>(), s -> true);

            excludedCrops = builder
                    .comment("The list of blocks (as registry names) to ensure are excluded from the list of blocks that regarded as crops.",
                            "This is useful if you notice modded blocks that have been added as crops that you don't want/think should be.",
                            "Example: [\"minecraft:wheat\", \"minecraft:melon\"]")
                    .worldRestart()
                    .defineList("excludedCrops", () -> new ArrayList<>(), s -> true);

            builder.pop();
        }
    }

    /**
     * Config options only available to each client.
     */
    public static class Client
    {
        /**
         * Whether the mixed blockling type textures are disabled.
         */
        public final ModConfigSpec.ConfigValue<Boolean> disableDirtyBlocklings;

        /**
         * @param builder the builder used to create the config.
         */
        public Client(@Nonnull ModConfigSpec.Builder builder)
        {
            Config.setInsertionOrderPreserved(true);

            builder.push("Misc");

            disableDirtyBlocklings = builder
                    .comment("Set this to true to use the pure blockling type texture instead of merged textures (merged assets are not bundled).")
                    .define("disableDirtyBlocklings", true);

            builder.pop();
        }
    }
}
