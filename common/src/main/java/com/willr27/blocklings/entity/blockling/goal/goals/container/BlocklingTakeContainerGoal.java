package com.willr27.blocklings.entity.blockling.goal.goals.container;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.goal.config.iteminfo.ItemInfo;
import com.willr27.blocklings.entity.blockling.task.BlocklingTasks;
import com.willr27.blocklings.entity.blockling.task.config.ItemConfigurationTypeProperty;
import com.willr27.blocklings.inventory.AbstractInventory;
import com.willr27.blocklings.inventory.BlocklingItemHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Finds nearby containers and takes items from them.
 */
public class BlocklingTakeContainerGoal extends BlocklingContainerGoal
{
    /**
     * @param taskId    the taskId associated with the goal's task.
     * @param blockling the blockling.
     * @param tasks     the blockling tasks.
     */
    public BlocklingTakeContainerGoal(@Nonnull UUID taskId, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
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
        int remainingTakeAmount = getTransferAmount();
        boolean useStopThresholds = isEnforcingStopThresholds();

        for (ItemInfo itemInfo : itemInfoSet)
        {
            if (remainingTakeAmount <= 0)
            {
                break;
            }

            Item item = itemInfo.getItem();

            ItemStack remainingStack = new ItemStack(item, remainingTakeAmount);
            int amountOfSpaceInInventoryForItem = remainingStack.getCount() - inv.addItem(remainingStack, true).getCount();

            if (amountOfSpaceInInventoryForItem == 0)
            {
                continue;
            }

            for (Direction direction : containerInfo.getSides())
            {
                if (amountOfSpaceInInventoryForItem == 0)
                {
                    continue;
                }

                BlocklingItemHandler itemHandler = getItemHandler(blockEntity, direction);

                if (itemHandler == null)
                {
                    continue;
                }

                if (!hasItemInContainer(itemHandler, item))
                {
                    continue;
                }

                int amountAllowed = remainingTakeAmount;

                if (itemConfigurationTypeProperty.getType() == ItemConfigurationTypeProperty.Type.ADVANCED)
                {
                    amountAllowed = getAdvancedTakeAmount(itemInfo, itemHandler, item, useStopThresholds);

                    if (amountAllowed <= 0)
                    {
                        continue;
                    }
                }

                amountAllowed = Math.min(amountAllowed, amountOfSpaceInInventoryForItem);

                if (amountAllowed <= 0)
                {
                    continue;
                }

                ItemStack stackLeftToTake = new ItemStack(item, amountAllowed);

                for (int slot = itemHandler.getSlots() - 1; slot >= 0 && !stackLeftToTake.isEmpty(); slot--)
                {
                    stackLeftToTake.shrink(itemHandler.extractItem(slot, stackLeftToTake.getCount(), simulate).getCount());
                }

                int amountTaken = amountAllowed - stackLeftToTake.getCount();

                if (amountTaken == 0)
                {
                    continue;
                }

                if (!simulate)
                {
                    ItemStack taken = new ItemStack(item, amountTaken);
                    ItemStack leftover = inv.addItem(taken);
                    if (!leftover.isEmpty())
                    {
                        for (int slot = 0; slot < itemHandler.getSlots() && !leftover.isEmpty(); slot++)
                        {
                            leftover = itemHandler.insertItem(slot, leftover, false);
                        }
                        if (!leftover.isEmpty())
                        {
                            blockling.dropItemStack(leftover);
                        }
                    }
                }
                else
                {
                    return true;
                }

                remainingTakeAmount -= amountTaken;
                amountOfSpaceInInventoryForItem -= amountTaken;
            }
        }

        return remainingTakeAmount < getTransferAmount();
    }

    /**
     * @return how many of this item may be taken under advanced start/stop rules, or 0 if none.
     */
    private int getAdvancedTakeAmount(@Nonnull ItemInfo itemInfo, @Nonnull BlocklingItemHandler itemHandler, @Nonnull Item item, boolean useStopThresholds)
    {
        int inventoryAmount = countItemsInInventory(item);
        int containerAmount = countItemsInContainer(itemHandler, item);

        Integer startInv = itemInfo.getStartInventoryAmount();
        Integer stopInv = itemInfo.getStopInventoryAmount();
        Integer startCont = itemInfo.getStartContainerAmount();
        Integer stopCont = itemInfo.getStopContainerAmount();

        if (useStopThresholds)
        {
            // Stop when inventory reached fill amount, or container drained to keep-amount.
            if (stopInv != null && inventoryAmount >= stopInv)
            {
                return 0;
            }
            if (stopCont != null && containerAmount <= stopCont)
            {
                return 0;
            }

            int byInv = stopInv != null ? stopInv - inventoryAmount : Integer.MAX_VALUE;
            int byCont = stopCont != null ? containerAmount - stopCont : containerAmount;
            return Math.max(0, Math.min(byInv, byCont));
        }

        if (startInv != null && inventoryAmount >= startInv)
        {
            return 0;
        }
        if (startCont != null && containerAmount <= startCont)
        {
            return 0;
        }

        if (stopInv != null && inventoryAmount >= stopInv)
        {
            return 0;
        }
        if (stopCont != null && containerAmount <= stopCont)
        {
            return 0;
        }

        int byInv = stopInv != null ? stopInv - inventoryAmount : Integer.MAX_VALUE;
        int byCont = stopCont != null ? containerAmount - stopCont : containerAmount;
        return Math.max(0, Math.min(byInv, byCont));
    }

    @Override
    public boolean hasItemsToTransfer()
    {
        for (ItemInfo itemInfo : itemInfoSet)
        {
            Item item = itemInfo.getItem();

            if (itemConfigurationTypeProperty.getType() == ItemConfigurationTypeProperty.Type.ADVANCED)
            {
                Integer startInv = itemInfo.getStartInventoryAmount();
                Integer stopInv = itemInfo.getStopInventoryAmount();
                int inventoryAmount = countItemsInInventory(item);

                if (stopInv != null && inventoryAmount >= stopInv)
                {
                    continue;
                }
                if (startInv != null && inventoryAmount >= startInv)
                {
                    continue;
                }
            }

            for (ContainerInfo containerInfo : containerInfos)
            {
                BlockEntity blockEntity = containerAsBlockEntity(containerInfo);

                if (blockEntity == null || !containerInfo.isConfigured())
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

                    if (!hasItemInContainer(itemHandler, item))
                    {
                        continue;
                    }

                    if (itemConfigurationTypeProperty.getType() == ItemConfigurationTypeProperty.Type.ADVANCED)
                    {
                        Integer startCont = itemInfo.getStartContainerAmount();
                        Integer stopCont = itemInfo.getStopContainerAmount();
                        int containerAmount = countItemsInContainer(itemHandler, item);

                        if (stopCont != null && containerAmount <= stopCont)
                        {
                            continue;
                        }
                        if (startCont != null && containerAmount <= startCont)
                        {
                            continue;
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isTakeItems()
    {
        return true;
    }
}
