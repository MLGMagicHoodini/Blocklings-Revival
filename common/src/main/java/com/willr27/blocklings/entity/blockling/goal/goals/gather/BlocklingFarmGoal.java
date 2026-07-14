package com.willr27.blocklings.entity.blockling.goal.goals.gather;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingHand;
import com.willr27.blocklings.entity.blockling.skill.skills.FarmingSkills;
import com.willr27.blocklings.entity.blockling.skill.skills.GeneralSkills;
import com.willr27.blocklings.entity.blockling.task.BlocklingTasks;
import com.willr27.blocklings.entity.blockling.goal.config.whitelist.GoalWhitelist;
import com.willr27.blocklings.entity.blockling.goal.config.whitelist.Whitelist;
import com.willr27.blocklings.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Farm Crops — calm, rectangular fields:
 * <ul>
 *   <li>Till/plant cap = {@code min(64, total allowed seeds of all types)} (125 seeds → 64 plots max).</li>
 *   <li>Expands only adjacent to existing farmland into a compact square/rectangle (prefers water).</li>
 *   <li>Never breaks player crops/plantations; only clears grass on the plot being worked.</li>
 *   <li>With Replanter: fills empty farmland with every allowed seed type in inventory.</li>
 * </ul>
 */
public class BlocklingFarmGoal extends BlocklingGatherGoal
{
    private static final int SEARCH_RADIUS_X = 8;
    private static final int SEARCH_RADIUS_Y = 2;
    /** Moisture range — farmland must be within this Chebyshev distance of water. */
    private static final int WATER_RANGE = 4;
    /** Max farmland plots for the whole farm (all seed types combined). */
    private static final int MAX_FARM_PLOTS = 64;
    /** Moisture property max on farmland (vanilla). */
    private static final int MAX_FARMLAND_MOISTURE = 7;

    public final GoalWhitelist cropWhitelist;
    public final GoalWhitelist seedWhitelist;

    private enum FarmWork
    {
        HARVEST,
        PLANT,
        TILL,
        IRRIGATE
    }

    @Nonnull
    private FarmWork farmWork = FarmWork.HARVEST;

    /** Round-robin index so every whitelisted seed type in the inventory gets planted. */
    private int seedScanIndex = 0;

    public BlocklingFarmGoal(@Nonnull UUID id, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(id, blockling, tasks);

        cropWhitelist = new GoalWhitelist("25140edf-f60e-459e-b1f0-9ff82108ec0b", "crops", Whitelist.Type.BLOCK, this);
        cropWhitelist.setIsUnlocked(blockling.getSkills().getSkill(FarmingSkills.CROP_WHITELIST).isBought(), false);
        BlockUtil.CROPS.get().forEach(crop -> cropWhitelist.put(RegistryUtil.blockId(crop), true));
        whitelists.add(cropWhitelist);

        seedWhitelist = new GoalWhitelist("d77bf1c1-7718-4733-b763-298b03340eea", "seeds", Whitelist.Type.ITEM, this);
        seedWhitelist.setIsUnlocked(blockling.getSkills().getSkill(FarmingSkills.SEED_WHITELIST).isBought(), false);
        BlockUtil.CROPS.get().forEach(crop ->
        {
            Item seed = BlockUtil.getCropSeedItem(crop);
            if (seed != Items.AIR)
            {
                seedWhitelist.put(RegistryUtil.itemId(seed), true);
            }
        });
        whitelists.add(seedWhitelist);

        setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
    }

    @Nonnull
    @Override
    public String getDebugStatus()
    {
        return super.getDebugStatus() + " work=" + farmWork;
    }

    @Override
    protected void tickGather()
    {
        if (farmWork == FarmWork.PLANT)
        {
            tickPlant();
            return;
        }

        if (farmWork == FarmWork.TILL)
        {
            tickTill();
            return;
        }

        if (farmWork == FarmWork.IRRIGATE)
        {
            tickIrrigate();
            return;
        }

        tickHarvest();
    }

