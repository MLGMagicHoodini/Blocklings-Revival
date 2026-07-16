package com.willr27.blocklings.entity.blockling.goal.goals.gather;

import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingHand;
import com.willr27.blocklings.entity.blockling.skill.skills.WoodcuttingSkills;
import com.willr27.blocklings.entity.blockling.task.BlocklingTasks;
import com.willr27.blocklings.entity.blockling.task.config.range.FloatRangeProperty;
import com.willr27.blocklings.entity.blockling.goal.config.whitelist.GoalWhitelist;
import com.willr27.blocklings.entity.blockling.goal.config.whitelist.Whitelist;
import com.willr27.blocklings.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Chops the targeted tree.
 */
public class BlocklingWoodcutGoal extends BlocklingGatherGoal
{
    /**
     * The minimum minimum number of leaves blocks for each log block to classify a tree as valid.
     */
    public static final float MIN_MIN_LEAVES_TO_LOGS_RATIO = 0.0f;

    /**
     * The maximum minimum number of leaves blocks for each log block to classify a tree as valid.
     */
    public static final float MAX_MIN_LEAVES_TO_LOGS_RATIO = 4.0f;

    /**
     * The x and z search radius. Kept modest — a full scan is O(x*x*y) blocks and runs per recalc
     * for every woodcutting blockling, so a large radius tanks the server tick with many blocklings.
     */
    private static final int SEARCH_RADIUS_X = 8;

    /**
     * The y search radius (tall dark oak canopies / floating tops after damage).
     */
    private static final int SEARCH_RADIUS_Y = 12;

    /**
     * The max number of blocks that can make up a tree's logs.
     * Dark oak / mega spruce are 2x2 trunks and easily exceed the old 30-log cap.
     */
    private static final int MAX_TREE_LOGS_SIZE = 80;

    /**
     * How far (Chebyshev) to pull in same-type logs split off by explosions / incomplete chops.
     */
    private static final int ORPHAN_LOG_RADIUS = 10;

    /**
     * Extra vertical reach so floating trunk pieces (creeper holes) stay choppable from below.
     */
    private static final float EXTRA_VERTICAL_CHOP_REACH = 12.0f;

    /**
     * The current target tree.
     */
    @Nonnull
    private final WorldUtil.Tree tree = new WorldUtil.Tree();

    /**
     * The log whitelist.
     */
    @Nonnull
    public final GoalWhitelist logWhitelist;

    /**
     * The set of positions we have attempted to use as path targets so far.
     */
    @Nonnull
    private final Set<BlockPos> pathTargetPositionsTested = new HashSet<>();

    /**
     * Ground-level log positions of the current tree (min Y). Used so Replanter plants at the
     * stump instead of mid-air where upper logs were chopped.
     */
    @Nonnull
    private final Set<BlockPos> treeBasePositions = new HashSet<>();

    /**
     * The minimum number of leaves blocks for each log block required to classify a tree as a tree.
     */
    @Nonnull
    private final FloatRangeProperty minLeavesToLogRatio;

    /**
     * @param id the id associated with the owning task of this goal.
     * @param blockling the blockling the goal is assigned to.
     * @param tasks the associated tasks.
     */
    public BlocklingWoodcutGoal(@Nonnull UUID id, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(id, blockling, tasks);

        logWhitelist = new GoalWhitelist("fbfbfd44-c1b0-4420-824a-270b34c866f7", "logs", Whitelist.Type.BLOCK, this);
        logWhitelist.setIsUnlocked(blockling.getSkills().getSkill(WoodcuttingSkills.WHITELIST).isBought(), false);
        BlockUtil.TREES.get().forEach(tree -> logWhitelist.put(RegistryUtil.blockId(tree.log), true));
        whitelists.add(logWhitelist);

        properties.add(minLeavesToLogRatio = new FloatRangeProperty(
                "689c67a9-8c02-4eac-afff-bdc4eab861c6", this,
                BlocklingsTranslationTextComponent.of("task.property.min_leaves_to_log_ratio.name"),
                BlocklingsTranslationTextComponent.of("task.property.min_leaves_to_log_ratio.desc"),
                MIN_MIN_LEAVES_TO_LOGS_RATIO, MAX_MIN_LEAVES_TO_LOGS_RATIO, BlocklingsConfig.COMMON.defaultMinLeavesToLogRatio.get().floatValue()));

        setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
    }

