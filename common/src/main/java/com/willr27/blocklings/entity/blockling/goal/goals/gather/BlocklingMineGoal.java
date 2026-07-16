package com.willr27.blocklings.entity.blockling.goal.goals.gather;

import com.mojang.datafixers.util.Pair;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingHand;
import com.willr27.blocklings.entity.blockling.skill.skills.MiningSkills;
import com.willr27.blocklings.entity.blockling.task.BlocklingTasks;
import com.willr27.blocklings.entity.blockling.goal.config.whitelist.GoalWhitelist;
import com.willr27.blocklings.entity.blockling.goal.config.whitelist.Whitelist;
import com.willr27.blocklings.util.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Mines the targeted ore/vein — finishes the whole vein before looking for another.
 */
public class BlocklingMineGoal extends BlocklingGatherGoal
{
    /**
     * The x and z search radius.
     */
    private static final int SEARCH_RADIUS_X = 10;

    /**
     * The y search radius (caves / vertical veins).
     */
    private static final int SEARCH_RADIUS_Y = 10;

    /**
     * The max number of blocks that can be part of a vein.
     */
    private static final int MAX_VEIN_SIZE = 64;

    /**
     * The list of block positions in the current vein.
     */
    @Nonnull
    public final List<BlockPos> veinBlockPositions = new ArrayList<>();

    /**
     * The ore whitelist.
     */
    @Nonnull
    public final GoalWhitelist oreWhitelist;

    /**
     * The set of positions we have attempted to use as path targets so far.
     */
    @Nonnull
    private final Set<BlockPos> pathTargetPositionsTested = new HashSet<>();

    /**
     * Last ore mined — next pick prefers face-adjacent vein members.
     */
    @Nullable
    private BlockPos lastMinedPos;

    /**
     * @param id the id associated with the owning task of this goal.
     * @param blockling the blockling the goal is assigned to.
     * @param tasks the associated tasks.
     */
    public BlocklingMineGoal(@Nonnull UUID id, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(id, blockling, tasks);

        oreWhitelist = new GoalWhitelist("24d7135e-607b-413b-a2a7-00d19119b9de", "ores", Whitelist.Type.BLOCK, this);
        oreWhitelist.setIsUnlocked(blockling.getSkills().getSkill(MiningSkills.WHITELIST).isBought(), false);
        ensureOreWhitelistEntries(false);
        whitelists.add(oreWhitelist);

        setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
    }

    /**
     * Fills missing ore entries (enabled by default). Fixes empty Ores tab when {@code #minecraft:ores} was used.
     */
    public void ensureOreWhitelistEntries(boolean sync)
    {
        boolean added = false;
        for (Block ore : BlockUtil.ORES.get())
        {
            ResourceLocation id = RegistryUtil.blockId(ore);
            if (!oreWhitelist.containsKey(id))
            {
                oreWhitelist.put(id, true);
                added = true;
            }
        }

        if (sync && added)
        {
            int whitelistIndex = whitelists.indexOf(oreWhitelist);
            if (whitelistIndex >= 0)
            {
                new com.willr27.blocklings.network.messages.WhitelistAllMessage(
                        blockling, id, whitelistIndex, oreWhitelist).sync();
            }
        }
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

        veinBlockPositions.clear();
        lastMinedPos = null;
        pathTargetPositionsTested.clear();
    }