    private void tickHarvest()
    {
        super.tickGather();

        ItemStack mainStack = blockling.getMainHandItem();
        ItemStack offStack = blockling.getOffhandItem();

        BlockPos targetPos = getTarget();
        if (targetPos == null)
        {
            return;
        }

        BlockState targetBlockState = world.getBlockState(targetPos);
        Block targetBlock = targetBlockState.getBlock();

        boolean mainCanHarvest = ToolUtil.canToolHarvest(mainStack, targetBlockState);
        boolean offCanHarvest = ToolUtil.canToolHarvest(offStack, targetBlockState);

        if (mainCanHarvest || offCanHarvest)
        {
            blockling.getActions().gather.tryStart();

            if (blockling.getActions().gather.isRunning())
            {
                float blocklingDestroySpeed = blockling.getStats().farmingSpeed.getValue();
                float mainDestroySpeed = mainCanHarvest ? ToolUtil.getToolHarvestSpeedWithEnchantments(mainStack, targetBlockState) : 0.0f;
                float offDestroySpeed = offCanHarvest ? ToolUtil.getToolHarvestSpeedWithEnchantments(offStack, targetBlockState) : 0.0f;

                float destroySpeed = blocklingDestroySpeed + mainDestroySpeed + offDestroySpeed;
                float blockStrength = targetBlockState.getDestroySpeed(world, targetPos);

                blockling.getStats().InteractionHand.setValue(BlocklingHand.fromBooleans(mainCanHarvest, offCanHarvest));

                float progress = destroySpeed / (blockStrength + 2.5f) / 100.0f;
                blockling.getActions().gather.tick(progress);

                if (blockling.getActions().gather.isFinished())
                {
                    blockling.getActions().gather.stop();
                    blockling.getStats().farmingXp.incrementValue((int) ((blockStrength + 1.0f) * 3.0f));

                    for (ItemStack stack : DropUtil.getDrops(DropUtil.Context.FARMING, blockling, targetPos, mainCanHarvest ? mainStack : ItemStack.EMPTY, offCanHarvest ? offStack : ItemStack.EMPTY))
                    {
                        stack = blockling.getEquipment().addItem(stack);
                        blockling.dropItemStack(stack);
                    }

                    if (ToolUtil.damageTool(mainStack, blockling, mainCanHarvest ? blockling.getSkills().getSkill(FarmingSkills.HASTY).isBought() ? 2 : 1 : 0))
                    {
                        mainStack.shrink(1);
                    }

                    if (ToolUtil.damageTool(offStack, blockling, offCanHarvest ? blockling.getSkills().getSkill(FarmingSkills.HASTY).isBought() ? 2 : 1 : 0))
                    {
                        offStack.shrink(1);
                    }

                    blockling.incCropsHarvestedRecently();

                    com.willr27.blocklings.command.BlocklingTaskLogger.event(
                            blockling, "HARVEST", targetBlock + " at " + targetPos.toShortString());

                    ItemStack seedStack = ItemStack.EMPTY;
                    boolean replanter = blockling.getSkills().getSkill(FarmingSkills.REPLANTER).isBought();

                    if (replanter && targetBlock instanceof CropBlock cropBlock)
                    {
                        seedStack = cropBlock.getCloneItemStack(world, targetPos, targetBlockState);
                    }

                    world.destroyBlock(targetPos, false);
                    world.destroyBlockProgress(blockling.getId(), targetPos, -1);

                    ensureFarmland(targetPos.below());

                    if (blockling.getSkills().getSkill(FarmingSkills.SCYTHE).isBought())
                    {
                        for (BlockPos surroundingPos : BlockUtil.getSurroundingBlockPositions(targetPos))
                        {
                            BlockState surroundingBlockState = world.getBlockState(surroundingPos);
                            Block surroundingBlock = surroundingBlockState.getBlock();

                            if (isValidHarvestTarget(surroundingPos))
                            {
                                for (ItemStack stack : DropUtil.getDrops(DropUtil.Context.FARMING, blockling, surroundingPos, mainCanHarvest ? mainStack : ItemStack.EMPTY, offCanHarvest ? offStack : ItemStack.EMPTY))
                                {
                                    stack = blockling.getEquipment().addItem(stack);
                                    blockling.dropItemStack(stack);
                                }

                                ItemStack seedStack2 = ItemStack.EMPTY;
                                if (replanter && surroundingBlock instanceof CropBlock cropBlock)
                                {
                                    seedStack2 = cropBlock.getCloneItemStack(world, surroundingPos, surroundingBlockState);
                                }

                                world.destroyBlock(surroundingPos, false);
                                ensureFarmland(surroundingPos.below());

                                if (replanter)
                                {
                                    tryPlantSeedAt(surroundingPos, seedStack2);
                                }
                            }
                        }
                    }

                    if (replanter)
                    {
                        tryPlantSeedAt(targetPos, seedStack);
                    }
                }
                else if (targetBlockState.isSolid())
                {
                    world.destroyBlockProgress(blockling.getId(), targetPos, BlockUtil.calcBlockBreakProgress(blockling.getActions().gather.getCount()));
                }
            }
        }
        else
        {
            world.destroyBlockProgress(blockling.getId(), targetPos, -1);
            blockling.getActions().gather.stop();
        }
    }

    private void tickPlant()
    {
        BlockPos farmlandPos = getTarget();
        if (farmlandPos == null)
        {
            return;
        }

        blockling.getLookControl().setLookAt(farmlandPos.getX() + 0.5, farmlandPos.getY() + 0.5, farmlandPos.getZ() + 0.5);

        if (!isValidPlantSpot(farmlandPos))
        {
            markEntireTargetBad();
            return;
        }

        ItemStack seed = findWhitelistedPlantableSeed();
        if (seed.isEmpty())
        {
            markEntireTargetBad();
            return;
        }

        // Only the grass on THIS plot — never a manic sweep that tears the whole field.
        if (!clearFarmVegetationAbove(farmlandPos))
        {
            markEntireTargetBad();
            return;
        }

        if (tryPlantSeedAt(farmlandPos.above(), seed))
        {
            com.willr27.blocklings.command.BlocklingTaskLogger.event(
                    blockling, "PLANT", seed.getHoverName().getString() + " at " + farmlandPos.above().toShortString());

            blockling.getStats().farmingXp.incrementValue(1);
            setTarget(null);
            setPathTargetPos(null, null);
        }
        else
        {
            markEntireTargetBad();
        }
    }

