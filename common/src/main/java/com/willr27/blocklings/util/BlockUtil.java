package com.willr27.blocklings.util;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.platform.Services;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import com.willr27.blocklings.util.Memoized;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * A class containing utility functions for blocks.
 */
public class BlockUtil
{
    /**
     * The most recent world to load (used to then lazy load the list of valid containers).
     */
    @Nullable
    public static Level latestWorld;

    /**
     * NeoForge / Fabric common ores tag ({@code #c:ores}). {@code #minecraft:ores} is empty in 1.21+.
     */
    private static final TagKey<Block> ORES_TAG_C = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));

    /**
     * Legacy vanilla-style tag (kept as secondary source).
     */
    private static final TagKey<Block> ORES_TAG_MC = TagKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace("ores"));

    /**
     * The list of blocks that are considered containers.
     */
    @Nonnull
    public static Memoized<List<Block>> CONTAINERS = Memoized.of(BlockUtil::createContainersList);

    /**
     * @return the set of blocks that are regarded as containers.
     */
    @Nonnull
    public static List<Block> createContainersList()
    {
        Blocklings.LOGGER.info("Creating valid containers set.");

        List<Block> containers = new ArrayList<>();

        for (Block block : BuiltInRegistries.BLOCK)
        {
            if (!(block instanceof EntityBlock entityBlock))
            {
                continue;
            }

            BlockState state = block.defaultBlockState();
            if (!state.hasBlockEntity())
            {
                continue;
            }

            BlockEntity blockEntity;
            try
            {
                blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, state);
            }
            catch (Exception ignored)
            {
                continue;
            }

            // Vanilla / most modded inventories implement Container on the block entity.
            if (blockEntity instanceof Container)
            {
                containers.add(block);
            }
        }

        Blocklings.LOGGER.info("Found {} container blocks.", containers.size());
        return containers;
    }

    /**
     * Checks if the given block type is a known container (for search UI).
     *
     * @param block the block to check.
     * @return true if the block is a container, false otherwise.
     */
    public static boolean isContainer(@Nonnull Block block)
    {
        return CONTAINERS.get().contains(block);
    }

    /**
     * Checks if the block at the given position can accept items (world selection / goals).
     */
    public static boolean isContainer(@Nonnull Level level, @Nonnull BlockPos pos)
    {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null)
        {
            return false;
        }

        for (Direction direction : Direction.values())
        {
            if (Services.INVENTORY.getItemHandler(level, pos, blockEntity, direction) != null)
            {
                return true;
            }
        }

        return blockEntity instanceof Container || isContainer(level.getBlockState(pos).getBlock());
    }

    /**
     * The list of blocks that are considered ores.
     */
    @Nonnull
    public static Memoized<Set<Block>> ORES = Memoized.of(BlockUtil::createOresList);

    /**
     * @return the list of blocks that are regarded as ores.
     */
    @Nonnull
    public static Set<Block> createOresList()
    {
        Blocklings.LOGGER.info("Creating ores list.");

        Set<Block> ores = new HashSet<>();

        List<? extends String> additionalBlocks = BlocklingsConfig.COMMON.additionalOres.get();
        List<? extends String> excludedBlocks = BlocklingsConfig.COMMON.excludedOres.get();

        addOresFromTag(ores, ORES_TAG_C, excludedBlocks);
        addOresFromTag(ores, ORES_TAG_MC, excludedBlocks);

        // Tags may not be bound yet (or #minecraft:ores is empty in 1.21) — keep vanilla ores playable.
        if (ores.isEmpty())
        {
            addVanillaOreFallbacks(ores, excludedBlocks);
        }

        for (String entry : additionalBlocks)
        {
            Runnable warn = () -> Blocklings.LOGGER.warn("Skipping additional ore \"" + entry + "\".");

            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry));

            if (block == Blocks.AIR)
            {
                warn.run();

                continue;
            }

            if (excludedBlocks.contains(entry))
            {
                warn.run();

                continue;
            }

            ores.add(block);
        }

        Blocklings.LOGGER.info("Ores list size: " + ores.size());
        return ores;
    }

    private static void addOresFromTag(@Nonnull Set<Block> ores, @Nonnull TagKey<Block> tag, @Nonnull List<? extends String> excludedBlocks)
    {
        BuiltInRegistries.BLOCK.getTagOrEmpty(tag).forEach(holder ->
        {
            Block block = holder.value();
            if (excludedBlocks.contains(RegistryUtil.blockId(block).toString()))
            {
                return;
            }

            ores.add(block);
        });
    }

    private static void addVanillaOreFallbacks(@Nonnull Set<Block> ores, @Nonnull List<? extends String> excludedBlocks)
    {
        Block[] fallbacks = new Block[]
        {
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS
        };

        for (Block block : fallbacks)
        {
            if (!excludedBlocks.contains(RegistryUtil.blockId(block).toString()))
            {
                ores.add(block);
            }
        }
    }

    /**
     * @param block the block to check.
     * @return true if the block is an ore.
     */
    public static boolean isOre(@Nonnull Block block)
    {
        return ORES.get().contains(block);
    }

    /**
     * Soft stone the miner may dig to reach ores (not a whitelist property — tunnel only).
     */
    public static boolean isTunnelStone(@Nonnull Block block)
    {
        BlockState state = block.defaultBlockState();
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER))
        {
            return true;
        }

        return block == Blocks.COBBLESTONE
                || block == Blocks.COBBLED_DEEPSLATE
                || block == Blocks.MOSSY_COBBLESTONE
                || block == Blocks.TUFF
                || block == Blocks.CALCITE
                || block == Blocks.DRIPSTONE_BLOCK
                || block == Blocks.SMOOTH_BASALT
                || block == Blocks.MAGMA_BLOCK;
    }

    /**
     * Ore whitelist siblings so disabling iron also disables deepslate iron (and vice versa).
     */
    @Nonnull
    public static List<ResourceLocation> oreWhitelistGroup(@Nonnull ResourceLocation id)
    {
        String path = id.getPath();
        String namespace = id.getNamespace();
        List<ResourceLocation> group = new ArrayList<>();
        group.add(id);

        if (path.startsWith("deepslate_") && path.endsWith("_ore"))
        {
            group.add(ResourceLocation.fromNamespaceAndPath(namespace, path.substring("deepslate_".length())));
        }
        else if (path.endsWith("_ore") && !path.startsWith("deepslate_") && !path.startsWith("nether_"))
        {
            group.add(ResourceLocation.fromNamespaceAndPath(namespace, "deepslate_" + path));
        }
        else if (path.equals("nether_gold_ore"))
        {
            group.add(ResourceLocation.fromNamespaceAndPath(namespace, "gold_ore"));
            group.add(ResourceLocation.fromNamespaceAndPath(namespace, "deepslate_gold_ore"));
        }
        else if (path.equals("gold_ore") || path.equals("deepslate_gold_ore"))
        {
            group.add(ResourceLocation.fromNamespaceAndPath(namespace, "nether_gold_ore"));
            if (path.equals("gold_ore"))
            {
                group.add(ResourceLocation.fromNamespaceAndPath(namespace, "deepslate_gold_ore"));
            }
            else
            {
                group.add(ResourceLocation.fromNamespaceAndPath(namespace, "gold_ore"));
            }
        }

        return group;
    }

    /**
     * @param blockItem a block in item form.
     * @return true if the block item is an ore.
     */
    public static boolean isOre(@Nonnull Item blockItem)
    {
        return getOre(blockItem) != null;
    }

    /**
     * Gets the block of the given block item.
     *
     * @param blockItem a block in item form.
     * @return the block if it is an ore else null.
     */
    @Nullable
    public static Block getOre(@Nonnull Item blockItem)
    {
        for (Block ore : ORES.get())
        {
            if (new ItemStack(ore).getItem() == blockItem)
            {
                return ore;
            }
        }

        return null;
    }

    /**
     * Represents the 3 blocks that make up a tree.
     */
    public static class TreeTuple
    {
        /**
         * The block that makes up the trunk of the tree.
         */
        @Nonnull
        public final Block log;

        /**
         * The block that makes up the leaves of the tree.
         */
        @Nonnull
        public final Block leaves;

        /**
         * The sapling block for the tree.
         */
        @Nonnull
        public final Block sapling;

        /**
         * @param log the log block.
         * @param leaves the leaves block.
         * @param sapling the sapling block.
         */
        public TreeTuple(@Nonnull Block log, @Nonnull Block leaves, @Nonnull Block sapling)
        {
            this.log = log;
            this.leaves = leaves;
            this.sapling = sapling;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof TreeTuple)
            {
                TreeTuple tree = (TreeTuple) obj;

                return tree.log == log && tree.leaves == leaves && tree.sapling == sapling;
            }

            return super.equals(obj);
        }
    }

    /**
     * The list of trees.
     */
    @Nonnull
    public static Memoized<List<TreeTuple>> TREES = Memoized.of(BlockUtil::createTreesList);

    /**
     * @return the list of trees.
     */
    @Nonnull
    public static List<TreeTuple> createTreesList()
    {
        Blocklings.LOGGER.info("Creating trees list.");

        List<TreeTuple> trees = new ArrayList<>();

        List<? extends String> customTrees = BlocklingsConfig.COMMON.customTrees.get();

        trees.clear();

        trees.add(new TreeTuple(Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES, Blocks.ACACIA_SAPLING));
        trees.add(new TreeTuple(Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, Blocks.BIRCH_SAPLING));
        trees.add(new TreeTuple(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES, Blocks.DARK_OAK_SAPLING));
        trees.add(new TreeTuple(Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES, Blocks.JUNGLE_SAPLING));
        trees.add(new TreeTuple(Blocks.OAK_LOG, Blocks.OAK_LEAVES, Blocks.OAK_SAPLING));
        trees.add(new TreeTuple(Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES, Blocks.SPRUCE_SAPLING));

        for (String treeString : customTrees)
        {
            Runnable warn = () -> Blocklings.LOGGER.warn("The custom tree \"" + treeString + "\" is invalid and won't be added. Should look like \"[minecraft:oak_log; minecraft:oak_leaf; minecraft:oak_sapling]\".");

            if (!treeString.startsWith("[") || !treeString.endsWith("]") || treeString.length() < 10)
            {
                warn.run();

                continue;
            }

            String[] splitTreeString = treeString.substring(1, treeString.length() - 1).split("; ");

            if (splitTreeString.length != 3)
            {
                warn.run();

                continue;
            }

            Block log = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(splitTreeString[0]));
            Block leaves = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(splitTreeString[1]));
            Block sapling = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(splitTreeString[2]));

            if (log == Blocks.AIR || leaves == Blocks.AIR || sapling == Blocks.AIR)
            {
                warn.run();

                continue;
            }

            TreeTuple tree = new TreeTuple(log, leaves, sapling);

            if (!trees.contains(tree))
            {
                trees.add(tree);
            }
        }

        return trees;
    }

    /**
     * @param block the block to check.
     * @return true if the block is a log.
     */
    public static boolean isLog(@Nonnull Block block)
    {
        return TREES.get().stream().anyMatch(tree -> tree.log == block);
    }

    /**
     * @param blockItem a block in item form.
     * @return true if the block item is a log.
     */
    public static boolean isLog(@Nonnull Item blockItem)
    {
        return getLog(blockItem) != null;
    }

    /**
     * Gets the block of the given block item.
     *
     * @param blockItem a block in item form.
     * @return the block if it is a log else null.
     */
    @Nullable
    public static Block getLog(@Nonnull Item blockItem)
    {
        for (TreeTuple tree : TREES.get())
        {
            if (new ItemStack(tree.log).getItem() == blockItem)
            {
                return tree.log;
            }
        }

        return null;
    }

    /**
     * @param block the block to check.
     * @return true if the block is leaves.
     */
    public static boolean isLeaves(@Nonnull Block block)
    {
        return TREES.get().stream().anyMatch(tree -> tree.leaves == block);
    }

    /**
     * @param blockItem a block in item form.
     * @return true if the block item is a leaf.
     */
    public static boolean isLeaves(@Nonnull Item blockItem)
    {
        return getLeaves(blockItem) != null;
    }

    /**
     * Gets the leaves block of the given log block.
     *
     * @return the block if it is leaves else null.
     */
    @Nullable
    public static Block getLeaves(@Nonnull Block logBlock)
    {
        for (TreeTuple tree : TREES.get())
        {
            if (tree.log == logBlock)
            {
                return tree.leaves;
            }
        }

        return null;
    }

    /**
     * Gets the block of the given block item.
     *
     * @param blockItem a block in item form.
     * @return the block if it is leaves else null.
     */
    @Nullable
    public static Block getLeaves(@Nonnull Item blockItem)
    {
        for (TreeTuple tree : TREES.get())
        {
            if (new ItemStack(tree.leaves).getItem() == blockItem)
            {
                return tree.leaves;
            }
        }

        return null;
    }

    /**
     * @param block the block to check.
     * @return true if the block is a sapling.
     */
    public static boolean isSapling(@Nonnull Block block)
    {
        return TREES.get().stream().anyMatch(tree -> tree.sapling == block);
    }

    /**
     * @param blockItem a block in item form.
     * @return true if the block item is a sapling.
     */
    public static boolean isSapling(@Nonnull Item blockItem)
    {
        return getSapling(blockItem) != null;
    }

    /**
     * Gets the block of the given block item.
     *
     * @param blockItem a block in item form.
     * @return the block if it is a sapling else null.
     */
    @Nullable
    public static Block getSapling(@Nonnull Item blockItem)
    {
        for (TreeTuple tree : TREES.get())
        {
            if (new ItemStack(tree.sapling).getItem() == blockItem)
            {
                return tree.sapling;
            }
        }

        return null;
    }

    /**
     * Gets the sapling block for the given log.
     *
     * @param logBlock the log block.
     * @return the sapling if the log block is recognised, else null.
     */
    @Nullable
    public static Block getSaplingFromLog(@Nonnull Block logBlock)
    {
        for (TreeTuple tree : TREES.get())
        {
            if (tree.log == logBlock)
            {
                return tree.sapling;
            }
        }

        return null;
    }

    /**
     * Dark oak (and similar) saplings only grow as a 2x2; a single sapling can still be placed.
     */
    public static boolean requiresTwoByTwoSaplings(@Nonnull Block sapling)
    {
        return sapling == Blocks.DARK_OAK_SAPLING;
    }

    /**
     * The list of blocks that are considered crops.
     */
    @Nonnull
    public static Memoized<Set<Block>> CROPS = Memoized.of(BlockUtil::createCropsList);

    /**
     * @return the list of blocks that are regarded as crops.
     */
    @Nonnull
    public static Set<Block> createCropsList()
    {
        Blocklings.LOGGER.info("Creating crops list.");

        Set<Block> crops = new HashSet<>();

        crops.clear();

        crops.add(Blocks.WHEAT);
        crops.add(Blocks.BEETROOTS);
        crops.add(Blocks.CARROTS);
        crops.add(Blocks.POTATOES);
        crops.add(Blocks.PUMPKIN);
        crops.add(Blocks.MELON);
        crops.add(Blocks.TORCHFLOWER_CROP);
        crops.add(Blocks.PITCHER_CROP);

        for (String additionalString : BlocklingsConfig.COMMON.additionalCrops.get())
        {
            Runnable warn = () -> Blocklings.LOGGER.warn("Skipping additional crop \"" + additionalString + "\".");

            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(additionalString));

            if (block == Blocks.AIR)
            {
                warn.run();

                continue;
            }

            crops.add(block);
        }

        for (String excludedString : BlocklingsConfig.COMMON.excludedCrops.get())
        {
            crops.remove(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(excludedString)));
        }

        return crops;
    }

    /**
     * @param block the block to check.
     * @return true if the block is a crop.
     */
    public static boolean isCrop(@Nonnull Block block)
    {
        return CROPS.get().contains(block);
    }

    /**
     * Resolves the seed/item used to plant a crop block.
     * Crop blocks often have {@code asItem() == AIR}; use this for whitelist icons and planting.
     */
    @Nonnull
    public static Item getCropSeedItem(@Nonnull Block crop)
    {
        // Prefer explicit vanilla mapping — getCloneItemStack(null, ...) is unreliable for GUI.
        if (crop == Blocks.WHEAT)
        {
            return Items.WHEAT_SEEDS;
        }
        if (crop == Blocks.BEETROOTS)
        {
            return Items.BEETROOT_SEEDS;
        }
        if (crop == Blocks.CARROTS)
        {
            return Items.CARROT;
        }
        if (crop == Blocks.POTATOES)
        {
            return Items.POTATO;
        }
        if (crop == Blocks.PUMPKIN || crop == Blocks.PUMPKIN_STEM || crop == Blocks.ATTACHED_PUMPKIN_STEM)
        {
            return Items.PUMPKIN_SEEDS;
        }
        if (crop == Blocks.MELON || crop == Blocks.MELON_STEM || crop == Blocks.ATTACHED_MELON_STEM)
        {
            return Items.MELON_SEEDS;
        }
        if (crop == Blocks.TORCHFLOWER_CROP || crop == Blocks.TORCHFLOWER)
        {
            return Items.TORCHFLOWER_SEEDS;
        }
        if (crop == Blocks.PITCHER_CROP || crop == Blocks.PITCHER_PLANT)
        {
            return Items.PITCHER_POD;
        }

        Item asItem = crop.asItem();
        if (asItem != null && asItem != Items.AIR)
        {
            return asItem;
        }

        if (crop instanceof net.minecraft.world.level.block.CropBlock cropBlock)
        {
            try
            {
                ItemStack seed = cropBlock.getCloneItemStack(null, BlockPos.ZERO, crop.defaultBlockState());
                if (!seed.isEmpty() && seed.getItem() != Items.AIR)
                {
                    return seed.getItem();
                }
            }
            catch (Exception ignored)
            {
            }
        }

        return Items.AIR;
    }

    /**
     * Resolves the block that should be placed when planting the given seed/item.
     * Pumpkin/melon seeds place stems; crop fruit blocks are not plantable directly.
     *
     * @return the plant block, or {@link Blocks#AIR} if the item is not a farm seed.
     */
    @Nonnull
    public static Block getPlantBlockForSeed(@Nonnull Item seed)
    {
        if (seed == Items.AIR)
        {
            return Blocks.AIR;
        }

        if (seed == Items.WHEAT_SEEDS)
        {
            return Blocks.WHEAT;
        }
        if (seed == Items.BEETROOT_SEEDS)
        {
            return Blocks.BEETROOTS;
        }
        if (seed == Items.CARROT)
        {
            return Blocks.CARROTS;
        }
        if (seed == Items.POTATO)
        {
            return Blocks.POTATOES;
        }
        if (seed == Items.PUMPKIN_SEEDS)
        {
            return Blocks.PUMPKIN_STEM;
        }
        if (seed == Items.MELON_SEEDS)
        {
            return Blocks.MELON_STEM;
        }
        if (seed == Items.TORCHFLOWER_SEEDS)
        {
            return Blocks.TORCHFLOWER_CROP;
        }
        if (seed == Items.PITCHER_POD)
        {
            return Blocks.PITCHER_CROP;
        }

        Block byItem = Block.byItem(seed);
        if (byItem != Blocks.AIR)
        {
            if (byItem instanceof net.minecraft.world.level.block.CropBlock
                    || byItem instanceof net.minecraft.world.level.block.StemBlock
                    || byItem instanceof net.minecraft.world.level.block.PitcherCropBlock
                    || isCrop(byItem))
            {
                return byItem;
            }
        }

        // Reverse lookup via known crop → seed mapping (handles config-added crops).
        for (Block crop : CROPS.get())
        {
            if (getCropSeedItem(crop) == seed)
            {
                if (crop == Blocks.PUMPKIN)
                {
                    return Blocks.PUMPKIN_STEM;
                }
                if (crop == Blocks.MELON)
                {
                    return Blocks.MELON_STEM;
                }
                return crop;
            }
        }

        return Blocks.AIR;
    }

    /**
     * @return true if the item can be planted on farmland by a farming blockling.
     */
    public static boolean isPlantableFarmSeed(@Nonnull Item item)
    {
        return getPlantBlockForSeed(item) != Blocks.AIR;
    }

    /**
     * @param blockItem a block in item form.
     * @return true if the block item is a crop.
     */
    public static boolean isCrop(@Nonnull Item blockItem)
    {
        return getCrop(blockItem) != null || getPlantBlockForSeed(blockItem) != Blocks.AIR;
    }

    /**
     * Gets the block of the given block item.
     *
     * @param blockItem a block in item form.
     * @return the block if it is a crop else null.
     */
    @Nullable
    public static Block getCrop(@Nonnull Item blockItem)
    {
        Block planted = getPlantBlockForSeed(blockItem);
        if (planted != Blocks.AIR)
        {
            return planted;
        }

        for (Block crop : CROPS.get())
        {
            if (getCropSeedItem(crop) == blockItem)
            {
                return crop;
            }
        }

        return null;
    }

    /**
     * @param world the world the block is being checked in.
     * @param block the block to check.
     * @param pos the position in the world at which to check.
     * @return true if the block can be placed at the given location.
     */
    public static boolean canPlaceAt(@Nonnull Level world, @Nonnull Block block, @Nonnull BlockPos pos)
    {
        return block.defaultBlockState().canSurvive(world, pos);
    }

    /**
     * @param percentage the percentage to convert to block break progress.
     * @return the block break progress.
     */
    public static int calcBlockBreakProgress(float percentage)
    {
        return (int) (10 * percentage);
    }

    /**
     * Checks whether all adjacent blocks are solid.
     *
     * @param world the world the block is in.
     * @param blockPos the block position to test.
     * @return true if all adjacent blocks are solid.
     */
    public static boolean areAllAdjacentBlocksSolid(@Nonnull Level world, @Nonnull BlockPos blockPos)
    {
        return !Arrays.stream(getAdjacentBlockPositions(blockPos)).anyMatch(adjacent -> !world.getBlockState(adjacent).isSolid());
    }

    /**
     * Gets the positions adjacent to the given block pos.
     * Does not include diagonals.
     *
     * @param blockPos the position to get the adjacent positions of.
     * @return an array of the adjacent block positions.
     */
    @Nonnull
    public static BlockPos[] getAdjacentBlockPositions(@Nonnull BlockPos blockPos)
    {
        return new BlockPos[]
        {
            blockPos.offset(-1, 0, 0),
            blockPos.offset(1, 0, 0),
            blockPos.offset(0, -1, 0),
            blockPos.offset(0, 1, 0),
            blockPos.offset(0, 0, -1),
            blockPos.offset(0, 0, 1),
        };
    }

    /**
     * Gets the positions surrounding the given block pos.
     * Includes diagonals.
     *
     * @param blockPos the position to get the surrounding positions of.
     * @return an array of the surrounding block positions.
     */
    @Nonnull
    public static BlockPos[] getSurroundingBlockPositions(@Nonnull BlockPos blockPos)
    {
        return new BlockPos[]
        {
            // Blocks at the same level first.
            blockPos.offset(-1, 0, -1),
            blockPos.offset(-1, 0, 0),
            blockPos.offset(-1, 0, 1),
            blockPos.offset(0, 0, -1),
            blockPos.offset(0, 0, 1),
            blockPos.offset(1, 0, -1),
            blockPos.offset(1, 0, 0),
            blockPos.offset(1, 0, 1),
            // Then blocks below.
            blockPos.offset(-1, -1, -1),
            blockPos.offset(-1, -1, 0),
            blockPos.offset(-1, -1, 1),
            blockPos.offset(0, -1, -1),
            blockPos.offset(0, -1, 0),
            blockPos.offset(0, -1, 1),
            blockPos.offset(1, -1, -1),
            blockPos.offset(1, -1, 0),
            blockPos.offset(1, -1, 1),
            // Then blocks above.
            blockPos.offset(0, 1, -1),
            blockPos.offset(0, 1, 0),
            blockPos.offset(0, 1, 1),
            blockPos.offset(-1, 1, -1),
            blockPos.offset(-1, 1, 0),
            blockPos.offset(-1, 1, 1),
            blockPos.offset(1, 1, -1),
            blockPos.offset(1, 1, 0),
            blockPos.offset(1, 1, 1),
        };
    }

    /**
     * @return the distance squared between two blocks from center to center.
     */
    public static double distanceSq(@Nonnull BlockPos blockPos1, @Nonnull BlockPos blockPos2)
    {
        return blockPos1.distSqr(blockPos2);
    }
}