    @Override
    protected void tickGather()
    {
        super.tickGather();

        ItemStack mainStack = blockling.getMainHandItem();
        ItemStack offStack = blockling.getOffhandItem();

        BlockPos targetPos = getTarget();
        BlockState targetBlockState = getTargetBlockState();

        boolean mainCanHarvest = ToolUtil.canToolHarvest(mainStack, targetBlockState);
        boolean offCanHarvest = ToolUtil.canToolHarvest(offStack, targetBlockState);

        if (mainCanHarvest || offCanHarvest)
        {
            blockling.getActions().gather.tryStart();

            if (blockling.getActions().gather.isRunning())
            {
                float blocklingDestroySpeed = blockling.getStats().miningSpeed.getValue();
                float mainDestroySpeed = mainCanHarvest ? ToolUtil.getToolHarvestSpeedWithEnchantments(mainStack, targetBlockState) : 0.0f;
                float offDestroySpeed = offCanHarvest ? ToolUtil.getToolHarvestSpeedWithEnchantments(offStack, targetBlockState) : 0.0f;

                float destroySpeed = blocklingDestroySpeed + mainDestroySpeed + offDestroySpeed;
                float blockStrength = targetBlockState.getDestroySpeed(world, targetPos);

                blockling.getStats().InteractionHand.setValue(BlocklingHand.fromBooleans(mainCanHarvest, offCanHarvest));

                float progress = destroySpeed / blockStrength / 100.0f;
                blockling.getActions().gather.tick(progress);

                if (blockling.getActions().gather.isFinished())
                {
                    blockling.getActions().gather.stop();
                    blockling.getStats().miningXp.incrementValue((int) (blockStrength * 2.0f));

                    for (ItemStack stack : DropUtil.getDrops(DropUtil.Context.MINING, blockling, targetPos, mainCanHarvest ? mainStack : ItemStack.EMPTY, offCanHarvest ? offStack : ItemStack.EMPTY))
                    {
                        stack = blockling.getEquipment().addItem(stack);
                        blockling.dropItemStack(stack);
                    }

                    if (ToolUtil.damageTool(mainStack, blockling, mainCanHarvest ? blockling.getSkills().getSkill(MiningSkills.HASTY).isBought() ? 2 : 1 : 0))
                    {
                        mainStack.shrink(1);
                    }

                    if (ToolUtil.damageTool(offStack, blockling, offCanHarvest ? blockling.getSkills().getSkill(MiningSkills.HASTY).isBought() ? 2 : 1 : 0))
                    {
                        offStack.shrink(1);
                    }

                    boolean minedOre = BlockUtil.isOre(targetBlockState.getBlock());

                    com.willr27.blocklings.command.BlocklingTaskLogger.event(
                            blockling, "MINE", targetBlockState.getBlock() + " at " + targetPos.toShortString());

                    world.destroyBlock(targetPos, false);
                    world.destroyBlockProgress(blockling.getId(), targetPos, -1);

                    if (minedOre)
                    {
                        blockling.incOresMinedRecently();
                        lastMinedPos = targetPos.immutable();
                        veinBlockPositions.remove(targetPos);
                    }

                    if (minedOre && blockling.getSkills().getSkill(MiningSkills.HAMMER).isBought())
                    {
                        for (BlockPos surroundingPos : BlockUtil.getSurroundingBlockPositions(targetPos))
                        {
                            if (isValidOreTarget(surroundingPos))
                            {
                                for (ItemStack stack : DropUtil.getDrops(DropUtil.Context.MINING, blockling, surroundingPos, mainCanHarvest ? mainStack : ItemStack.EMPTY, offCanHarvest ? offStack : ItemStack.EMPTY))
                                {
                                    stack = blockling.getEquipment().addItem(stack);
                                    blockling.dropItemStack(stack);
                                }

                                world.destroyBlock(surroundingPos, false);
                                veinBlockPositions.remove(surroundingPos);
                            }
                        }
                    }

                    // Pull in newly exposed neighbours, then keep mining the same vein (or tunnel to it).
                    if (minedOre && lastMinedPos != null)
                    {
                        expandVeinAround(lastMinedPos);
                    }
                    pickNextOreAfterMine();
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

    /**
     * After a successful break: target the next ore in this vein (prefer adjacent / exposed / nearest).
     */
    private void pickNextOreAfterMine()
    {
        pruneInvalidVeinOres();

        BlockPos next = findNextMineTarget();
        if (next == null)
        {
            setTarget(null);
            setPathTargetPos(null, null);
            return;
        }

        setTarget(next);

        if (isWithinMineRange(next))
        {
            setPathTargetPos(next, null);
        }
        else
        {
            Pair<BlockPos, Path> path = findPathToVein();
            if (path != null)
            {
                setPathTargetPos(path.getFirst(), path.getSecond());
            }
            else
            {
                setPathTargetPos(next, null);
            }
        }
    }

    @Override
    public void checkForAndHandleInvalidTargets()
    {
        pruneInvalidVeinOres();
    }

    private void pruneInvalidVeinOres()
    {
        for (BlockPos blockPos : new ArrayList<>(veinBlockPositions))
        {
            if (!isValidOreTarget(blockPos))
            {
                veinBlockPositions.remove(blockPos);
                // Still an ore block but disabled / unharvestable — skip for a while.
                if (!world.getBlockState(blockPos).isAir() && BlockUtil.isOre(world.getBlockState(blockPos).getBlock()))
                {
                    badTargets.add(blockPos);
                }
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

        pruneInvalidVeinOres();

        if (veinBlockPositions.isEmpty())
        {
            badTargets.clear();
            lastMinedPos = null;
            setPathTargetPos(null, null);

            if (!tryFindVein())
            {
                return false;
            }
        }
        else
        {
            absorbNearbyOres();
            pruneInvalidVeinOres();
            if (veinBlockPositions.isEmpty())
            {
                setPathTargetPos(null, null);
                return false;
            }
        }

        BlockPos next = findNextMineTarget();
        if (next == null)
        {
            veinBlockPositions.clear();
            setTarget(null);
            setPathTargetPos(null, null);
            return false;
        }

        setTarget(next);

        // Path to the same block we intend to mine — never walk toward a different ore while "mining" stone.
        if (isWithinMineRange(next))
        {
            setPathTargetPos(next, null);
        }
        else
        {
            Path path = EntityUtil.createPathTo(blockling, next, getRangeSq());
            if (path != null)
            {
                setPathTargetPos(next, path);
            }
            else
            {
                Pair<BlockPos, Path> pathToVein = findPathToVein();
                if (pathToVein == null)
                {
                    markBad(next);
                    setTarget(null);
                    setPathTargetPos(null, null);
                    return false;
                }
                setTarget(pathToVein.getFirst());
                setPathTargetPos(pathToVein.getFirst(), pathToVein.getSecond());
            }
        }

        return true;
    }

    /**
     * Next block: ore in reach first; else stone blocking that ore; else path to ore.
     */
    @Nullable
    private BlockPos findNextMineTarget()
    {
        BlockPos oreInRange = findClosestOreInRange();
        if (oreInRange != null)
        {
            return oreInRange;
        }

        BlockPos bestOre = findBestOreInVein();
        if (bestOre == null)
        {
            return null;
        }

        // Pierre devant / collée au minerai et à portée → la casser, puis le minerai.
        BlockPos blockingStone = findBlockingStoneFor(bestOre);
        if (blockingStone != null)
        {
            return blockingStone;
        }

        return bestOre;
    }

    /**
     * Soft stone that blocks access to {@code ore}: face-adjacent to the ore, or one step
     * from the blockling toward the ore. Never random cave digging.
     */
    @Nullable
    private BlockPos findBlockingStoneFor(@Nonnull BlockPos ore)
    {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        // 1) Stone stuck to the ore that we can already hit.
        for (BlockPos adj : BlockUtil.getAdjacentBlockPositions(ore))
        {
            if (!isValidTunnelTarget(adj) || !isWithinMineRange(adj))
            {
                continue;
            }
            double dist = blockling.distanceToSqr(adj.getX() + 0.5, adj.getY() + 0.5, adj.getZ() + 0.5);
            if (dist < bestDist)
            {
                bestDist = dist;
                best = adj;
            }
        }
        if (best != null)
        {
            return best;
        }

        // 2) One step toward the ore from the blockling (stone "in front").
        BlockPos feet = blockling.blockPosition();
        int sx = Integer.signum(ore.getX() - feet.getX());
        int sy = Integer.signum(ore.getY() - feet.getY());
        int sz = Integer.signum(ore.getZ() - feet.getZ());

        BlockPos[] toward = new BlockPos[]
        {
            feet.offset(sx, 0, sz),
            feet.offset(sx, 1, sz),
            feet.offset(sx, sy, sz),
            feet.above().offset(sx, 0, sz),
            feet.offset(0, sy != 0 ? sy : 1, 0)
        };

        double oreDist = blockling.distanceToSqr(ore.getX() + 0.5, ore.getY() + 0.5, ore.getZ() + 0.5);
        for (BlockPos pos : toward)
        {
            if (!isValidTunnelTarget(pos) || !isWithinMineRange(pos))
            {
                continue;
            }
            double posToOre = (pos.getX() + 0.5 - ore.getX() - 0.5) * (pos.getX() + 0.5 - ore.getX() - 0.5)
                    + (pos.getY() + 0.5 - ore.getY() - 0.5) * (pos.getY() + 0.5 - ore.getY() - 0.5)
                    + (pos.getZ() + 0.5 - ore.getZ() - 0.5) * (pos.getZ() + 0.5 - ore.getZ() - 0.5);
            if (posToOre < oreDist)
            {
                return pos;
            }
        }

        return null;
    }

    /** Closest harvestable ore in the current vein that is within mining range. */
    @Nullable
    private BlockPos findClosestOreInRange()
    {
        BlockPos closest = null;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos ore : new ArrayList<>(veinBlockPositions))
        {
            if (!isValidOreTarget(ore))
            {
                veinBlockPositions.remove(ore);
                continue;
            }
            if (!isWithinMineRange(ore))
            {
                continue;
            }

            double dist = blockling.distanceToSqr(ore.getX() + 0.5, ore.getY() + 0.5, ore.getZ() + 0.5);
            if (dist < bestDist)
            {
                bestDist = dist;
                closest = ore;
            }
        }

        return closest;
    }

    @Nullable
    private BlockPos findBestOreInVein()
    {
        BlockPos best = null;
        int bestScore = Integer.MIN_VALUE;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos ore : new ArrayList<>(veinBlockPositions))
        {
            if (!isValidOreTarget(ore))
            {
                veinBlockPositions.remove(ore);
                continue;
            }

            int score = 0;
            if (lastMinedPos != null && isFaceAdjacent(lastMinedPos, ore))
            {
                score += 100;
            }
            if (isWithinMineRange(ore))
            {
                score += 200;
            }
            if (!BlockUtil.areAllAdjacentBlocksSolid(world, ore))
            {
                score += 50;
            }

            double dist = blockling.distanceToSqr(ore.getX() + 0.5, ore.getY() + 0.5, ore.getZ() + 0.5);
            if (score > bestScore || (score == bestScore && dist < bestDist))
            {
                best = ore;
                bestScore = score;
                bestDist = dist;
            }
        }

        return best;
    }

    private static boolean isFaceAdjacent(@Nonnull BlockPos a, @Nonnull BlockPos b)
    {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        return dx + dy + dz == 1;
    }

    /** True if the blockling can mine this block from the current position (slight grace for wall ores). */
    private boolean isWithinMineRange(@Nonnull BlockPos ore)
    {
        float range = blockling.getStats().miningRange.getValue() + 0.75f;
        return blockling.distanceToSqr(ore.getX() + 0.5, ore.getY() + 0.5, ore.getZ() + 0.5) <= (double) (range * range);
    }

    /** Whitelisted harvestable ore. Respects the Ores whitelist when the skill is bought. */
    private boolean isValidOreTarget(@Nullable BlockPos pos)
    {
        if (!isValidTargetPos(pos) || pos == null)
        {
            return false;
        }
        Block block = world.getBlockState(pos).getBlock();
        return BlockUtil.isOre(block) && isOreAllowedByWhitelist(block) && canHarvestPos(pos);
    }

    /** Soft stone used only to clear a path to a known ore. */
    private boolean isValidTunnelTarget(@Nullable BlockPos pos)
    {
        if (!isValidTargetPos(pos) || pos == null)
        {
            return false;
        }
        return BlockUtil.isTunnelStone(world.getBlockState(pos).getBlock()) && canHarvestPos(pos);
    }

    /**
     * When the whitelist skill is bought, only enabled entries are mined.
     * Uses the skill flag (not only {@code isUnlocked}) to avoid client/server desync.
     */
    private boolean isOreAllowedByWhitelist(@Nonnull Block block)
    {
        boolean skillBought = blockling.getSkills().getSkill(MiningSkills.WHITELIST).isBought();
        if (skillBought && !oreWhitelist.isUnlocked())
        {
            oreWhitelist.setIsUnlocked(true, false);
        }

        if (!skillBought)
        {
            return true;
        }

        ResourceLocation id = RegistryUtil.blockId(block);
        Boolean enabled = oreWhitelist.get(id);
        if (enabled != null)
        {
            return enabled;
        }

        return oreWhitelist.isEmpty();
    }

    @Override
    public boolean isInRangeOfPathTargetPos()
    {
        BlockPos target = getTarget();
        if (target == null)
        {
            return hasPathTargetPos() && super.isInRangeOfPathTargetPos();
        }
        return isWithinMineRange(target);
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
    public void markBad(@Nonnull BlockPos blockPos)
    {
        super.markBad(blockPos);

        veinBlockPositions.remove(blockPos);
    }

    @Override
    protected boolean isValidTargetBlock(@Nonnull Block block)
    {
        if (BlockUtil.isOre(block))
        {
            return isOreAllowedByWhitelist(block);
        }
        // Stone only while we have a known vein to dig toward (not free cave mining).
        return BlockUtil.isTunnelStone(block) && !veinBlockPositions.isEmpty();
    }

    @Nonnull
    @Override
    protected ToolType getToolType()
    {
        return ToolType.PICKAXE;
    }

    /**
     * Tries to find the nearest vein.
     *
     * @return true if a vein was found.
     */
    private boolean tryFindVein()
    {
        // Existing blocklings may still have an empty ores map from the old #minecraft:ores tag.
        ensureOreWhitelistEntries(false);

        BlockPos blocklingBlockPos = blockling.blockPosition();

        List<BlockPos> bestVein = new ArrayList<>();
        Set<BlockPos> testedBlockPositions = new HashSet<>();

        double closestVeinDistSq = Double.MAX_VALUE;

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

                    if (isValidOreTarget(testBlockPos))
                    {
                        List<BlockPos> veinToTest = findVeinFrom(testBlockPos);

                        boolean canSeeVein = false;
                        double closestInVein = Double.MAX_VALUE;

                        for (BlockPos veinBlockPos : veinToTest)
                        {
                            testedBlockPositions.add(veinBlockPos);

                            if (!canSeeVein && EntityUtil.canSee(blockling, veinBlockPos))
                            {
                                canSeeVein = true;
                            }

                            double dist = blockling.distanceToSqr(veinBlockPos.getX() + 0.5, veinBlockPos.getY() + 0.5, veinBlockPos.getZ() + 0.5);
                            if (dist < closestInVein)
                            {
                                closestInVein = dist;
                            }
                        }

                        if (!canSeeVein)
                        {
                            // Cave walls often block raycasts; accept if within search radius horizontally.
                            for (BlockPos veinBlockPos : veinToTest)
                            {
                                double dx = blockling.getX() - (veinBlockPos.getX() + 0.5);
                                double dz = blockling.getZ() - (veinBlockPos.getZ() + 0.5);
                                if ((dx * dx + dz * dz) <= (double) (SEARCH_RADIUS_X * SEARCH_RADIUS_X))
                                {
                                    canSeeVein = true;
                                    break;
                                }
                            }
                        }

                        if (!canSeeVein)
                        {
                            continue;
                        }

                        boolean hasInRange = false;
                        for (BlockPos veinBlockPos : veinToTest)
                        {
                            if (isWithinMineRange(veinBlockPos))
                            {
                                hasInRange = true;
                                break;
                            }
                        }

                        // Prefer veins already in reach over distant ones.
                        double score = hasInRange ? closestInVein - 1000.0 : closestInVein;
                        if (score < closestVeinDistSq)
                        {
                            closestVeinDistSq = score;
                            bestVein = veinToTest;
                        }
                    }
                }
            }
        }

        if (!bestVein.isEmpty())
        {
            this.veinBlockPositions.clear();
            this.veinBlockPositions.addAll(bestVein);
            return true;
        }

        return false;
    }

    /**
     * BFS vein from starting ore. Connectivity uses whitelist only (not harvest / badTargets)
     * so the full patch is collected even if one block was temporarily skipped.
     */
    @Nonnull
    private List<BlockPos> findVeinFrom(@Nonnull BlockPos startingBlockPos)
    {
        List<BlockPos> vein = new ArrayList<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        queue.add(startingBlockPos);
        vein.add(startingBlockPos);

        while (!queue.isEmpty() && vein.size() < MAX_VEIN_SIZE)
        {
            BlockPos testBlockPos = queue.removeFirst();

            for (BlockPos surroundingPos : BlockUtil.getAdjacentBlockPositions(testBlockPos))
            {
                if (vein.size() >= MAX_VEIN_SIZE)
                {
                    break;
                }

                if (!isVeinOre(surroundingPos) || vein.contains(surroundingPos))
                {
                    continue;
                }

                vein.add(surroundingPos);
                queue.add(surroundingPos);
            }
        }

        return vein;
    }

    /** Ore for BFS — whitelist only, never tunnel-stone side effects. */
    private boolean isVeinOre(@Nonnull BlockPos pos)
    {
        Block block = world.getBlockState(pos).getBlock();
        return BlockUtil.isOre(block) && isOreAllowedByWhitelist(block);
    }

    /**
     * Pull any ores near the blockling into the active vein so wall iron isn't ignored
     * while stuck on a distant / wrong cluster.
     */
    private void absorbNearbyOres()
    {
        ensureOreWhitelistEntries(false);

        BlockPos origin = blockling.blockPosition();
        for (int i = -SEARCH_RADIUS_X; i <= SEARCH_RADIUS_X; i++)
        {
            for (int j = -SEARCH_RADIUS_Y; j <= SEARCH_RADIUS_Y; j++)
            {
                for (int k = -SEARCH_RADIUS_X; k <= SEARCH_RADIUS_X; k++)
                {
                    if (veinBlockPositions.size() >= MAX_VEIN_SIZE)
                    {
                        return;
                    }

                    BlockPos pos = origin.offset(i, j, k);
                    if (!isVeinOre(pos) || veinBlockPositions.contains(pos))
                    {
                        continue;
                    }

                    // Prefer absorbing ores we can already reach / see nearby.
                    if (!isWithinMineRange(pos) && blockling.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 36.0)
                    {
                        continue;
                    }

                    for (BlockPos extra : findVeinFrom(pos))
                    {
                        if (veinBlockPositions.size() >= MAX_VEIN_SIZE)
                        {
                            return;
                        }
                        if (!veinBlockPositions.contains(extra))
                        {
                            veinBlockPositions.add(extra);
                        }
                    }
                }
            }
        }
    }

    /**
     * After mining, attach newly exposed adjacent ores of the same vein type.
     */
    private void expandVeinAround(@Nonnull BlockPos minedPos)
    {
        for (BlockPos adjacent : BlockUtil.getAdjacentBlockPositions(minedPos))
        {
            if (veinBlockPositions.size() >= MAX_VEIN_SIZE)
            {
                break;
            }

            if (isVeinOre(adjacent) && !veinBlockPositions.contains(adjacent))
            {
                // Also pull the rest of any newly connected cluster.
                for (BlockPos extra : findVeinFrom(adjacent))
                {
                    if (veinBlockPositions.size() >= MAX_VEIN_SIZE)
                    {
                        break;
                    }
                    if (!veinBlockPositions.contains(extra))
                    {
                        veinBlockPositions.add(extra);
                    }
                }
            }
        }
    }

    /**
     * Path to the best ore in the vein: exposed first, then nearest.
     */
    @Nullable
    public Pair<BlockPos, Path> findPathToVein()
    {
        List<BlockPos> ordered = new ArrayList<>(veinBlockPositions);
        ordered.sort((a, b) ->
        {
            boolean aExposed = !BlockUtil.areAllAdjacentBlocksSolid(world, a);
            boolean bExposed = !BlockUtil.areAllAdjacentBlocksSolid(world, b);
            if (aExposed != bExposed)
            {
                return aExposed ? -1 : 1;
            }
            double da = blockling.distanceToSqr(a.getX() + 0.5, a.getY() + 0.5, a.getZ() + 0.5);
            double db = blockling.distanceToSqr(b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5);
            return Double.compare(da, db);
        });

        for (BlockPos veinBlockPos : ordered)
        {
            if (!isValidOreTarget(veinBlockPos))
            {
                continue;
            }

            if (BlockUtil.areAllAdjacentBlocksSolid(world, veinBlockPos))
            {
                continue;
            }

            if (isBadPathTargetPos(veinBlockPos))
            {
                continue;
            }

            Path path = EntityUtil.createPathTo(blockling, veinBlockPos, getRangeSq());

            if (path != null)
            {
                return new Pair<>(veinBlockPos, path);
            }
        }

        return null;
    }

    /**
     * Expands the tracked vein from {@code blockPos} without dropping already-known ores.
     */
    public void changeVeinRootTo(@Nonnull BlockPos blockPos)
    {
        List<BlockPos> previous = new ArrayList<>(veinBlockPositions);
        List<BlockPos> rebuilt = findVeinFrom(blockPos);

        for (BlockPos old : previous)
        {
            if (isVeinOre(old) && !rebuilt.contains(old))
            {
                rebuilt.add(old);
            }
        }

        veinBlockPositions.clear();
        veinBlockPositions.addAll(rebuilt);
    }

    @Override
    protected boolean recalcPath(boolean force)
    {
        if (hasTarget() && isWithinMineRange(getTarget()))
        {
            if (!hasPathTargetPos())
            {
                setPathTargetPos(getTarget(), null);
            }
            return true;
        }

        if (force)
        {
            Pair<BlockPos, Path> result = findPathToVein();

            if (result != null)
            {
                setPathTargetPos(result.getFirst(), result.getSecond());
                return true;
            }

            setPathTargetPos(null, null);
            return false;
        }

        for (BlockPos veinBlockPos : veinBlockPositions)
        {
            if (pathTargetPositionsTested.contains(veinBlockPos))
            {
                continue;
            }

            pathTargetPositionsTested.add(veinBlockPos);

            if (!isValidOreTarget(veinBlockPos))
            {
                continue;
            }

            if (BlockUtil.areAllAdjacentBlocksSolid(world, veinBlockPos))
            {
                continue;
            }

            Path path = EntityUtil.createPathTo(blockling, veinBlockPos, getRangeSq());

            if (path != null)
            {
                if (this.path == null || path.getDistToTarget() < this.path.getDistToTarget())
                {
                    setPathTargetPos(veinBlockPos, path);
                    return true;
                }
            }
        }

        pathTargetPositionsTested.clear();

        return hasPath() || (hasTarget() && isWithinMineRange(getTarget()));
    }

    @Override
    protected boolean isValidPathTargetPos(@Nonnull BlockPos blockPos)
    {
        if (hasTarget() && isWithinMineRange(getTarget()))
        {
            return true;
        }
        return veinBlockPositions.contains(blockPos);
    }

    @Override
    public void setPathTargetPos(@Nullable BlockPos blockPos, @Nullable Path pathToPos)
    {
        super.setPathTargetPos(blockPos, pathToPos);

        // Do not rebuild/shrink the vein on every path change — that dropped adjacent ores.
        // Only expand from the path target if it is still ore.
        if (hasPathTargetPos() && isVeinOre(getPathTargetPos()))
        {
            List<BlockPos> extra = findVeinFrom(getPathTargetPos());
            for (BlockPos pos : extra)
            {
                if (veinBlockPositions.size() >= MAX_VEIN_SIZE)
                {
                    break;
                }
                if (!veinBlockPositions.contains(pos))
                {
                    veinBlockPositions.add(pos);
                }
            }
        }
    }

    @Override
    public float getRangeSq()
    {
        return blockling.getStats().miningRangeSq.getValue();
    }

    @Nonnull
    @Override
    public String getDebugStatus()
    {
        BlockPos target = getTarget();
        String inRange = target != null ? String.valueOf(isWithinMineRange(target)) : "n/a";
        return super.getDebugStatus() + " vein=" + veinBlockPositions.size() + " inRange=" + inRange;
    }
}
