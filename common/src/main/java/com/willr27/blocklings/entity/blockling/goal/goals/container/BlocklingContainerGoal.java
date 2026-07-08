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
import com.willr27.blocklings.entity.blockling.goal.BlocklingTargetGoal;
import com.willr27.blocklings.entity.blockling.goal.config.iteminfo.OrderedItemInfoSet;
import com.willr27.blocklings.entity.blockling.skill.skills.GeneralSkills;
import com.willr27.blocklings.entity.blockling.task.BlocklingTasks;
import com.willr27.blocklings.entity.blockling.task.config.ItemConfigurationTypeProperty;
import com.willr27.blocklings.network.messages.GoalMessage;
import com.willr27.blocklings.util.BlocklingsTranslationTextComponent;
import com.willr27.blocklings.util.Version;
import com.willr27.blocklings.util.event.ValueChangedEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;
import com.willr27.blocklings.inventory.BlocklingItemHandler;
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
     * The amount of items that can be transferred per second.
     */
    private int transferAmount = 1;

    /**
     * The timer used to determine when to transfer items.
     */
    private int transferTimer = 0;

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
    protected void tickGoal()
    {
        if (transferTimer < 20)
        {
            transferTimer++;

            return;
        }

        if (isInRangeOfPathTargetPos())
        {
            boolean depositedAnItem = tryTransferItems(getTarget(), false);

            // If no items were deposited then try other targets before this one again.
            if (!depositedAnItem)
            {
                markTargetBad();
            }
        }

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

    @Override
    public boolean tryRecalcTarget()
    {
        if (!hasItemsToTransfer())
        {
            setTarget(null);
            setPathTargetPos(null, null);

            return false;
        }

//        for (BlockPos testPos : BlockPos.betweenClosed(blockling.blockPosition().offset(-range, -range, -range), blockling.blockPosition().offset(range, range, range)))
//        {
//            TileEntity tileEntity = world.getBlockEntity(testPos);
//
//            if (isValidTarget(tileEntity))
//            {
//                setTarget(tileEntity);
//                setPathTargetPos(null, null);
//
//                return true;
//            }
//        }

        for (ContainerInfo containerInfo : containerInfos)
        {
            if (!isInRange(containerInfo.getBlockPos(), getRangeSq()))
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
        setPathTargetPos(getTarget().getBlockPos(), path);

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
        return 256.0f;
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

        if (badTargets.contains(containerInfo))
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
        return blockling.getEquipment().count(new ItemStack(item));
    }

    /**
     * @return true if the blockling has the given item in their inventory.
     */
    public boolean hasItemInInventory(@Nonnull Item item)
    {
        return blockling.getEquipment().has(new ItemStack(item));
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
        return Services.INVENTORY.getItemHandler(world, blockEntity.getBlockPos(), blockEntity, direction);
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
        addContainerInfo(containerInfo, true);
    }

    /**
     * Adds the given container info to the list.
     *
     * @param containerInfo the container info.
     * @param sync whether to sync the container info to the client.
     */
    public void addContainerInfo(@Nonnull ContainerInfo containerInfo, boolean sync)
    {
        containerInfos.add(containerInfo);

            Player player = (Player) blockling.getOwner();
            ContainerConfigureCapability.get(player).isConfiguring = true;

        if (sync)
        {
            new ContainerGoalContainerAddRemoveMessage(blockling, id, containerInfos.size() - 1, true).sync();
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
        containerInfos.remove(index);

        if (sync)
        {
            new ContainerGoalContainerAddRemoveMessage(blockling, id, index, false).sync();
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
        containerInfos.set(index, containerInfo);

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
        tabbedPanel.addTab(BlocklingsTranslationTextComponent.of("config.containers"));
    }

    private void recreateItemsConfigurationControl(@Nonnull ItemConfigurationTypeProperty.Type type)
    {
        // GUI port pending.
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
         */
        public ContainerGoalContainerAddRemoveMessage(@Nonnull BlocklingEntity blockling, @Nonnull UUID taskId, int index, boolean add)
        {
            super(blockling, taskId);
            this.index = index;
            this.add = add;
        }

        @Override
        public void encode(@Nonnull FriendlyByteBuf buf)
        {
            super.encode(buf);

            buf.writeBoolean(add);
            buf.writeInt(index);
        }

        @Override
        public void decode(@Nonnull FriendlyByteBuf buf)
        {
            super.decode(buf);

            add = buf.readBoolean();
            index = buf.readInt();
        }

        @Override
        protected void handle(@Nonnull Player player, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingContainerGoal goal)
        {
            if (add)
            {
                goal.addContainerInfo(new ContainerInfo(), false);
            }
            else
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
            this.containerInfo = containerInfo;
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