    private void tickTill()
    {
        BlockPos tillPos = getTarget();
        if (tillPos == null)
        {
            return;
        }

        blockling.getLookControl().setLookAt(tillPos.getX() + 0.5, tillPos.getY() + 0.5, tillPos.getZ() + 0.5);

        // Equip a hoe from the inventory before tilling (not only with AUTOSWITCH), so a blockling
        // holding an axe for woodcutting still swaps to its hoe to farm instead of "tilling with an axe".
        if (!ToolUtil.isHoe(blockling.getMainHandItem()) && !ToolUtil.isHoe(blockling.getOffhandItem())
                || blockling.getSkills().getSkill(GeneralSkills.AUTOSWITCH).isBought())
        {
            blockling.getEquipment().trySwitchToBestTool(BlocklingHand.BOTH, new ToolContext(ToolType.HOE, world.getBlockState(tillPos)));
        }

        if (!isValidTillSpot(tillPos) || !hasHoeReady() || !canTillMore())
        {
            markEntireTargetBad();
            return;
        }

        // Clear only the vegetation sitting on this soil cell.
        if (!clearFarmVegetationAbove(tillPos))
        {
            markEntireTargetBad();
            return;
        }

        if (ensureFarmland(tillPos))
        {
            setTarget(null);
            setPathTargetPos(null, null);
        }
        else
        {
            markEntireTargetBad();
        }
    }

    private void tickIrrigate()
    {
        BlockPos waterPos = getTarget();
        if (waterPos == null)
        {
            return;
        }

        blockling.getLookControl().setLookAt(waterPos.getX() + 0.5, waterPos.getY() + 0.5, waterPos.getZ() + 0.5);

        if (!hasReplanter() || !hasWaterBucket() || !isValidWaterPlacement(waterPos))
        {
            markEntireTargetBad();
            return;
        }

        // Clear grass on top before digging the water hole.
        clearFarmVegetationAbove(waterPos);

        if (!blockling.getEquipment().take(new ItemStack(Items.WATER_BUCKET)))
        {
            markEntireTargetBad();
            return;
        }

        com.willr27.blocklings.command.BlocklingTaskLogger.event(
                blockling, "IRRIGATE", "water at " + waterPos.toShortString());

        world.setBlock(waterPos, Blocks.WATER.defaultBlockState(), 3);
        world.gameEvent(blockling, GameEvent.BLOCK_PLACE, waterPos);
        world.playSound(null, waterPos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);

        ItemStack leftover = blockling.getEquipment().addItem(new ItemStack(Items.BUCKET));
        blockling.dropItemStack(leftover);

        moistenFarmlandNear(waterPos);

        setTarget(null);
        setPathTargetPos(null, null);
    }

