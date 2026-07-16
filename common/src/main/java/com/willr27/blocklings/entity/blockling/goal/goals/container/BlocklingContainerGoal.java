package com.willr27.blocklings.entity.blockling.goal.goals.container;

import net.minecraft.client.gui.GuiGraphics;
import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.capabilities.ContainerConfigureCapability;
import com.willr27.blocklings.client.gui.control.BaseControl;
import com.willr27.blocklings.client.gui.control.Control;
import com.willr27.blocklings.client.gui.control.controls.TexturedControl;
import com.willr27.blocklings.client.gui.control.controls.config.ContainerControl;
import com.willr27.blocklings.client.gui.control.controls.config.ItemsConfigurationControl;
import com.willr27.blocklings.client.gui.control.controls.panels.StackPanel;
import com.willr27.blocklings.client.gui.control.controls.panels.TabbedPanel;
import com.willr27.blocklings.client.gui.control.event.events.*;
import com.willr27.blocklings.client.gui.control.event.events.input.MouseReleasedEvent;
import com.willr27.blocklings.client.gui.texture.Textures;
import com.willr27.blocklings.client.gui.util.GuiUtil;
import com.willr27.blocklings.client.gui.util.ScissorStack;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingHand;
import com.willr27.blocklings.entity.blockling.goal.BlocklingTargetGoal;
import com.willr27.blocklings.entity.blockling.goal.config.iteminfo.ItemInfo;
import com.willr27.blocklings.entity.blockling.goal.config.iteminfo.OrderedItemInfoSet;
import com.willr27.blocklings.entity.blockling.skill.skills.GeneralSkills;
import com.willr27.blocklings.entity.blockling.task.BlocklingTasks;
import com.willr27.blocklings.entity.blockling.task.config.ItemConfigurationTypeProperty;
import com.willr27.blocklings.network.messages.GoalMessage;
import com.willr27.blocklings.util.BlocklingsTranslationTextComponent;
import com.willr27.blocklings.util.BlockUtil;
import com.willr27.blocklings.util.EntityUtil;
import com.willr27.blocklings.util.Version;
import com.willr27.blocklings.util.event.ValueChangedEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.core.Direction;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;
import com.willr27.blocklings.inventory.BlocklingItemHandler;
import com.willr27.blocklings.inventory.AbstractInventory;
import com.willr27.blocklings.inventory.EquipmentInventory;
import com.willr27.blocklings.platform.Services;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

/**
 * A base class for handling goals that involve moving to and interacting with containers.
 */
public abstract class BlocklingContainerGoal extends BlocklingTargetGoal<ContainerInfo> implements OrderedItemInfoSet.IOrderedItemInfoSetProvider
{
    /**
     * The maximum number of items that can be included in the items list.
     */
    public static final int MAX_ITEMS = 32;

    /**
     * The maximum number of containers that the blockling can interact with.
     */
    public static final int MAX_CONTAINERS = 8;

    /**
     * The list of items to use as a whitelist.
     */
    @Nonnull
    public final OrderedItemInfoSet itemInfoSet;

    /**
     * The list of containers that the blockling can interact with in priority order.
     */
    @Nonnull
    protected final List<ContainerInfo> containerInfos = new ArrayList<>();

    /**
     * The property used to select the type of item configuration to use.
     */
    @Nonnull
    public final ItemConfigurationTypeProperty itemConfigurationTypeProperty;

    /**
     * The container control used for the items tab in the configuration screen.
     */
    private BaseControl itemsContainer;

    /**
     * The maximum search range squared for finding configured containers.
     */
    private static final float SEARCH_RANGE_SQ = 256.0f;

    /**
     * Interact range squared (~1.5 blocks) — must stand next to the container.
     */
    private static final float INTERACT_RANGE_SQ = 2.25f;

    /**
     * Ticks between each item transfer (~0.4s). Arm swing matches this interval.
     */
    private static final int TRANSFER_INTERVAL_TICKS = 8;

    /**
     * Ticks to play the gather/open animation before transferring.
     */
    private static final int OPEN_ANIM_TICKS = 6;

    /**
     * Ticks to play the gather/close animation after transferring.
     */
    private static final int CLOSE_ANIM_TICKS = 6;

    /**
     * The amount of items that can be transferred per second.
     */
    private int transferAmount = 1;

    /**
     * The timer used to determine when to transfer items.
     */
    private int transferTimer = 0;

    /**
     * Current deposit/take ceremony phase.
     */
    @Nonnull
    private CeremonyPhase ceremonyPhase = CeremonyPhase.NONE;

    /**
     * Ticks spent in the current ceremony phase.
     */
    private int ceremonyTimer = 0;

    /**
     * Whether the container lid/openers counter is currently open.
     */
    private boolean containerLidOpen = false;

    /**
     * Position of the container whose lid we opened (for safe close if target is cleared).
     */
    @Nullable
    private BlockPos openedContainerPos = null;