    @Override
    public boolean canUse()
    {
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse()
    {
        return super.canContinueToUse();
    }

    @Override
    public void stop()
    {
        super.stop();

        tree.logs.clear();
        tree.leaves.clear();
        treeBasePositions.clear();
    }

    @Override
    protected void tickGather()
    {
        super.tickGather();

        ItemStack mainStack = blockling.getMainHandItem();
        ItemStack offStack = blockling.getOffhandItem();

        BlockPos targetPos = getTarget();
        BlockState targetBlockState = getTargetBlockState();
        Block targetBlock = getTargetBlock();

        boolean mainCanHarvest = ToolUtil.canToolHarvest(mainStack, targetBlockState);
        boolean offCanHarvest = ToolUtil.canToolHarvest(offStack, targetBlockState);

        if (mainCanHarvest || offCanHarvest)
        {
            blockling.getActions().gather.tryStart();

            if (blockling.getActions().gather.isRunning())
            {
                float blocklingDestroySpeed = blockling.getStats().woodcuttingSpeed.getValue();
                float mainDestroySpeed = mainCanHarvest ? ToolUtil.getToolHarvestSpeedWithEnchantments(mainStack, targetBlockState) : 0.0f;
                float offDestroySpeed = offCanHarvest ? ToolUtil.getToolHarvestSpeedWithEnchantments(offStack, targetBlockState) : 0.0f;

                float destroySpeed = blocklingDestroySpeed + mainDestroySpeed + offDestroySpeed;
                float blockStrength = targetBlockState.getDestroySpeed(world, targetPos) + 1.5f;

                blockling.getStats().InteractionHand.setValue(BlocklingHand.fromBooleans(mainCanHarvest, offCanHarvest));

                float progress = destroySpeed / blockStrength / 100.0f;
                blockling.getActions().gather.tick(progress);

                if (blockling.getActions().gather.isFinished())
                {
                    blockling.getActions().gather.stop();
                    blockling.getStats().woodcuttingXp.incrementValue((int) blockStrength);

                    for (ItemStack stack : DropUtil.getDrops(DropUtil.Context.WOODCUTTING, blockling, targetPos, mainCanHarvest ? mainStack : ItemStack.EMPTY, offCanHarvest ? offStack : ItemStack.EMPTY))
                    {
                        stack = blockling.getEquipment().addItem(stack);
                        blockling.dropItemStack(stack);
                    }

                    if (ToolUtil.damageTool(mainStack, blockling, mainCanHarvest ? blockling.getSkills().getSkill(WoodcuttingSkills.HASTY).isBought() ? 2 : 1 : 0))
                    {
                        mainStack.shrink(1);
                    }

                    if (ToolUtil.damageTool(offStack, blockling, offCanHarvest ? blockling.getSkills().getSkill(WoodcuttingSkills.HASTY).isBought() ? 2 : 1 : 0))
                    {
                        offStack.shrink(1);
                    }

                    blockling.incLogsChoppedRecently();

                    com.willr27.blocklings.command.BlocklingTaskLogger.event(
                            blockling, "CHOP", targetBlock + " at " + targetPos.toShortString());

                    world.destroyBlock(targetPos, false);
                    world.destroyBlockProgress(blockling.getId(), targetPos, -1);

                    if (blockling.getSkills().getSkill(WoodcuttingSkills.LEAF_BLOWER).isBought())
                    {
                        for (BlockPos surroundingPos : BlockUtil.getSurroundingBlockPositions(targetPos))
                        {
                            if (isValidLeavesPos(surroundingPos))
                            {
                                if (blockling.getSkills().getSkill(WoodcuttingSkills.TREE_SURGEON).isBought())
                                {
                                    for (ItemStack stack : DropUtil.getDrops(DropUtil.Context.WOODCUTTING, blockling, surroundingPos, mainCanHarvest ? mainStack : ItemStack.EMPTY, offCanHarvest ? offStack : ItemStack.EMPTY))
                                    {
                                        stack = blockling.getEquipment().addItem(stack);
                                        blockling.dropItemStack(stack);
                                    }
                                }

                                world.destroyBlock(surroundingPos, false);
                            }
                        }
                    }

                    if (blockling.getSkills().getSkill(WoodcuttingSkills.LUMBER_AXE).isBought())
                    {
                        for (BlockPos surroundingPos : BlockUtil.getSurroundingBlockPositions(targetPos))
                        {
                            if (isValidTarget(surroundingPos))
                            {
                                for (ItemStack stack : DropUtil.getDrops(DropUtil.Context.WOODCUTTING, blockling, surroundingPos, mainCanHarvest ? mainStack : ItemStack.EMPTY, offCanHarvest ? offStack : ItemStack.EMPTY))
                                {
                                    stack = blockling.getEquipment().addItem(stack);
                                    blockling.dropItemStack(stack);
                                }

                                world.destroyBlock(surroundingPos, false);
                            }
                        }
                    }

                    // Replant at the stump (tree base), not mid-air where upper logs were.
                    // Saplings often only appear after leaves break — retry every chop while we have them.
                    if (blockling.getSkills().getSkill(WoodcuttingSkills.REPLANTER).isBought())
                    {
                        Block saplingBlock = BlockUtil.getSaplingFromLog(targetBlock);
                        if (saplingBlock != null)
                        {
                            tryReplantAtTreeBase(saplingBlock);
                        }
                    }
                }
                else
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
    @Override
    public void checkForAndHandleInvalidTargets()
    {
        for (BlockPos blockPos : new ArrayList<>(tree.logs))
        {
            if (!isValidTarget(blockPos))
            {
                markBad(blockPos);
            }
        }
    }

    @Override
    public boolean tryRecalcTarget()
    {
        if (super.tryRecalcTarget())
        {
            return true;
        }

        if (tree.logs.isEmpty())
        {
            // Previously skipped logs (stuck on floating pieces) must be retryable.
            badTargets.clear();

            if (!tryFindTree())
            {
                return false;
            }

            Pair<BlockPos, Path> pathToTree = findPathToTree();

            if (pathToTree == null)
            {
                // Already under a floating remnant — chop without a path.
                BlockPos inRange = findNextChopTargetInRange();
                if (inRange != null)
                {
                    setTarget(inRange);
                    setPathTargetPos(inRange, null);
                    return true;
                }
                return false;
            }

            setPathTargetPos(pathToTree.getKey(), pathToTree.getValue());
        }

        BlockPos nextLog = findNextChopTarget();
        if (nextLog == null)
        {
            tree.logs.clear();
            tree.leaves.clear();
            return false;
        }

        setTarget(nextLog);

        // If we can already reach it (standing under floating logs), don't require a path.
        if (isWithinChopRange(nextLog) && getPathTargetPos() == null)
        {
            setPathTargetPos(nextLog, null);
        }

        return true;
    }

    /**
     * Next log to chop: the nearest harvestable log, with lower logs preferred as a tie-break.
     * <p>
     * Nearest-first (not globally-lowest-first) is important: BFS can merge a whole birch/oak
     * grove into one 80-log "tree", and picking the globally lowest log there can select one
     * 20+ blocks away and unreachable — the blockling then paths to a nearby log but keeps its
     * far target, never gets it in range, thinks it is stuck, and loops ACTIVE/IDLE forever.
     * Targeting the closest log keeps target and reachable path consistent so it actually chops.
     */
    @Nullable
    private BlockPos findNextChopTarget()
    {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int bestY = Integer.MAX_VALUE;

        for (BlockPos log : new ArrayList<>(tree.logs))
        {
            if (!isValidTarget(log))
            {
                // Gone or not choppable anymore — drop from the active tree list.
                tree.logs.remove(log);
                continue;
            }

            double dist = blockling.distanceToSqr(log.getX() + 0.5, log.getY() + 0.5, log.getZ() + 0.5);
            if (dist < bestDist - 1.0e-4 || (Math.abs(dist - bestDist) <= 1.0e-4 && log.getY() < bestY))
            {
                best = log;
                bestDist = dist;
                bestY = log.getY();
            }
        }

        return best;
    }

    @Nullable
    private BlockPos findNextChopTargetInRange()
    {
        BlockPos best = findNextChopTarget();
        if (best != null && isWithinChopRange(best))
        {
            return best;
        }

        BlockPos closestInRange = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos log : new ArrayList<>(tree.logs))
        {
            if (!isValidTarget(log) || !isWithinChopRange(log))
            {
                continue;
            }
            double dist = blockling.distanceToSqr(log.getX() + 0.5, log.getY() + 0.5, log.getZ() + 0.5);
            if (dist < bestDist)
            {
                bestDist = dist;
                closestInRange = log;
            }
        }
        return closestInRange;
    }

    @Override
    public void markEntireTargetBad()
    {
        // Do NOT abandon the whole tree when stuck on one log (common on dark oak 2x2).
        // Only skip the current log and keep chopping the rest.
        if (hasTarget())
        {
            markBad(getTarget());
        }
    }

    @Override
    public void markBad(@Nonnull BlockPos blockPos)
    {
        super.markBad(blockPos);

        tree.logs.remove(blockPos);
        tree.leaves.remove(blockPos);
    }

    @Override
    protected boolean isValidTargetBlock(@Nonnull Block block)
    {
        return isLogAllowedByWhitelist(block);
    }

    /**
     * Without Log Whitelist skill → all whitelisted-at-construction logs allowed.
     * With skill → only enabled log entries.
     */
    private boolean isLogAllowedByWhitelist(@Nonnull Block block)
    {
        boolean skillBought = blockling.getSkills().getSkill(WoodcuttingSkills.WHITELIST).isBought();
        if (logWhitelist.isUnlocked() != skillBought)
        {
            logWhitelist.setIsUnlocked(skillBought, false);
        }

        if (!skillBought)
        {
            return true;
        }

        Boolean enabled = logWhitelist.get(RegistryUtil.blockId(block));
        if (enabled != null)
        {
            return enabled;
        }
        return logWhitelist.isEmpty();
    }

    @Nonnull
    @Override
    protected ToolType getToolType()
    {
        return ToolType.AXE;
    }

    /**
     * Tries to find the nearest tree.
     *
     * @return true if a tree was found.
     */
    private boolean tryFindTree()
    {
        BlockPos blocklingBlockPos = blockling.blockPosition();

        WorldUtil.Tree tree = new WorldUtil.Tree();
        List<BlockPos> testedBlockPositions = new ArrayList<>();

        double closestTreeDistSq = Float.MAX_VALUE;

        for (int i = -SEARCH_RADIUS_X; i <= SEARCH_RADIUS_X; i++)
        {
            for (int j = -SEARCH_RADIUS_Y; j <= SEARCH_RADIUS_Y; j++)
            {
                for (int k = -SEARCH_RADIUS_X; k <= SEARCH_RADIUS_X; k++)
                {
                    BlockPos testBlockPos = blocklingBlockPos.offset(i, j, k);

                    if (testedBlockPositions.contains(testBlockPos))
                    {
                        continue;
                    }

                    if (isValidTarget(testBlockPos))
                    {
                        WorldUtil.Tree treeToTest = findTreeFrom(testBlockPos);

                        if (!treeToTest.isValid(minLeavesToLogRatio.getValue()))
                        {
                            continue;
                        }

                        boolean canSeeTree = false;

                        for (BlockPos logBlockPos : treeToTest.logs)
                        {
                            if (!testedBlockPositions.contains(logBlockPos))
                            {
                                testedBlockPositions.add(logBlockPos);
                            }

                            if (!canSeeTree && EntityUtil.canSee(blockling, logBlockPos))
                            {
                                canSeeTree = true;
                            }
                        }

                        for (BlockPos leafBlockPos : treeToTest.leaves)
                        {
                            if (!testedBlockPositions.contains(leafBlockPos))
                            {
                                testedBlockPositions.add(leafBlockPos);
                            }

                            if (!canSeeTree && EntityUtil.canSee(blockling, leafBlockPos))
                            {
                                canSeeTree = true;
                            }
                        }

                        if (!canSeeTree)
                        {
                            // Dense canopy / floating tops often fail raycasts; accept if within search radius horizontally.
                            for (BlockPos logBlockPos : treeToTest.logs)
                            {
                                double dx = blockling.getX() - (logBlockPos.getX() + 0.5);
                                double dz = blockling.getZ() - (logBlockPos.getZ() + 0.5);
                                if ((dx * dx + dz * dz) <= (double) (SEARCH_RADIUS_X * SEARCH_RADIUS_X))
                                {
                                    canSeeTree = true;
                                    break;
                                }
                            }
                        }

                        if (!canSeeTree)
                        {
                            continue;
                        }

                        attachNearbyOrphanLogs(treeToTest);

                        for (BlockPos logBlockPos : treeToTest.logs)
                        {
                            float distanceSq = (float) blockling.distanceToSqr(logBlockPos.getX() + 0.5f, logBlockPos.getY() + 0.5f, logBlockPos.getZ() + 0.5f);

                            if (distanceSq < closestTreeDistSq)
                            {
                                closestTreeDistSq = distanceSq;
                                tree = treeToTest;

                                break;
                            }
                        }
                    }
                }
            }
        }

        if (!tree.logs.isEmpty())
        {
            this.tree.logs.clear();
            this.tree.leaves.clear();
            this.tree.logs.addAll(tree.logs);
            this.tree.leaves.addAll(tree.leaves);
            refreshTreeBasePositions();

            return true;
        }

        return false;
    }

    /**
     * Remembers the stump footprint (logs at the lowest Y) for Replanter.
     * Keeps the original stump if only floating remnants remain after the base was chopped.
     */
    private void refreshTreeBasePositions()
    {
        if (tree.logs.isEmpty())
        {
            return;
        }

        int minY = Integer.MAX_VALUE;
        for (BlockPos log : tree.logs)
        {
            minY = Math.min(minY, log.getY());
        }

        if (!treeBasePositions.isEmpty())
        {
            int existingY = treeBasePositions.iterator().next().getY();
            if (minY > existingY)
            {
                return;
            }
        }

        treeBasePositions.clear();
        for (BlockPos log : tree.logs)
        {
            if (log.getY() == minY)
            {
                treeBasePositions.add(log.immutable());
            }
        }
    }

    /**
     * Pulls in same-type log clusters nearby that are no longer BFS-connected
     * (creeper holes, player breaks, etc.) so the floating top is still chopped.
     */
    private void attachNearbyOrphanLogs(@Nonnull WorldUtil.Tree targetTree)
    {
        if (targetTree.logs.isEmpty())
        {
            return;
        }

        Block logType = world.getBlockState(targetTree.logs.get(0)).getBlock();
        Set<BlockPos> known = new HashSet<>(targetTree.logs);

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos log : targetTree.logs)
        {
            minX = Math.min(minX, log.getX());
            minY = Math.min(minY, log.getY());
            minZ = Math.min(minZ, log.getZ());
            maxX = Math.max(maxX, log.getX());
            maxY = Math.max(maxY, log.getY());
            maxZ = Math.max(maxZ, log.getZ());
        }

        minX -= ORPHAN_LOG_RADIUS;
        minY -= ORPHAN_LOG_RADIUS;
        minZ -= ORPHAN_LOG_RADIUS;
        maxX += ORPHAN_LOG_RADIUS;
        maxY += ORPHAN_LOG_RADIUS;
        maxZ += ORPHAN_LOG_RADIUS;

        for (int x = minX; x <= maxX; x++)
        {
            for (int y = minY; y <= maxY; y++)
            {
                for (int z = minZ; z <= maxZ; z++)
                {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (known.contains(pos) || world.getBlockState(pos).getBlock() != logType || !isConnectedTreeLog(pos))
                    {
                        continue;
                    }

                    if (!isNearAny(known, pos, ORPHAN_LOG_RADIUS))
                    {
                        continue;
                    }

                    WorldUtil.Tree orphan = findTreeFrom(pos);
                    for (BlockPos orphanLog : orphan.logs)
                    {
                        if (known.add(orphanLog))
                        {
                            targetTree.logs.add(orphanLog);
                        }
                    }
                    for (BlockPos orphanLeaf : orphan.leaves)
                    {
                        if (!targetTree.leaves.contains(orphanLeaf))
                        {
                            targetTree.leaves.add(orphanLeaf);
                        }
                    }
                }
            }
        }
    }

    private static boolean isNearAny(@Nonnull Set<BlockPos> positions, @Nonnull BlockPos pos, int chebyshev)
    {
        for (BlockPos other : positions)
        {
            int d = Math.max(Math.max(Math.abs(other.getX() - pos.getX()), Math.abs(other.getY() - pos.getY())), Math.abs(other.getZ() - pos.getZ()));
            if (d <= chebyshev)
            {
                return true;
            }
        }
        return false;
    }

    /** True if the blockling can chop this log (horizontal range + extra vertical for floating pieces). */
    private boolean isWithinChopRange(@Nonnull BlockPos log)
    {
        double dx = blockling.getX() - (log.getX() + 0.5);
        double dy = blockling.getEyeY() - (log.getY() + 0.5);
        double dz = blockling.getZ() - (log.getZ() + 0.5);
        float range = blockling.getStats().woodcuttingRange.getValue();
        return (dx * dx + dz * dz) <= (double) (range * range)
                && Math.abs(dy) <= (double) (range + EXTRA_VERTICAL_CHOP_REACH);
    }

    @Override
    public boolean isInRangeOfPathTargetPos()
    {
        BlockPos target = getTarget();
        if (target == null)
        {
            return hasPathTargetPos() && super.isInRangeOfPathTargetPos();
        }
        return isWithinChopRange(target);
    }

    /**
     * Plants saplings on the tree stump. Dark oak needs a 2x2 of saplings to grow later;
     * plants as many of the four base spots as inventory allows.
     */
    private boolean tryReplantAtTreeBase(@Nonnull Block saplingBlock)
    {
        ItemStack saplingStack = new ItemStack(saplingBlock);
        if (!blockling.getEquipment().has(saplingStack))
        {
            return false;
        }

        if (treeBasePositions.isEmpty())
        {
            return false;
        }

        if (BlockUtil.requiresTwoByTwoSaplings(saplingBlock))
        {
            int minX = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int y = treeBasePositions.iterator().next().getY();

            for (BlockPos base : treeBasePositions)
            {
                minX = Math.min(minX, base.getX());
                minZ = Math.min(minZ, base.getZ());
            }

            boolean plantedAny = false;
            BlockPos[] square = new BlockPos[]
            {
                new BlockPos(minX, y, minZ),
                new BlockPos(minX + 1, y, minZ),
                new BlockPos(minX, y, minZ + 1),
                new BlockPos(minX + 1, y, minZ + 1)
            };

            for (BlockPos pos : square)
            {
                if (tryPlaceSaplingAt(saplingBlock, pos))
                {
                    plantedAny = true;
                }
            }

            return plantedAny;
        }

        for (BlockPos base : treeBasePositions)
        {
            if (tryPlaceSaplingAt(saplingBlock, base))
            {
                return true;
            }
        }

        return false;
    }

    private boolean tryPlaceSaplingAt(@Nonnull Block saplingBlock, @Nonnull BlockPos pos)
    {
        BlockState state = world.getBlockState(pos);
        if (state.is(saplingBlock))
        {
            return false;
        }

        if (!state.isAir() && !state.canBeReplaced())
        {
            return false;
        }

        if (!BlockUtil.canPlaceAt(world, saplingBlock, pos))
        {
            return false;
        }

        ItemStack saplingStack = new ItemStack(saplingBlock);
        if (!blockling.getEquipment().has(saplingStack) || !blockling.getEquipment().take(saplingStack))
        {
            return false;
        }

        world.setBlock(pos, saplingBlock.defaultBlockState(), 3);
        return true;
    }

    /**
     * Finds the tree stemming from the given pos.
     * Connectivity uses log+whitelist only (not canHarvest) so a full 2x2 dark oak trunk
     * is collected even when some logs are awkward to path to yet.
     */
    @Nonnull
    private WorldUtil.Tree findTreeFrom(@Nonnull BlockPos blockPos)
    {
        return WorldUtil.findTreeFromPos(world, blockPos, MAX_TREE_LOGS_SIZE, this::isConnectedTreeLog, this::isValidLeavesPos);
    }

    /** Log belongs to the current tree scan (ignore harvestability for BFS connectivity). */
    private boolean isConnectedTreeLog(@Nonnull BlockPos pos)
    {
        if (!isValidTargetPos(pos))
        {
            return false;
        }

        Block block = world.getBlockState(pos).getBlock();
        return BlockUtil.isLog(block) && isLogAllowedByWhitelist(block);
    }

    /**
     * Finds the first valid path to the tree, not necessarily the most optimal.
     *
     * @return the path target position and the path to the tree, or null if no path could be found.
     */
    @Nullable
    public Pair<BlockPos, Path> findPathToTree()
    {
        List<BlockPos> ordered = new ArrayList<>(tree.logs);
        // Lowest first so stump is cleared, then work upward toward floating segments.
        ordered.sort((a, b) ->
        {
            int byY = Integer.compare(a.getY(), b.getY());
            if (byY != 0)
            {
                return byY;
            }
            double da = blockling.distanceToSqr(a.getX() + 0.5, a.getY() + 0.5, a.getZ() + 0.5);
            double db = blockling.distanceToSqr(b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5);
            return Double.compare(da, db);
        });

        for (BlockPos logBlockPos : ordered)
        {
            if (!isValidTarget(logBlockPos))
            {
                continue;
            }

            for (BlockPos stand : standPositionsNearLog(logBlockPos))
            {
                Path path = EntityUtil.createPathTo(blockling, stand, getRangeSq());
                if (path != null)
                {
                    return new MutablePair<>(logBlockPos, path);
                }
            }
        }

        return null;
    }

    /**
     * Places the blockling can stand to reach a log — includes solid ground under floating trunks.
     */
    @Nonnull
    private List<BlockPos> standPositionsNearLog(@Nonnull BlockPos log)
    {
        List<BlockPos> stands = new ArrayList<>();
        stands.add(log);
        stands.add(log.above());

        for (Direction dir : Direction.Plane.HORIZONTAL)
        {
            stands.add(log.relative(dir));
            stands.add(log.relative(dir).below());
        }

        // Mid-air below(dy) is not pathable — drop to solid ground under the column (and neighbours).
        for (int ox = -1; ox <= 1; ox++)
        {
            for (int oz = -1; oz <= 1; oz++)
            {
                BlockPos groundStand = findGroundStandBelow(log.offset(ox, 0, oz), 24);
                if (groundStand != null)
                {
                    stands.add(groundStand);
                }
            }
        }

        return stands;
    }

    /**
     * Walkable feet position on solid ground under {@code from}, or null if none within {@code maxDrop}.
     */
    @Nullable
    private BlockPos findGroundStandBelow(@Nonnull BlockPos from, int maxDrop)
    {
        for (int dy = 1; dy <= maxDrop; dy++)
        {
            BlockPos feet = from.below(dy);
            BlockState feetState = world.getBlockState(feet);
            BlockState belowState = world.getBlockState(feet.below());

            if (!belowState.isSolid())
            {
                continue;
            }

            if (!feetState.isAir() && feetState.blocksMotion())
            {
                continue;
            }

            BlockState headState = world.getBlockState(feet.above());
            if (!headState.isAir() && headState.blocksMotion())
            {
                continue;
            }

            return feet;
        }

        return null;
    }

    /**
     * Sets the tree's root position to the given block pos.
     * Keeps explosion-orphaned logs of the same type instead of dropping the floating top.
     */
    public void changeTreeRootTo(@Nonnull BlockPos blockPos)
    {
        Block logType = world.getBlockState(blockPos).getBlock();
        List<BlockPos> previousLogs = new ArrayList<>(tree.logs);

        WorldUtil.Tree newTree = findTreeFrom(blockPos);

        // Preserve still-existing same-type logs from before (disconnected by creeper, etc.).
        for (BlockPos old : previousLogs)
        {
            if (world.getBlockState(old).getBlock() == logType && isConnectedTreeLog(old) && !newTree.logs.contains(old))
            {
                newTree.logs.add(old);
            }
        }

        attachNearbyOrphanLogs(newTree);

        tree.logs.clear();
        tree.leaves.clear();
        tree.logs.addAll(newTree.logs);
        tree.leaves.addAll(newTree.leaves);
        refreshTreeBasePositions();
    }

    /**
     * @param blockPos the pos to check.
     * @return true if the block at the pos is leaves and has a persistent property set to false.
     */
    private boolean isValidLeavesPos(@Nonnull BlockPos blockPos)
    {
        return isValidLeaves(world.getBlockState(blockPos));
    }

    /**
     * @param blockState the blockState to check.
     * @return true if the block is a leaves block and has a persistent property set to false.
     */
    private boolean isValidLeaves(@Nonnull BlockState blockState)
    {
        return isValidLeaves(blockState.getBlock()) && (!(blockState.getBlock() instanceof LeavesBlock) || !blockState.getValue(LeavesBlock.PERSISTENT));
    }

    /**
     * @param block the block to check.
     * @return true if the block is leaves.
     */
    private boolean isValidLeaves(@Nonnull Block block)
    {
        return BlockUtil.isLeaves(block);
    }

    @Override
    protected boolean recalcPath(boolean force)
    {
        // Already under floating remnants — keep chopping without requiring a fresh path.
        if (hasTarget() && isWithinChopRange(getTarget()))
        {
            if (!hasPathTargetPos())
            {
                setPathTargetPos(getTarget(), null);
            }
            return true;
        }

        if (force)
        {
            Pair<BlockPos, Path> result = findPathToTree();

            if (result != null)
            {
                setPathTargetPos(result.getKey(), result.getValue());
                return true;
            }

            setPathTargetPos(null, null);
            return false;
        }

        // Try to improve our path each recalc by testing different logs in the tree
        for (BlockPos logBlockPos : tree.logs)
        {
            if (pathTargetPositionsTested.contains(logBlockPos))
            {
                continue;
            }

            pathTargetPositionsTested.add(logBlockPos);

            if (BlockUtil.areAllAdjacentBlocksSolid(world, logBlockPos))
            {
                continue;
            }

            for (BlockPos stand : standPositionsNearLog(logBlockPos))
            {
                Path path = EntityUtil.createPathTo(blockling, stand, getRangeSq());

                if (path != null)
                {
                    if (this.path == null || path.getDistToTarget() < this.path.getDistToTarget())
                    {
                        setPathTargetPos(logBlockPos, path);
                        return true;
                    }
                }
            }

            return hasPath();
        }

        pathTargetPositionsTested.clear();

        return hasPath() || (hasTarget() && isWithinChopRange(getTarget()));
    }

    @Override
    protected boolean isValidPathTargetPos(@Nonnull BlockPos blockPos)
    {
        // Path may end on a stand spot under a floating log; still valid if we can chop the target.
        if (hasTarget() && isWithinChopRange(getTarget()))
        {
            return true;
        }
        return tree.logs.contains(blockPos);
    }

    @Override
    public void setPathTargetPos(@Nullable BlockPos blockPos, @Nullable Path pathToPos)
    {
        super.setPathTargetPos(blockPos, pathToPos);

        if (hasPathTargetPos())
        {
            changeTreeRootTo(getPathTargetPos());
        }
    }

    @Override
    public float getRangeSq()
    {
        return blockling.getStats().woodcuttingRangeSq.getValue();
    }
}
