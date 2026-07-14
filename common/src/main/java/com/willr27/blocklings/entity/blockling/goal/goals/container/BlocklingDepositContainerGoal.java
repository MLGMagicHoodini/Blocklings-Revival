package com.willr27.blocklings.entity.blockling.goal.goals.container;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.goal.config.iteminfo.ItemInfo;
import com.willr27.blocklings.entity.blockling.task.BlocklingTasks;
import com.willr27.blocklings.entity.blockling.task.config.ItemConfigurationTypeProperty;
import com.willr27.blocklings.inventory.AbstractInventory;
import com.willr27.blocklings.inventory.BlocklingItemHandler;
import com.willr27.blocklings.inventory.EquipmentInventory;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Finds nearby containers and deposits items into them.
 */
public class BlocklingDepositContainerGoal extends BlocklingContainerGoal
{
    /**
     * @param taskId    the taskId associated with the goal's task.
     * @param blockling the blockling.
     * @param tasks     the blockling tasks.
     */
    public BlocklingDepositContainerGoal(@Nonnull UUID taskId, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(taskId, blockling, tasks);
    }

    @Override
    protected boolean tryTransferItems(@Nonnull ContainerInfo containerInfo, boolean simulate)
    {
        BlockEntity blockEntity = containerAsBlockEntity(containerInfo);

        if (blockEntity == null)
        {
            return false;
        }

        AbstractInventory inv = blockling.getEquipment();
        int remainingDepositAmount = getTransferAmount();
        // While transferring (or finishing), enforce stop thresholds; otherwise start thresholds.
        boolean useStopThresholds = isEnforcingStopThresholds();

        for (ItemInfo itemInfo : itemInfoSet)
        {
            if (remainingDepositAmount <= 0)
            {
                break;
            }

            Item item = itemInfo.getItem();

            if (!hasItemInInventory(item))
            {
                continue;
            }

            for (Direction direction : containerInfo.getSides())
            {
                BlocklingItemHandler itemHandler = getItemHandler(blockEntity, direction);

                if (itemHandler == null)
                {
                    continue;
                }

                ItemStack remainingStack = new ItemStack(item, remainingDepositAmount);
                int amountOfSpaceInContainerForItem = 0;

                for (int i = 0; i < itemHandler.getSlots() && !remainingStack.isEmpty(); i++)
                {
                    ItemStack remainderStack = itemHandler.insertItem(i, remainingStack, true);
                    amountOfSpaceInContainerForItem += remainingStack.getCount() - remainderStack.getCount();
                    remainingStack = remainderStack;
                }

                if (amountOfSpaceInContainerForItem == 0)
                {
                    continue;
                }

                int amountAllowed = remainingDepositAmount;

                if (itemConfigurationTypeProperty.getType() == ItemConfigurationTypeProperty.Type.ADVANCED)
                {
                    amountAllowed = getAdvancedDepositAmount(itemInfo, itemHandler, item, useStopThresholds);

                    if (amountAllowed <= 0)
                    {
                        continue;
                    }
                }

                amountAllowed = Math.min(amountAllowed, amountOfSpaceInContainerForItem);

                if (amountAllowed <= 0)
                {
                    continue;
                }

                ItemStack stackLeftToDeposit = new ItemStack(item, amountAllowed);

                ItemStack stackDeposited = inv.takeItem(
                        stackLeftToDeposit,
                        EquipmentInventory.TOOL_OFF_HAND + 1,
                        inv.getContainerSize() - 1,
                        simulate);

                int amountTaken = stackDeposited.getCount();

                if (amountTaken == 0)
                {
                    continue;
                }

                if (!simulate)
                {
                    ItemStack toInsert = stackDeposited.copy();
                    for (int i = 0; i < itemHandler.getSlots() && !toInsert.isEmpty(); i++)
                    {
                        toInsert = itemHandler.insertItem(i, toInsert, false);
                    }

                    int inserted = amountTaken - toInsert.getCount();

                    if (!toInsert.isEmpty())
                    {
                        ItemStack leftover = inv.addItem(toInsert);
                        if (!leftover.isEmpty())
                        {
                            blockling.dropItemStack(leftover);
                        }
                    }

                    remainingDepositAmount -= inserted;
                }
                else
                {
                    return true;
                }
            }
        }

        return remainingDepositAmount < getTransferAmount();
    }

    /**
     * @return how many of this item may be deposited under advanced start/stop rules, or 0 if none.
     */
    private int getAdvancedDepositAmount(@Nonnull ItemInfo itemInfo, @Nonnull BlocklingItemHandler itemHandler, @Nonnull Item item, boolean useStopThresholds)
    {
        int inventoryAmount = countItemsInInventory(item);
        int containerAmount = countItemsInContainer(itemHandler, item);

        Integer startInv = itemInfo.getStartInventoryAmount();
        Integer stopInv = itemInfo.getStopInventoryAmount();
        Integer startCont = itemInfo.getStartContainerAmount();
        Integer stopCont = itemInfo.getStopContainerAmount();

        if (useStopThresholds)
        {
            // Stop when inventory is at/below keep-amount, or container reached fill limit.
            if (stopInv != null && inventoryAmount <= stopInv)
            {
                return 0;
            }
            if (stopCont != null && containerAmount >= stopCont)
            {
                return 0;
            }

            int byInv = stopInv != null ? inventoryAmount - stopInv : inventoryAmount;
            int byCont = stopCont != null ? stopCont - containerAmount : Integer.MAX_VALUE;
            return Math.max(0, Math.min(byInv, byCont));
        }

        // Start only when inventory is above start threshold and container is below start fill.
        if (startInv != null && inventoryAmount <= startInv)
        {
            return 0;
        }
        if (startCont != null && containerAmount >= startCont)
        {
            return 0;
        }

        // First transfer of a trip: also respect stop so we never open just to no-op.
        if (stopInv != null && inventoryAmount <= stopInv)
        {
            return 0;
        }
        if (stopCont != null && containerAmount >= stopCont)
        {
            return 0;
        }

        int byInv = stopInv != null ? inventoryAmount - stopInv : inventoryAmount;
        int byCont = stopCont != null ? stopCont - containerAmount : Integer.MAX_VALUE;
        return Math.max(0, Math.min(byInv, byCont));
    }

    @Override
    public boolean hasItemsToTransfer()
    {
        for (ItemInfo itemInfo : itemInfoSet)
        {
            Item item = itemInfo.getItem();
            int inventoryAmount = countItemsInInventory(item);

            if (inventoryAmount <= 0)
            {
                continue;
            }

            if (itemConfigurationTypeProperty.getType() == ItemConfigurationTypeProperty.Type.ADVANCED)
            {
                Integer startInv = itemInfo.getStartInventoryAmount();
                Integer stopInv = itemInfo.getStopInventoryAmount();

                // Need something above the keep-amount, and enough to trigger a new trip.
                if (stopInv != null && inventoryAmount <= stopInv)
                {
                    continue;
                }
                if (startInv != null && inventoryAmount <= startInv)
                {
                    continue;
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean isTakeItems()
    {
        return false;
    }
}