    /**
     * Whether tools were temporarily swapped for a deposit/take display item.
     */
    private boolean toolsSwapped = false;

    /**
     * Saved main-hand stack while displaying a transfer item.
     */
    @Nullable
    private ItemStack savedMainHand = null;

    /**
     * Saved off-hand stack while displaying a transfer item.
     */
    @Nullable
    private ItemStack savedOffHand = null;

    private enum CeremonyPhase
    {
        NONE,
        OPEN,
        TRANSFER,
        CLOSE
    }

    /**
     * @param id        the id associated with the goal's task.
     * @param blockling the blockling.
     * @param tasks     the blockling tasks.
     */
    public BlocklingContainerGoal(@Nonnull UUID id, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(id, blockling, tasks);

        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));

        itemInfoSet = new OrderedItemInfoSet(this);

        properties.add(itemConfigurationTypeProperty = new ItemConfigurationTypeProperty(
                "35d1e5a5-dfff-4a06-bb71-de1df8823632", this,
                BlocklingsTranslationTextComponent.of("task.property.item_configuration_type.name"),
                BlocklingsTranslationTextComponent.of("task.property.item_configuration_type.desc")));

        itemConfigurationTypeProperty.setEnabled(blockling.getSkills().getSkill(GeneralSkills.ADVANCED_COURIER).isBought());
        itemConfigurationTypeProperty.onTypeChanged.subscribe((this::recreateItemsConfigurationControl));
    }

    @Override
    public void writeToNBT(@Nonnull CompoundTag taskTag)
    {
        // Never persist mid-swap hand state — restore tools before equipment is saved with the entity.
        restoreDepositTools();
        closeContainerLid();

        super.writeToNBT(taskTag);

        ListTag containerInfosTag = new ListTag();

        for (int i = 0; i < containerInfos.size(); i++)
        {
            containerInfosTag.add(containerInfos.get(i).writeToNBT());
        }

        taskTag.put("container_infos", containerInfosTag);
        taskTag.put("item_set", itemInfoSet.writeToNBT());
    }

    @Override
    public void readFromNBT(@Nonnull CompoundTag taskTag, @Nonnull Version tagVersion)
    {
        super.readFromNBT(taskTag, tagVersion);

        containerInfos.clear();
        ListTag containerInfosTag = taskTag.getList("container_infos", 10);

        for (int i = 0; i < containerInfosTag.size(); i++)
        {
            ContainerInfo containerInfo = new ContainerInfo();
            containerInfo.readFromNBT(containerInfosTag.getCompound(i), tagVersion);

            if (containerInfo.getBlock() != Blocks.AIR)
            {
                containerInfos.add(containerInfo);
            }
        }

        CompoundTag itemSetTag = taskTag.getCompound("item_set");

        if (taskTag.contains("item_set"))
        {
            itemInfoSet.readFromNBT(itemSetTag, tagVersion);
        }
        else
        {
            Blocklings.LOGGER.warn("Could not find item set for deposit container goal!");
        }
    }

    @Override
    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        super.encode(buf);

        buf.writeInt(containerInfos.size());

        for (int i = 0; i < containerInfos.size(); i++)
        {
            containerInfos.get(i).encode(buf);
        }

        itemInfoSet.encode(buf);
    }

    @Override
    public void decode(@Nonnull FriendlyByteBuf buf)
    {
        super.decode(buf);

        containerInfos.clear();
        int size = buf.readInt();

        for (int i = 0; i < size; i++)
        {
            ContainerInfo containerInfo = new ContainerInfo();
            containerInfo.decode(buf);
            containerInfos.add(containerInfo);
        }

        itemInfoSet.decode(buf);
    }

    @Override
    public void stop()
    {
        cleanupCeremony();
        super.stop();
    }

    @Override
    protected void tickGoal()
    {
        if (!hasTarget())
        {
            cleanupCeremony();
            return;
        }

        ContainerInfo target = getTarget();
        BlockPos lookPos = target.getBlockPos();
        blockling.getLookControl().setLookAt(lookPos.getX() + 0.5, lookPos.getY() + 0.5, lookPos.getZ() + 0.5);

        if (!isInRangeOfPathTargetPos())
        {
            // Still walking — abort mid-ceremony if we somehow left range.
            if (ceremonyPhase != CeremonyPhase.NONE)
            {
                cleanupCeremony();
            }
            return;
        }

        switch (ceremonyPhase)
        {
            case NONE ->
            {
                // Never open the chest if nothing can actually be transferred (advanced thresholds, full chest, etc.).
                if (!tryTransferItems(target, true))
                {
                    cleanupCeremony();
                    markTargetBad();
                    return;
                }

                ceremonyPhase = CeremonyPhase.OPEN;
                ceremonyTimer = 0;
                openContainerLid(target);
                swapToolsForDeposit(findDisplayStack());
                // Drive arm swing in BlocklingModel (gather only animates when InteractionHand != NONE).
                blockling.getStats().InteractionHand.setValue(BlocklingHand.BOTH);
                startGatherAnimation();
            }
            case OPEN ->
            {
                ceremonyTimer++;
                tickGatherAnimation();
                if (ceremonyTimer >= OPEN_ANIM_TICKS)
                {
                    ceremonyPhase = CeremonyPhase.TRANSFER;
                    ceremonyTimer = 0;
                    // Start a full swing cycle, then deposit/take at the end of the cycle.
                    transferTimer = 0;
                }
            }
            case TRANSFER ->
            {
                transferTimer++;
                // One arm-swing cycle == one transfer pulse (same interval as items moved).
                tickGatherAnimation();

                if (transferTimer < TRANSFER_INTERVAL_TICKS)
                {
                    return;
                }

                transferTimer = 0;
                boolean transferred = tryTransferItems(target, false);

                // Finished with this container when nothing more can be transferred.
                if (!transferred || !tryTransferItems(target, true))
                {
                    ceremonyPhase = CeremonyPhase.CLOSE;
                    ceremonyTimer = 0;
                    restoreDepositTools();
                    startGatherAnimation();
                }
            }
            case CLOSE ->
            {
                if (ceremonyTimer == 0)
                {
                    closeContainerLid();
                }

                ceremonyTimer++;
                tickGatherAnimation();

                if (ceremonyTimer >= CLOSE_ANIM_TICKS)
                {
                    stopGatherAnimation();
                    ceremonyPhase = CeremonyPhase.NONE;
                    ceremonyTimer = 0;

                    if (!tryTransferItems(target, true))
                    {
                        markTargetBad();
                    }
                }
            }
        }
    }

    private void startGatherAnimation()
    {
        blockling.getStats().InteractionHand.setValue(BlocklingHand.BOTH);
        // Count 0..1 drives one arm swing in BlocklingModel (same period as a transfer).
        blockling.getActions().gather.setCount(0.0f);
    }

    private void stopGatherAnimation()
    {
        blockling.getActions().gather.stop();
        blockling.getStats().InteractionHand.setValue(BlocklingHand.NONE);
    }

    private void tickGatherAnimation()
    {
        if (blockling.getStats().InteractionHand.getValue() != BlocklingHand.BOTH)
        {
            blockling.getStats().InteractionHand.setValue(BlocklingHand.BOTH);
        }

        float progress;
        if (ceremonyPhase == CeremonyPhase.TRANSFER)
        {
            progress = transferTimer / (float) TRANSFER_INTERVAL_TICKS;
        }
        else if (ceremonyPhase == CeremonyPhase.OPEN)
        {
            progress = ceremonyTimer / (float) OPEN_ANIM_TICKS;
        }
        else if (ceremonyPhase == CeremonyPhase.CLOSE)
        {
            progress = ceremonyTimer / (float) CLOSE_ANIM_TICKS;
        }
        else
        {
            progress = 0.0f;
        }

        // Keep in [0, 1) so gather stays running; model uses this as swing percent.
        float count = Mth.clamp(progress, 0.0f, 0.999f);
        blockling.getActions().gather.setCount(count);
    }

    /**
     * @return a display stack for the item currently being transferred, or empty.
     */
    @Nonnull
    private ItemStack findDisplayStack()
    {
        for (ItemInfo itemInfo : itemInfoSet)
        {
            Item item = itemInfo.getItem();

            if (isTakeItems())
            {
                return new ItemStack(item);
            }

            if (hasItemInInventory(item))
            {
                return new ItemStack(item);
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * Temporarily puts the transfer item in the main hand (tools saved and restored safely).
     */
    private void swapToolsForDeposit(@Nonnull ItemStack displayStack)
    {
        if (toolsSwapped || world.isClientSide() || displayStack.isEmpty())
        {
            return;
        }

        // Deep-copy tools out of the equipment slots before replacing them.
        savedMainHand = blockling.getEquipment().getHandStack(InteractionHand.MAIN_HAND).copy();
        savedOffHand = blockling.getEquipment().getHandStack(InteractionHand.OFF_HAND).copy();
        toolsSwapped = true;

        // Display-only copy — never taken from inventory tool slots during deposit.
        blockling.getEquipment().setHandStack(InteractionHand.MAIN_HAND, displayStack.copyWithCount(1));
        blockling.getEquipment().setHandStack(InteractionHand.OFF_HAND, ItemStack.EMPTY);
    }

    /**
     * Restores tools saved during {@link #swapToolsForDeposit(ItemStack)}.
     */
    private void restoreDepositTools()
    {
        if (!toolsSwapped)
        {
            return;
        }

        if (world.isClientSide())
        {
            toolsSwapped = false;
            savedMainHand = null;
            savedOffHand = null;
            return;
        }

        try
        {
            blockling.getEquipment().setHandStack(
                    InteractionHand.MAIN_HAND,
                    savedMainHand != null ? savedMainHand : ItemStack.EMPTY);
            blockling.getEquipment().setHandStack(
                    InteractionHand.OFF_HAND,
                    savedOffHand != null ? savedOffHand : ItemStack.EMPTY);
        }
        finally
        {
            toolsSwapped = false;
            savedMainHand = null;
            savedOffHand = null;
        }
    }

    private void openContainerLid(@Nonnull ContainerInfo containerInfo)
    {
        if (containerLidOpen || world.isClientSide())
        {
            return;
        }

        BlockPos pos = containerInfo.getBlockPos();
        // Use block events + sound only — do NOT call startOpen(player).
        // startOpen ties the lid to the owner player and raced with GUI/config, and must not
        // mutate opener state while the player is configuring or has screens open.
        var state = world.getBlockState(pos);
        world.blockEvent(pos, state.getBlock(), 1, 1);
        world.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5f, world.random.nextFloat() * 0.1f + 0.9f);

        containerLidOpen = true;
        openedContainerPos = pos.immutable();
    }

    private void closeContainerLid()
    {
        if (!containerLidOpen || world.isClientSide())
        {
            containerLidOpen = false;
            openedContainerPos = null;
            return;
        }

        BlockPos pos = openedContainerPos != null ? openedContainerPos : (hasTarget() ? getTarget().getBlockPos() : null);

        if (pos != null)
        {
            var state = world.getBlockState(pos);
            world.blockEvent(pos, state.getBlock(), 1, 0);
            world.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5f, world.random.nextFloat() * 0.1f + 0.9f);
        }

        containerLidOpen = false;
        openedContainerPos = null;
    }

    private void cleanupCeremony()
    {
        closeContainerLid();
        restoreDepositTools();
        stopGatherAnimation();
        ceremonyPhase = CeremonyPhase.NONE;
        ceremonyTimer = 0;
        transferTimer = 0;
    }

    /**
     * Tries to transfer items to the target container.
     *
     * @param containerInfo the container info.
     * @param simulate whether to simulate the transfer.
     * @return returns true if an item was transferred.
     */
    protected abstract boolean tryTransferItems(@Nonnull ContainerInfo containerInfo, boolean simulate);

    /**
     * @return true when stop thresholds should be used (mid-transfer), false for start thresholds (deciding to visit).
     */
    protected boolean isEnforcingStopThresholds()
    {
        return ceremonyPhase == CeremonyPhase.TRANSFER || ceremonyPhase == CeremonyPhase.CLOSE;
    }

    @Override
    public boolean tryRecalcTarget()
    {
        // Do NOT auto-remove Blank/unconfigured entries here.
        // Right-click clears isConfiguring before ContainerGoalContainerMessage arrives,
        // so purging Blanks races and deletes the chest the player just selected.

        if (!hasItemsToTransfer())
        {
            setTarget(null);
            setPathTargetPos(null, null);

            return false;
        }

        for (ContainerInfo containerInfo : containerInfos)
        {
            if (!isInRange(containerInfo.getBlockPos(), SEARCH_RANGE_SQ))
            {
                continue;
            }

            if (isValidTarget(containerInfo))
            {
                setTarget(containerInfo);
                setPathTargetPos(null, null);

                return true;
            }
        }

        return false;
    }

    @Override
    protected boolean recalcPath(boolean force)
    {
        if (!hasTarget())
        {
            setPathTargetPos(null, null);
            return false;
        }

        BlockPos containerPos = getTarget().getBlockPos();

        // Already close enough to interact — no path required.
        if (BlockUtil.distanceSq(blockling.blockPosition(), containerPos) <= getRangeSq())
        {
            setPathTargetPos(containerPos, null);
            return true;
        }

        // Use stop distance 0 to get the best path; interact range is enforced by isInRangeOfPathTargetPos.
        Path pathToContainer = EntityUtil.createPathTo(blockling, containerPos, 0);

        if (pathToContainer == null)
        {
            setPathTargetPos(null, null);
            return false;
        }

        setPathTargetPos(containerPos, pathToContainer);
        return true;
    }

    @Override
    protected boolean isValidPathTargetPos(@Nonnull BlockPos blockPos)
    {
        return true;
    }

    @Override
    public float getRangeSq()
    {
        return INTERACT_RANGE_SQ;
    }

    @Override
    protected void checkForAndHandleInvalidTargets()
    {
        if (!isTargetValid())
        {
            markTargetBad();
        }
    }

    @Override
    public void markEntireTargetBad()
    {
        if (hasTarget())
        {
            markTargetBad();
        }
    }

    @Override
    public boolean isStuck()
    {
        if (hasPathTargetPos() && isInRangeOfPathTargetPos())
        {
            return false;
        }

        if (hasPath() && !getPath().isDone())
        {
            return false;
        }

        return super.isStuck();
    }

    @Override
    public boolean isValidTarget(@Nullable ContainerInfo containerInfo)
    {
        if (containerInfo == null)
        {
            return false;
        }

        if (!containerInfo.isConfigured())
        {
            return false;
        }

        // Manual "Chest" pick without world coords must not target (0,0,0).
        if (containerInfo.getBlockPos().equals(BlockPos.ZERO))
        {
            return false;
        }

        if (badTargets.contains(containerInfo))
        {
            return false;
        }

        if (!BlockUtil.isContainer(world, containerInfo.getBlockPos()))
        {
            return false;
        }

        if (!tryTransferItems(containerInfo, true))
        {
            return false;
        }

        return true;
    }

    /**
     * @return whether there are items available to transfer.
     */
    protected abstract boolean hasItemsToTransfer();

    /**
     * Counts the number of items in the blockling's inventory.
     *
     * @param item the item to count.
     * @return the number of items in the blockling's inventory.
     */
    public int countItemsInInventory(@Nonnull Item item)
    {
        // Ignore tool slots so a temporary display stack in-hand is never counted/deposited.
        AbstractInventory inv = blockling.getEquipment();
        return inv.count(new ItemStack(item), EquipmentInventory.TOOL_OFF_HAND + 1, inv.getContainerSize() - 1);
    }

    /**
     * @return true if the blockling has the given item in their inventory.
     */
    public boolean hasItemInInventory(@Nonnull Item item)
    {
        return countItemsInInventory(item) > 0;
    }

    /**
     * @param containerItemHandler the container to check.
     * @param item the item to check for.
     * @return true if the container has the given item.
     */
    public boolean hasItemInContainer(@Nonnull BlocklingItemHandler containerItemHandler, @Nonnull Item item)
    {
        for (int slot = 0; slot < containerItemHandler.getSlots(); slot++)
        {
            ItemStack stack = containerItemHandler.getStackInSlot(slot);

            if (stack.getItem() == item)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Counts the number of items in the container.
     *
     * @param containerItemHandler the container to count the items in.
     * @param item the item to count.
     * @return the number of items in the container.
     */
    public int countItemsInContainer(@Nonnull BlocklingItemHandler containerItemHandler, @Nonnull Item item)
    {
        int count = 0;

        for (int slot = 0; slot < containerItemHandler.getSlots(); slot++)
        {
            ItemStack stack = containerItemHandler.getStackInSlot(slot);

            if (stack.getItem() == item)
            {
                count += stack.getCount();
            }
        }

        return count;
    }

    /**
     * @return returns the target tile entity.
     */
    @Nullable
    public BlockEntity targetAsBlockEntity()
    {
        if (getTarget() == null)
        {
            return null;
        }

        return containerAsBlockEntity(getTarget());
    }

    @Nullable
    public BlockEntity containerAsBlockEntity(@Nonnull ContainerInfo containerInfo)
    {
        return world.getBlockEntity(containerInfo.getBlockPos());
    }

    @Nullable
    public BlocklingItemHandler getItemHandler(@Nonnull BlockEntity blockEntity, @Nonnull Direction direction)
    {
        BlocklingItemHandler handler = Services.INVENTORY.getItemHandler(world, blockEntity.getBlockPos(), blockEntity, direction);
        if (handler != null)
        {
            return handler;
        }

        if (blockEntity instanceof net.minecraft.world.Container container)
        {
            return new com.willr27.blocklings.inventory.ContainerItemHandlerAdapter(container, direction);
        }

        return null;
    }

    /**
     * @return whether the container list is full.
     */
    public boolean isContainerListFull()
    {
        return containerInfos.size() >= MAX_CONTAINERS;
    }

    /**
     * Adds the given container info to the list and syncs it to the client/server.
     *
     * @param containerInfo the container info.
     */
    public void addContainerInfo(@Nonnull ContainerInfo containerInfo)
    {
        addContainerInfo(containerInfo, false, true);
    }

    /**
     * Adds the given container info to the list and syncs it to the client/server.
     *
     * @param containerInfo the container info.
     * @param configureInWorld whether to configure the container in the world.
     */
    public void addContainerInfo(@Nonnull ContainerInfo containerInfo, boolean configureInWorld)
    {
        addContainerInfo(containerInfo, configureInWorld, true);
    }

    /**
     * Adds the given container info to the list.
     *
     * @param containerInfo the container info.
     * @param configureInWorld whether to configure the container in the world.
     * @param sync whether to sync the container info to the client.
     */
    public void addContainerInfo(@Nonnull ContainerInfo containerInfo, boolean configureInWorld, boolean sync)
    {
        containerInfos.add(containerInfo);

        if (configureInWorld && blockling.getOwner() instanceof Player player)
        {
            ContainerConfigureCapability.get(player).isConfiguring = true;
        }

        if (sync)
        {
            new ContainerGoalContainerAddRemoveMessage(blockling, id, containerInfos.size() - 1, true, configureInWorld).sync();
            new ContainerGoalContainerMessage(blockling, id, containerInfo, containerInfos.size() - 1).sync();
        }
    }

    /**
     * Removes the container info at the given index and syncs it to the client/server.
     *
     * @param index the index.
     */
    public void removeContainerInfo(int index)
    {
        removeContainerInfo(index, true);
    }

    /**
     * Removes the container info at the given index.
     *
     * @param index the index.
     * @param sync whether to sync the container info to the client.
     */
    public void removeContainerInfo(int index, boolean sync)
    {
        if (index < 0 || index >= containerInfos.size())
        {
            return;
        }

        containerInfos.remove(index);

        if (sync)
        {
            new ContainerGoalContainerAddRemoveMessage(blockling, id, index, false, false).sync();
        }
    }

    /**
     * Sets the container info at the given index and syncs it to the client/server.
     *
     * @param index the index.
     * @param containerInfo the container info.
     */
    public void setContainerInfo(int index, @Nonnull ContainerInfo containerInfo)
    {
        setContainerInfo(index, containerInfo, true);
    }

    /**
     * Sets the container info at the given index.
     *
     * @param index the index.
     * @param containerInfo the container info.
     * @param sync whether to sync the container info to the client.
     */
    public void setContainerInfo(int index, @Nonnull ContainerInfo containerInfo, boolean sync)
    {
        if (index < 0)
        {
            return;
        }

        if (index < containerInfos.size())
        {
            containerInfos.set(index, containerInfo);
        }
        else if (index == containerInfos.size())
        {
            // Message arrived after a desynced remove — re-append instead of crashing.
            containerInfos.add(containerInfo);
        }
        else
        {
            return;
        }

        if (sync)
        {
            new ContainerGoalContainerMessage(blockling, id, containerInfo, index).sync();
        }
    }

    /**
     * Moves the container info at the given index to the given index and syncs it to the client/server.
     *
     * @param fromIndex the index to move from.
     * @param toIndex the index to move to.
     */
    public void moveContainerInfo(int fromIndex, int toIndex)
    {
        moveContainerInfo(fromIndex, toIndex, true);
    }

    /**
     * Moves the container info at the given index to the given index.
     *
     * @param fromIndex the index to move from.
     * @param toIndex the index to move to.
     * @param sync whether to sync the container info to the client.
     */
    public void moveContainerInfo(int fromIndex, int toIndex, boolean sync)
    {
        ContainerInfo containerInfo = containerInfos.get(fromIndex);
        containerInfos.remove(fromIndex);
        containerInfos.add(toIndex - (fromIndex < toIndex ? 1 : 0), containerInfo);

        if (sync)
        {
            new ContainerGoalContainerMoveMessage(blockling, id, fromIndex, toIndex).sync();
        }
    }

    @Nonnull
    @Override
    public OrderedItemInfoSet getItemSet()
    {
        return itemInfoSet;
    }

    /**
     * @return the number of items to transfer every second.
     */
    public int getTransferAmount()
    {
        return transferAmount;
    }

    /**
     * @param transferAmount the number of items to transfer every second.
     */
    public void setTransferAmount(int transferAmount)
    {
        this.transferAmount = transferAmount;
    }

    /**
     * @return whether the goal should take items from the container or deposit them.
     */
    public abstract boolean isTakeItems();

    @Nonnull
    @Override
    @OnlyIn(Dist.CLIENT)
    public void addConfigTabControls(@Nonnull TabbedPanel tabbedPanel)
    {
        super.addConfigTabControls(tabbedPanel);

        itemsContainer = tabbedPanel.addTab(BlocklingsTranslationTextComponent.of("config.items"));
        itemsContainer.setCanScrollVertically(true);

        recreateItemsConfigurationControl(itemConfigurationTypeProperty.getType());

        BaseControl containersContainer = tabbedPanel.addTab(BlocklingsTranslationTextComponent.of("config.containers"));
        containersContainer.setCanScrollVertically(true);

        // Unfinished Blank rows are kept until the player removes them or finishes world-pick.
        // Auto-purging here previously wiped chests that had just been selected (client/server race).

        StackPanel stackPanel = new StackPanel();
        stackPanel.setParent(containersContainer);
        stackPanel.setWidthPercentage(1.0);
        stackPanel.setFitHeightToContent(true);
        stackPanel.setMargins(5.0, 9.0, 5.0, 5.0);
        stackPanel.setSpacing(4.0);
        stackPanel.setClipContentsToBounds(false);
        stackPanel.eventBus.subscribe((BaseControl c, ReorderEvent e) ->
        {
            int movedIndex = stackPanel.getChildren().indexOf(e.draggedControl);
            int closestIndex = stackPanel.getChildren().indexOf(e.closestControl);

            moveContainerInfo(movedIndex, closestIndex + (e.insertBefore ? 0 : 1));
        });

        Control addContainerContainer = new Control();
        addContainerContainer.setParent(stackPanel);
        addContainerContainer.setWidthPercentage(1.0);
        addContainerContainer.setFitHeightToContent(true);
        addContainerContainer.setReorderable(false);

        Function<ContainerInfo, ContainerControl> addContainerControl = (ContainerInfo containerInfo) ->
        {
            ContainerControl containerControl = new ContainerControl(containerInfo);
            stackPanel.insertChildBefore(containerControl, addContainerContainer);
            containerControl.setWidthPercentage(1.0);
            containerControl.setDraggableY(true);
            containerControl.setScrollFromDragControl(containersContainer);
            containerControl.eventBus.subscribe((BaseControl c, ValueChangedEvent<ContainerInfo> e2) ->
            {
                ContainerInfo info = ((ContainerControl) c).containerInfo;
                int index = containerInfos.indexOf(info);
                if (index >= 0)
                {
                    setContainerInfo(index, info);
                }
            });
            containerControl.eventBus.subscribe((BaseControl c, ParentChangedEvent e2) ->
            {
                if (e2.newParent == null)
                {
                    int index = containerInfos.indexOf(((ContainerControl) c).containerInfo);
                    if (index >= 0)
                    {
                        removeContainerInfo(index);
                    }
                }
            });

            return containerControl;
        };

        for (ContainerInfo containerInfo : containerInfos)
        {
            addContainerControl.apply(containerInfo);
        }

        TexturedControl addContainerButton = new TexturedControl(Textures.Common.PLUS_ICON)
        {
            @Override
            protected void onRender(@Nonnull GuiGraphics matrixStack, @Nonnull ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
            {
                if (isContainerListFull())
                {
                    renderTextureAsBackground(matrixStack, Textures.Common.PLUS_ICON_DISABLED);
                }
                else
                {
                    super.onRender(matrixStack, scissorStack, mouseX, mouseY, partialTicks);
                }
            }

            @Override
            public void onRenderTooltip(@Nonnull GuiGraphics matrixStack, double mouseX, double mouseY, float partialTicks)
            {
                List<FormattedCharSequence> tooltip = new ArrayList<>();
                tooltip.add(BlocklingsTranslationTextComponent.create("config.container.add")
                        .withStyle(isContainerListFull() ? ChatFormatting.GRAY : ChatFormatting.WHITE)
                        .getVisualOrderText());
                tooltip.add(BlocklingsTranslationTextComponent.create("config.container.amount", containerInfos.size(), MAX_CONTAINERS)
                        .withStyle(ChatFormatting.GRAY)
                        .getVisualOrderText());
                tooltip.add(Component.empty().getVisualOrderText());
                tooltip.addAll(GuiUtil.get().split(
                        BlocklingsTranslationTextComponent.create(
                                "config.container.add.help",
                                Component.literal(Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage().getString())
                                        .withStyle(ChatFormatting.ITALIC))
                                .withStyle(ChatFormatting.GRAY),
                        200));
                renderTooltip(matrixStack, mouseX, mouseY, tooltip);
            }

            @Override
            protected void onMouseReleased(@Nonnull MouseReleasedEvent e)
            {
                if (isPressed() && !isContainerListFull())
                {
                    ContainerInfo containerInfo = new ContainerInfo(
                            BlockPos.ZERO,
                            Blocks.AIR,
                            Arrays.asList(Direction.UP, Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH, Direction.DOWN));
                    addContainerInfo(containerInfo, !GuiUtil.get().isCrouchKeyDown());

                    ContainerControl containerControl = addContainerControl.apply(containerInfo);

                    if (GuiUtil.get().isCrouchKeyDown())
                    {
                        containerControl.onFirstAdded();
                    }
                    else
                    {
                        ContainerControl.currentlyConfiguredContainerControl = containerControl;
                        ContainerControl.screenToGoBackTo = Minecraft.getInstance().screen;
                        getScreen().setShouldReallyClose(false);
                        ContainerControl.screenToGoBackTo.onClose();
                        getScreen().setShouldReallyClose(true);
                    }

                    e.setIsHandled(true);
                }
            }
        };
        addContainerButton.setParent(addContainerContainer);
        addContainerButton.setHorizontalAlignment(0.5);
        addContainerButton.setMargins(0.0, 1.0, 0.0, 1.0);
    }

    /**
     * Recreates the items configuration control.
     */
    @OnlyIn(Dist.CLIENT)
    private void recreateItemsConfigurationControl(@Nonnull ItemConfigurationTypeProperty.Type type)
    {
        if (itemsContainer == null)
        {
            return;
        }

        itemsContainer.clearChildren();

        ItemsConfigurationControl itemsConfigurationControl = type.createItemsConfigurationControl(itemInfoSet, isTakeItems());
        itemsConfigurationControl.setParent(itemsContainer);
        itemsConfigurationControl.setMargins(5.0, 9.0, 5.0, 5.0);
        itemsConfigurationControl.setMaxItems(MAX_ITEMS);
        itemsConfigurationControl.setScrollFromDragControl(itemsContainer);
    }

    /**
     * A message used to sync adding and removing container info between a goal on the client/server.
     */
    public static class ContainerGoalContainerAddRemoveMessage extends GoalMessage<ContainerGoalContainerAddRemoveMessage, BlocklingContainerGoal>
    {
        /**
         * Whether to add or remove the container info.
         */
        private boolean add;

        /**
         * The index of the container.
         */
        private int index;

        /**
         * Whether to configure the container in world.
         */
        private boolean configureInWorld;

        /**
         * Empty constructor used ONLY for decoding.
         */
        public ContainerGoalContainerAddRemoveMessage()
        {
            super();
        }

        /**
         * @param blockling the blockling.
         * @param taskId the id of the goal.
         * @param index the index of the container.
         * @param add whether to add or remove the container info.
         * @param configureInWorld whether to configure the container in world.
         */
        public ContainerGoalContainerAddRemoveMessage(@Nonnull BlocklingEntity blockling, @Nonnull UUID taskId, int index, boolean add, boolean configureInWorld)
        {
            super(blockling, taskId);
            this.index = index;
            this.add = add;
            this.configureInWorld = configureInWorld;
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf)
        {
            super.encode(buf);

            buf.writeBoolean(add);
            buf.writeInt(index);
            buf.writeBoolean(configureInWorld);
        }

        @Override
        public void decode(@Nonnull FriendlyByteBuf buf)
        {
            super.decode(buf);

            add = buf.readBoolean();
            index = buf.readInt();
            configureInWorld = buf.readBoolean();
        }

        @Override
        protected void handle(@Nonnull Player player, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingContainerGoal goal)
        {
            if (add)
            {
                goal.addContainerInfo(new ContainerInfo(), configureInWorld, false);
            }
            else if (index >= 0 && index < goal.containerInfos.size())
            {
                goal.removeContainerInfo(index, false);
            }
        }
    }

    /**
 * A message used to sync container info between a goal on the client/server.
 */
    public static class ContainerGoalContainerMessage extends GoalMessage<ContainerGoalContainerMessage, BlocklingContainerGoal>
    {
        /**
         * The container info.
         */
        private ContainerInfo containerInfo;

        /**
         * The index of the container.
         */
        private int index;

        /**
         * Empty constructor used ONLY for decoding.
         */
        public ContainerGoalContainerMessage()
        {
            super();
        }

        /**
         * @param blockling the blockling.
         * @param taskId the id of the goal.
         * @param containerInfo the container info.
         * @param index the index of the container.
         */
        public ContainerGoalContainerMessage(@Nonnull BlocklingEntity blockling, @Nonnull UUID taskId, @Nonnull ContainerInfo containerInfo, int index)
        {
            super(blockling, taskId);
            // Snapshot so a later mutation of the live GUI instance cannot change the packet.
            this.containerInfo = new ContainerInfo(containerInfo);
            this.index = index;
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf)
        {
            super.encode(buf);

            containerInfo.encode(buf);
            buf.writeInt(index);
        }

        @Override
        public void decode(@Nonnull FriendlyByteBuf buf)
        {
            super.decode(buf);

            containerInfo = new ContainerInfo();
            containerInfo.decode(buf);
            index = buf.readInt();
        }

        @Override
        protected void handle(@Nonnull Player player, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingContainerGoal goal)
        {
            goal.setContainerInfo(index, containerInfo, false);
        }
    }
    /**
     * A message used to sync the priority of the container info between a goal on the client/server.
     */
    public static class ContainerGoalContainerMoveMessage extends GoalMessage<ContainerGoalContainerMoveMessage, BlocklingContainerGoal>
    {
        /**
         * The index of the container to move.
         */
        private int fromIndex;

        /**
         * The index of the container to move to.
         */
        private int toIndex;

        /**
         * Empty constructor used ONLY for decoding.
         */
        public ContainerGoalContainerMoveMessage()
        {
            super();
        }

        /**
         * @param blockling the blockling.
         * @param taskId the id of the goal.
         * @param fromIndex the index of the container to move.
         * @param toIndex the index of the container to move to.
         */
        public ContainerGoalContainerMoveMessage(@Nonnull BlocklingEntity blockling, @Nonnull UUID taskId, int fromIndex, int toIndex)
        {
            super(blockling, taskId);
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf)
        {
            super.encode(buf);

            buf.writeInt(fromIndex);
            buf.writeInt(toIndex);
        }

        @Override
        public void decode(@Nonnull FriendlyByteBuf buf)
        {
            super.decode(buf);

            fromIndex = buf.readInt();
            toIndex = buf.readInt();
        }

        @Override
        protected void handle(@Nonnull Player player, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingContainerGoal goal)
        {
            goal.moveContainerInfo(fromIndex, toIndex, false);
        }
    }
}