    private void moistenFarmlandNear(@Nonnull BlockPos waterPos)
    {
        for (int x = -WATER_RANGE; x <= WATER_RANGE; x++)
        {
            for (int y = -1; y <= 1; y++)
            {
                for (int z = -WATER_RANGE; z <= WATER_RANGE; z++)
                {
                    BlockPos pos = waterPos.offset(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (state.is(Blocks.FARMLAND) && state.hasProperty(BlockStateProperties.MOISTURE))
                    {
                        world.setBlock(pos, state.setValue(BlockStateProperties.MOISTURE, MAX_FARMLAND_MOISTURE), 3);
                    }
                }
            }
        }
    }

    private boolean tryPlantSeedAt(@Nonnull BlockPos plantPos, @Nonnull ItemStack seedStack)
    {
        if (seedStack.isEmpty() || !isAllowedFarmSeed(seedStack.getItem()))
        {
            return false;
        }

        if (!world.getBlockState(plantPos.below()).is(Blocks.FARMLAND))
        {
            return false;
        }

        if (!clearFarmVegetationAt(plantPos))
        {
            return false;
        }

        if (!world.getBlockState(plantPos).isAir())
        {
            return false;
        }

        if (!blockling.getEquipment().take(seedStack.copyWithCount(1)))
        {
            return false;
        }

        Block cropBlock = BlockUtil.getPlantBlockForSeed(seedStack.getItem());
        if (cropBlock == Blocks.AIR)
        {
            blockling.getEquipment().addItem(seedStack.copyWithCount(1));
            return false;
        }

        BlockState toPlace = cropBlock.defaultBlockState();
        if (!toPlace.canSurvive(world, plantPos))
        {
            blockling.getEquipment().addItem(seedStack.copyWithCount(1));
            return false;
        }

        world.setBlock(plantPos, toPlace, 3);
        world.gameEvent(blockling, GameEvent.BLOCK_PLACE, plantPos);
        return true;
    }

    /** @return true if the block is now farmland. */
    private boolean ensureFarmland(@Nonnull BlockPos soilPos)
    {
        BlockState state = world.getBlockState(soilPos);
        if (state.is(Blocks.FARMLAND))
        {
            return true;
        }

        Block block = state.getBlock();
        if (block != Blocks.DIRT && block != Blocks.GRASS_BLOCK && block != Blocks.DIRT_PATH)
        {
            return false;
        }

        // Same as tickTill: grab a hoe from the inventory even without AUTOSWITCH.
        if (!ToolUtil.isHoe(blockling.getMainHandItem()) && !ToolUtil.isHoe(blockling.getOffhandItem())
                || blockling.getSkills().getSkill(GeneralSkills.AUTOSWITCH).isBought())
        {
            blockling.getEquipment().trySwitchToBestTool(BlocklingHand.BOTH, new ToolContext(ToolType.HOE, state));
        }

        if (!ToolUtil.isHoe(blockling.getMainHandItem()) && !ToolUtil.isHoe(blockling.getOffhandItem()))
        {
            return false;
        }

        com.willr27.blocklings.command.BlocklingTaskLogger.event(
                blockling, "TILL", block + " -> farmland at " + soilPos.toShortString());

        world.playSound(null, soilPos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0f, 1.0f);
        world.setBlock(soilPos, Blocks.FARMLAND.defaultBlockState(), 11);
        world.gameEvent(blockling, GameEvent.BLOCK_CHANGE, soilPos);

        ItemStack main = blockling.getMainHandItem();
        ItemStack off = blockling.getOffhandItem();
        if (ToolUtil.isHoe(main))
        {
            ToolUtil.damageTool(main, blockling, 1);
        }
        else if (ToolUtil.isHoe(off))
        {
            ToolUtil.damageTool(off, blockling, 1);
        }

        return true;
    }

    @Override
    public boolean tryRecalcTarget()
    {
        if (hasTarget() && isCurrentWorkStillValid())
        {
            return true;
        }

        if (hasTarget())
        {
            markTargetBad();
        }

        if (tryFindCrop())
        {
            farmWork = FarmWork.HARVEST;
            return setPathToTarget();
        }

        // Fill existing empty farmland first (player fields + already tilled plots).
        if (hasReplanter() && tryFindPlantSpot())
        {
            farmWork = FarmWork.PLANT;
            return setPathToTarget();
        }

        // Expand the farm as a compact rectangle up to min(64, total seeds).
        if (tryFindTillSpot())
        {
            farmWork = FarmWork.TILL;
            return setPathToTarget();
        }

        // Place water from a bucket so dry farmland hydrates (Replanter only).
        if (hasReplanter() && tryFindIrrigateSpot())
        {
            farmWork = FarmWork.IRRIGATE;
            return setPathToTarget();
        }

        return false;
    }

    private boolean setPathToTarget()
    {
        Pair<BlockPos, Path> path = findPathTo(getTarget());
        if (path != null)
        {
            setPathTargetPos(path.getKey(), path.getValue());
            return true;
        }
        markEntireTargetBad();
        return false;
    }

    @Override
    protected boolean recalcPath(boolean force)
    {
        Pair<BlockPos, Path> result = findPathTo(getTarget());
        if (result != null)
        {
            setPathTargetPos(result.getKey(), result.getValue());
            return true;
        }
        setPathTargetPos(null, null);
        return false;
    }

    private boolean isCurrentWorkStillValid()
    {
        BlockPos target = getTarget();
        if (target == null)
        {
            return false;
        }

        return switch (farmWork)
        {
            case HARVEST -> isValidHarvestTarget(target);
            case PLANT -> hasReplanter() && isValidPlantSpot(target) && !findWhitelistedPlantableSeed().isEmpty();
            case TILL -> isValidTillSpot(target) && hasHoeReady() && canTillMore();
            case IRRIGATE -> hasReplanter() && hasWaterBucket() && isValidWaterPlacement(target);
        };
    }

    private boolean hasReplanter()
    {
        return blockling.getSkills().getSkill(FarmingSkills.REPLANTER).isBought();
    }

    private boolean tryFindCrop()
    {
        BlockPos closest = findBest(this::isValidHarvestTarget, pos ->
                blockling.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        if (closest != null)
        {
            setTarget(closest);
            return true;
        }
        return false;
    }

    private boolean tryFindIrrigateSpot()
    {
        if (!hasWaterBucket())
        {
            return false;
        }

        // Only place water when dry farmland exists outside moisture range of any water.
        BlockPos dry = findBest(this::isDryFarmlandNeedingWater, pos ->
                blockling.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        if (dry == null)
        {
            return false;
        }

        BlockPos waterPos = findBestWaterPlacementNear(dry);
        if (waterPos == null)
        {
            return false;
        }

        setTarget(waterPos);
        return true;
    }

    private boolean tryFindPlantSpot()
    {
        // Only plant with Replanter + a whitelisted seed in inventory.
        if (findWhitelistedPlantableSeed().isEmpty())
        {
            return false;
        }

        // Prefer farmland closest to water (moisture), then closest to blockling.
        BlockPos best = findBest(this::isValidPlantSpot, this::plantScore);
        if (best != null)
        {
            setTarget(best);
            return true;
        }
        return false;
    }

    private boolean tryFindTillSpot()
    {
        if (!hasHoeReady() || !canTillMore())
        {
            return false;
        }

        if (findWhitelistedPlantableSeed().isEmpty())
        {
            return false;
        }

        BlockPos best = findBest(this::isValidTillSpot, this::tillScore);
        if (best != null)
        {
            setTarget(best);
            return true;
        }
        return false;
    }

    /**
     * Lower score = better. Plant: prefer moist plots near water, then closest to blockling.
     */
    private double plantScore(@Nonnull BlockPos farmlandPos)
    {
        int waterDist = distanceToWater(farmlandPos);
        double toBlockling = blockling.distanceToSqr(farmlandPos.getX() + 0.5, farmlandPos.getY() + 0.5, farmlandPos.getZ() + 0.5);
        return waterDist * 1000.0 + toBlockling;
    }

    /**
     * Lower score = better. Builds a compact square/rectangle:
     * prefer water, same Y, adjacent fill that grows the smaller side of the farm bounds.
     */
    private double tillScore(@Nonnull BlockPos soilPos)
    {
        int waterDist = distanceToWater(soilPos);
        double toBlockling = blockling.distanceToSqr(soilPos.getX() + 0.5, soilPos.getY() + 0.5, soilPos.getZ() + 0.5);
        FarmBounds bounds = getFarmlandBounds();

        if (bounds == null)
        {
            // First plot: start near water when possible, otherwise near the blockling.
            return waterDist * 1000.0 + toBlockling;
        }

        double score = waterDist * 200.0 + toBlockling * 0.05;

        if (soilPos.getY() != bounds.minY)
        {
            score += 100000.0;
        }

        // Prefer completing a rectangle: grow the side that keeps width≈depth (toward a square),
        // and always prefer cells that touch the current bounds (already required by isValidTillSpot).
        int width = bounds.maxX - bounds.minX + 1;
        int depth = bounds.maxZ - bounds.minZ + 1;
        boolean extendsX = soilPos.getX() < bounds.minX || soilPos.getX() > bounds.maxX;
        boolean extendsZ = soilPos.getZ() < bounds.minZ || soilPos.getZ() > bounds.maxZ;

        if (extendsX && extendsZ)
        {
            // Diagonal corner jump — messy; avoid.
            score += 80000.0;
        }
        else if (extendsX)
        {
            // Growing X is better when width <= depth (toward square / filled row).
            score += width <= depth ? 0.0 : 5000.0;
            // Prefer staying within current Z span (straight rectangle edge).
            if (soilPos.getZ() < bounds.minZ || soilPos.getZ() > bounds.maxZ)
            {
                score += 40000.0;
            }
        }
        else if (extendsZ)
        {
            score += depth <= width ? 0.0 : 5000.0;
            if (soilPos.getX() < bounds.minX || soilPos.getX() > bounds.maxX)
            {
                score += 40000.0;
            }
        }
        else
        {
            // Inside the bounding box hole (rare) — fill first.
            score -= 1000.0;
        }

        return score;
    }

    @Nullable
    private BlockPos findBest(@Nonnull java.util.function.Predicate<BlockPos> predicate,
                              @Nonnull java.util.function.ToDoubleFunction<BlockPos> scoreFn)
    {
        BlockPos origin = blockling.blockPosition();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        for (int i = -SEARCH_RADIUS_X; i <= SEARCH_RADIUS_X; i++)
        {
            for (int j = -SEARCH_RADIUS_Y; j <= SEARCH_RADIUS_Y; j++)
            {
                for (int k = -SEARCH_RADIUS_X; k <= SEARCH_RADIUS_X; k++)
                {
                    BlockPos pos = origin.offset(i, j, k);
                    if (!predicate.test(pos))
                    {
                        continue;
                    }

                    double score = scoreFn.applyAsDouble(pos);
                    if (score < bestScore)
                    {
                        bestScore = score;
                        best = pos;
                    }
                }
            }
        }

        return best;
    }

    /**
     * Stop tilling once total farmland in range reaches {@code min(64, total allowed seeds)}.
     * Example: 125 seeds of mixed types → at most 64 farmland blocks, then plant only.
     */
    private boolean canTillMore()
    {
        int tillCap = getTillCap();
        if (tillCap <= 0)
        {
            return false;
        }

        return countFarmlandInRange() < tillCap;
    }

    /**
     * Combined seed count of every allowed type, capped at 64.
     * Wheat + carrot + pumpkin + … all share the same 64-plot budget.
     */
    private int getTillCap()
    {
        return Math.min(MAX_FARM_PLOTS, countAllowedPlantableSeeds());
    }

    /**
     * Grass, tall grass, ferns, flowers — only cleared on the plot currently being tilled/planted.
     */
    private static boolean isClearableFarmVegetation(@Nonnull BlockState state)
    {
        return state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(BlockTags.FLOWERS);
    }

    /** Air or grass the farmer may remove on the working plot (never crops). */
    private boolean isOpenForFarming(@Nonnull BlockPos pos)
    {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || isClearableFarmVegetation(state);
    }

    /**
     * True if the block is a crop / plantation that must not be broken by tilling or grass-clear.
     */
    private boolean isProtectedPlant(@Nonnull BlockState state)
    {
        if (state.isAir() || isClearableFarmVegetation(state))
        {
            return false;
        }

        Block block = state.getBlock();
        if (block instanceof CropBlock)
        {
            return true;
        }

        return block == Blocks.PUMPKIN_STEM
                || block == Blocks.MELON_STEM
                || block == Blocks.ATTACHED_PUMPKIN_STEM
                || block == Blocks.ATTACHED_MELON_STEM
                || block == Blocks.TORCHFLOWER_CROP
                || block == Blocks.PITCHER_CROP
                || block == Blocks.SWEET_BERRY_BUSH
                || BlockUtil.isCrop(block);
    }

    /** Clears grass/tall grass above soil only. Never touches protected crops. */
    private boolean clearFarmVegetationAbove(@Nonnull BlockPos soilPos)
    {
        return clearFarmVegetationAt(soilPos.above());
    }

    /**
     * Breaks clearable vegetation at {@code pos} (and the block above for double plants).
     * @return true if {@code pos} is air afterwards, or was already open.
     */
    private boolean clearFarmVegetationAt(@Nonnull BlockPos pos)
    {
        BlockState state = world.getBlockState(pos);
        if (state.isAir())
        {
            return true;
        }

        if (isProtectedPlant(state))
        {
            return false;
        }

        if (!isClearableFarmVegetation(state))
        {
            return false;
        }

        com.willr27.blocklings.command.BlocklingTaskLogger.event(
                blockling, "CLEAR", state.getBlock() + " at " + pos.toShortString());

        world.destroyBlock(pos, true);
        world.gameEvent(blockling, GameEvent.BLOCK_DESTROY, pos);
        world.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);

        BlockPos above = pos.above();
        BlockState upper = world.getBlockState(above);
        if (isClearableFarmVegetation(upper) && !isProtectedPlant(upper))
        {
            world.destroyBlock(above, true);
            world.gameEvent(blockling, GameEvent.BLOCK_DESTROY, above);
        }

        return world.getBlockState(pos).isAir();
    }

    private boolean isAdjacentToFarmland(@Nonnull BlockPos soilPos)
    {
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST})
        {
            if (world.getBlockState(soilPos.relative(dir)).is(Blocks.FARMLAND))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Bounding box of farmland in search range — used to grow a clean rectangle/square.
     */
    @Nullable
    private FarmBounds getFarmlandBounds()
    {
        BlockPos origin = blockling.blockPosition();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        boolean found = false;

        for (int i = -SEARCH_RADIUS_X; i <= SEARCH_RADIUS_X; i++)
        {
            for (int j = -SEARCH_RADIUS_Y; j <= SEARCH_RADIUS_Y; j++)
            {
                for (int k = -SEARCH_RADIUS_X; k <= SEARCH_RADIUS_X; k++)
                {
                    BlockPos pos = origin.offset(i, j, k);
                    if (!world.getBlockState(pos).is(Blocks.FARMLAND))
                    {
                        continue;
                    }

                    found = true;
                    minX = Math.min(minX, pos.getX());
                    minY = Math.min(minY, pos.getY());
                    minZ = Math.min(minZ, pos.getZ());
                    maxX = Math.max(maxX, pos.getX());
                    maxZ = Math.max(maxZ, pos.getZ());
                }
            }
        }

        return found ? new FarmBounds(minX, minY, minZ, maxX, maxZ) : null;
    }

    private static final class FarmBounds
    {
        final int minX;
        final int minY;
        final int minZ;
        final int maxX;
        final int maxZ;

        FarmBounds(int minX, int minY, int minZ, int maxX, int maxZ)
        {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxZ = maxZ;
        }
    }

    /**
     * Seed count used for the tilling cap and for deciding whether planting/tilling can start.
     * Respects the farming Seed Whitelist skill live (not only the cached unlock flag).
     */
    private int countAllowedPlantableSeeds()
    {
        int total = 0;
        for (int i = 0; i < blockling.getEquipment().getContainerSize(); i++)
        {
            ItemStack stack = blockling.getEquipment().getItem(i);
            if (stack.isEmpty() || !isAllowedFarmSeed(stack.getItem()))
            {
                continue;
            }
            total += stack.getCount();
        }
        return total;
    }

    /**
     * Without Seed Whitelist skill → all plantable seeds allowed.
     * With skill → only whitelist-enabled seeds (wheat disabled = not counted / not planted).
     * Crop whitelist (when unlocked) must also allow the crop that seed grows into.
     */
    private boolean isAllowedFarmSeed(@Nonnull Item item)
    {
        if (!isPlantableSeedItem(item))
        {
            return false;
        }

        // Keep GoalWhitelist unlock in sync with the skill (fixes stale locked=allow-all).
        boolean skillBought = blockling.getSkills().getSkill(FarmingSkills.SEED_WHITELIST).isBought();
        if (seedWhitelist.isUnlocked() != skillBought)
        {
            seedWhitelist.setIsUnlocked(skillBought, false);
        }

        if (skillBought)
        {
            Boolean enabled = seedWhitelist.get(RegistryUtil.itemId(item));
            if (enabled != null)
            {
                if (!enabled)
                {
                    return false;
                }
            }
            else if (!seedWhitelist.isEmpty())
            {
                return false;
            }
        }

        Block plant = BlockUtil.getPlantBlockForSeed(item);
        Block cropForWhitelist = cropBlockForWhitelist(plant);
        return isCropAllowedByWhitelist(cropForWhitelist) || isCropAllowedByWhitelist(plant);
    }

    /**
     * Stem/fruit crops are stored as pumpkin/melon in the crop whitelist, not as stems.
     */
    @Nonnull
    private static Block cropBlockForWhitelist(@Nonnull Block plant)
    {
        if (plant == Blocks.PUMPKIN_STEM || plant == Blocks.ATTACHED_PUMPKIN_STEM)
        {
            return Blocks.PUMPKIN;
        }
        if (plant == Blocks.MELON_STEM || plant == Blocks.ATTACHED_MELON_STEM)
        {
            return Blocks.MELON;
        }
        if (plant == Blocks.TORCHFLOWER)
        {
            return Blocks.TORCHFLOWER_CROP;
        }
        if (plant == Blocks.PITCHER_PLANT)
        {
            return Blocks.PITCHER_CROP;
        }
        return plant;
    }

    /**
     * Without Crop Whitelist skill → all crops allowed.
     * With skill → only enabled crop entries.
     */
    private boolean isCropAllowedByWhitelist(@Nonnull Block block)
    {
        boolean skillBought = blockling.getSkills().getSkill(FarmingSkills.CROP_WHITELIST).isBought();
        if (cropWhitelist.isUnlocked() != skillBought)
        {
            cropWhitelist.setIsUnlocked(skillBought, false);
        }

        if (!skillBought)
        {
            return true;
        }

        Boolean enabled = cropWhitelist.get(RegistryUtil.blockId(block));
        if (enabled != null)
        {
            return enabled;
        }
        return cropWhitelist.isEmpty();
    }

    /** Farmland (empty or with crops) in search radius — used for till scoring adjacency. */
    private int countFarmlandInRange()
    {
        BlockPos origin = blockling.blockPosition();
        int count = 0;

        for (int i = -SEARCH_RADIUS_X; i <= SEARCH_RADIUS_X; i++)
        {
            for (int j = -SEARCH_RADIUS_Y; j <= SEARCH_RADIUS_Y; j++)
            {
                for (int k = -SEARCH_RADIUS_X; k <= SEARCH_RADIUS_X; k++)
                {
                    BlockPos pos = origin.offset(i, j, k);
                    if (world.getBlockState(pos).is(Blocks.FARMLAND))
                    {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    /**
     * Chebyshev distance to nearest water. Returns a large value if none nearby
     * so dry spots are still tillable, just scored worse than moist ones.
     */
    private int distanceToWater(@Nonnull BlockPos pos)
    {
        int best = 64;
        for (int x = -WATER_RANGE; x <= WATER_RANGE; x++)
        {
            for (int y = -1; y <= 1; y++)
            {
                for (int z = -WATER_RANGE; z <= WATER_RANGE; z++)
                {
                    BlockPos around = pos.offset(x, y, z);
                    if (world.getFluidState(around).is(FluidTags.WATER) || world.getBlockState(around).is(Blocks.WATER))
                    {
                        int d = Math.max(Math.abs(x), Math.abs(z));
                        if (d < best)
                        {
                            best = d;
                        }
                    }
                }
            }
        }
        return best;
    }

    @Nullable
    private Pair<BlockPos, Path> findPathTo(@Nullable BlockPos target)
    {
        if (target == null)
        {
            return null;
        }

        // Plant/till/irrigate targets are soil/water holes. Path to the air above so the blockling can stand
        // on the plot — createPathTo often rejects paths that end on soil.above() when the
        // destination is the soil itself, which broke replanting after mobs destroy crops.
        BlockPos pathPos = target;
        if (farmWork == FarmWork.PLANT || farmWork == FarmWork.TILL || farmWork == FarmWork.IRRIGATE)
        {
            pathPos = target.above();
        }
        else if (BlockUtil.areAllAdjacentBlocksSolid(world, target))
        {
            return null;
        }

        if (isBadPathTargetPos(pathPos))
        {
            return null;
        }

        Path path = EntityUtil.createPathTo(blockling, pathPos, getRangeSq());
        if (path != null)
        {
            return new MutablePair<>(pathPos, path);
        }

        return null;
    }

    private boolean isValidHarvestTarget(@Nullable BlockPos target)
    {
        if (!isValidTargetPos(target) || target == null)
        {
            return false;
        }

        BlockState blockState = world.getBlockState(target);
        Block block = blockState.getBlock();

        if (!isCropAllowedByWhitelist(block))
        {
            return false;
        }

        if (!canHarvestPos(target))
        {
            return false;
        }

        // Pumpkin/melon STEMS are never harvest targets — only the ripe fruit block is. A stem is not
        // a CropBlock, so it would otherwise skip the maturity check and be "harvested" the instant it
        // is planted, breaking the sprout the farmer just made and wasting the seed (plant/break loop).
        if (block == Blocks.PUMPKIN_STEM || block == Blocks.MELON_STEM
                || block == Blocks.ATTACHED_PUMPKIN_STEM || block == Blocks.ATTACHED_MELON_STEM)
        {
            return false;
        }

        if (block instanceof CropBlock cropBlock && !cropBlock.isMaxAge(blockState))
        {
            return false;
        }

        return true;
    }

    private boolean isValidPlantSpot(@Nullable BlockPos farmlandPos)
    {
        if (!isValidTargetPos(farmlandPos) || farmlandPos == null)
        {
            return false;
        }

        if (!world.getBlockState(farmlandPos).is(Blocks.FARMLAND))
        {
            return false;
        }

        // Never plant over / clear an existing plantation.
        BlockState above = world.getBlockState(farmlandPos.above());
        if (isProtectedPlant(above))
        {
            return false;
        }

        return isOpenForFarming(farmlandPos.above());
    }

    private boolean isValidTillSpot(@Nullable BlockPos pos)
    {
        if (!isValidTargetPos(pos) || pos == null)
        {
            return false;
        }

        Block block = world.getBlockState(pos).getBlock();
        if (block != Blocks.DIRT && block != Blocks.GRASS_BLOCK && block != Blocks.DIRT_PATH)
        {
            return false;
        }

        BlockState above = world.getBlockState(pos.above());
        // Respect player plantations — never hoe under an existing crop.
        if (isProtectedPlant(above))
        {
            return false;
        }

        if (!isOpenForFarming(pos.above()))
        {
            return false;
        }

        if (!canTillMore())
        {
            return false;
        }

        // First plot anywhere (score prefers water). Afterwards only adjacent → clean rectangle.
        if (countFarmlandInRange() == 0)
        {
            return true;
        }

        return isAdjacentToFarmland(pos);
    }

    @Nonnull
    private ItemStack findWhitelistedPlantableSeed()
    {
        int size = blockling.getEquipment().getContainerSize();
        if (size <= 0)
        {
            return ItemStack.EMPTY;
        }

        // Round-robin so mixed seed stacks (wheat, carrot, pumpkin…) are all used.
        for (int n = 0; n < size; n++)
        {
            int i = Math.floorMod(seedScanIndex + n, size);
            ItemStack stack = blockling.getEquipment().getItem(i);
            if (stack.isEmpty() || !isAllowedFarmSeed(stack.getItem()))
            {
                continue;
            }

            seedScanIndex = Math.floorMod(i + 1, size);
            return stack.copyWithCount(1);
        }
        return ItemStack.EMPTY;
    }

    private boolean hasWaterBucket()
    {
        for (int i = 0; i < blockling.getEquipment().getContainerSize(); i++)
        {
            if (blockling.getEquipment().getItem(i).is(Items.WATER_BUCKET))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isDryFarmlandNeedingWater(@Nullable BlockPos pos)
    {
        if (!isValidTargetPos(pos) || pos == null)
        {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        if (!state.is(Blocks.FARMLAND) || !state.hasProperty(BlockStateProperties.MOISTURE))
        {
            return false;
        }

        if (state.getValue(BlockStateProperties.MOISTURE) >= MAX_FARMLAND_MOISTURE)
        {
            return false;
        }

        // Already within moisture range of water — vanilla will hydrate it.
        return distanceToWater(pos) > WATER_RANGE;
    }

    @Nullable
    private BlockPos findBestWaterPlacementNear(@Nonnull BlockPos dryFarmland)
    {
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        for (int x = -WATER_RANGE; x <= WATER_RANGE; x++)
        {
            for (int y = -1; y <= 1; y++)
            {
                for (int z = -WATER_RANGE; z <= WATER_RANGE; z++)
                {
                    BlockPos candidate = dryFarmland.offset(x, y, z);
                    if (!isValidWaterPlacement(candidate))
                    {
                        continue;
                    }

                    // Prefer a center-like hole: more neighboring farmland = better.
                    int farmNeighbors = 0;
                    for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST})
                    {
                        if (world.getBlockState(candidate.relative(dir)).is(Blocks.FARMLAND))
                        {
                            farmNeighbors++;
                        }
                    }

                    double toBlockling = blockling.distanceToSqr(candidate.getX() + 0.5, candidate.getY() + 0.5, candidate.getZ() + 0.5);
                    double score = -farmNeighbors * 1000.0 + toBlockling;
                    if (score < bestScore)
                    {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }
        }

        return best;
    }

    private boolean isValidWaterPlacement(@Nullable BlockPos pos)
    {
        if (!isValidTargetPos(pos) || pos == null)
        {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        if (state.is(Blocks.WATER) || world.getFluidState(pos).is(FluidTags.WATER))
        {
            return false;
        }

        // Sacrifice empty tillable soil for a water source — never a plot with crops.
        if (state.is(Blocks.FARMLAND) || state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH))
        {
            BlockState above = world.getBlockState(pos.above());
            if (isProtectedPlant(above) || !isOpenForFarming(pos.above()))
            {
                return false;
            }
        }
        else if (state.isAir())
        {
            BlockState below = world.getBlockState(pos.below());
            if (!below.isFaceSturdy(world, pos.below(), Direction.UP))
            {
                return false;
            }
        }
        else
        {
            return false;
        }

        // Must hydrate at least one dry plot that currently has no water in range.
        for (int x = -WATER_RANGE; x <= WATER_RANGE; x++)
        {
            for (int y = -1; y <= 1; y++)
            {
                for (int z = -WATER_RANGE; z <= WATER_RANGE; z++)
                {
                    BlockPos around = pos.offset(x, y, z);
                    BlockState farm = world.getBlockState(around);
                    if (farm.is(Blocks.FARMLAND) && farm.hasProperty(BlockStateProperties.MOISTURE)
                            && farm.getValue(BlockStateProperties.MOISTURE) < MAX_FARMLAND_MOISTURE
                            && distanceToWater(around) > WATER_RANGE)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isPlantableSeedItem(@Nonnull Item item)
    {
        return BlockUtil.isPlantableFarmSeed(item);
    }

    private boolean hasHoeReady()
    {
        // A hoe anywhere in the inventory is enough — tilling now swaps to it even without AUTOSWITCH.
        // Non-mutating scan (findBestToolsToSwitchTo transiently swaps slots and must not run in a check).
        for (int i = 0; i < blockling.getEquipment().getContainerSize(); i++)
        {
            if (ToolUtil.isHoe(blockling.getEquipment().getItem(i)))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void checkForAndHandleInvalidTargets()
    {
        if (!isCurrentWorkStillValid())
        {
            markEntireTargetBad();
        }
    }

    @Override
    public void markEntireTargetBad()
    {
        if (hasTarget())
        {
            markBad(getTarget());
        }
    }

    @Override
    protected boolean isValidTargetBlock(@Nonnull Block block)
    {
        return isCropAllowedByWhitelist(block);
    }

    @Override
    protected boolean isValidPathTargetPos(@Nonnull BlockPos blockPos)
    {
        if (!hasTarget())
        {
            return false;
        }

        BlockPos target = getTarget();
        // Plant/till path to the air above the soil.
        return target.equals(blockPos) || target.above().equals(blockPos);
    }

    @Override
    public boolean isValidTarget(@Nullable BlockPos target)
    {
        return switch (farmWork)
        {
            case HARVEST -> isValidHarvestTarget(target);
            case PLANT -> hasReplanter() && isValidPlantSpot(target) && !findWhitelistedPlantableSeed().isEmpty();
            case TILL -> isValidTillSpot(target) && hasHoeReady() && canTillMore();
            case IRRIGATE -> hasReplanter() && hasWaterBucket() && isValidWaterPlacement(target);
        };
    }

    @Nonnull
    @Override
    protected ToolType getToolType()
    {
        return ToolType.HOE;
    }

    @Override
    public float getRangeSq()
    {
        return blockling.getStats().farmingRangeSq.getValue();
    }
}
