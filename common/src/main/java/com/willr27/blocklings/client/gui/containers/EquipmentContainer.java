package com.willr27.blocklings.client.gui.containers;

import com.willr27.blocklings.loader.BlocklingsRegistries;
import com.willr27.blocklings.client.gui.containers.slots.ToolSlot;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.inventory.EquipmentInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class EquipmentContainer extends AbstractContainerMenu
{
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 79;

    @Nonnull
    public final BlocklingEntity blockling;

    public EquipmentContainer(int windowId, @Nonnull Player player, @Nonnull BlocklingEntity blockling)
    {
        super((net.minecraft.world.inventory.MenuType<EquipmentContainer>) BlocklingsRegistries.equipmentMenu(), windowId);
        this.blockling = blockling;

        EquipmentInventory blocklingInv = blockling.getEquipment();

        addSlot(new ToolSlot(blocklingInv, EquipmentInventory.TOOL_MAIN_HAND, 12, PLAYER_INV_Y - 22));
        addSlot(new ToolSlot(blocklingInv, EquipmentInventory.TOOL_OFF_HAND, 36, PLAYER_INV_Y - 22));

        for (int i = 0; i < 4; i++)
        {
            for (int j = 0; j < 6; j++)
            {
                addSlot(new Slot(blocklingInv, j + i * 6 + 2, PLAYER_INV_X + (j * 18) + 54, PLAYER_INV_Y + (i * 18) - 76));
            }
        }

        Inventory inventory = player.getInventory();
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 9; j++)
            {
                addSlot(new Slot(inventory, j + i * 9 + 9, PLAYER_INV_X + (j * 18), PLAYER_INV_Y + (i * 18)));
            }
        }
        for (int i = 0; i < 9; i++)
        {
            addSlot(new Slot(inventory, i, PLAYER_INV_X + (i * 18), PLAYER_INV_Y + 58));
        }
    }

    @Override
    public boolean stillValid(@Nonnull Player player)
    {
        return true;
    }

    /** Blockling tool + bag slots occupy indices [0, PLAYER_INV_START). */
    private static final int PLAYER_INV_START = 26;

    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int clickedSlotIndex)
    {
        ItemStack remainder = ItemStack.EMPTY;
        if (clickedSlotIndex < 0 || clickedSlotIndex >= this.slots.size())
        {
            return remainder;
        }

        Slot clickedSlot = this.slots.get(clickedSlotIndex);

        if (clickedSlot.hasItem())
        {
            ItemStack clickedSlotStack = clickedSlot.getItem();
            remainder = clickedSlotStack.copy();

            // moveItemStackTo end index is exclusive; slots are 0 .. size-1.
            int playerInvEnd = this.slots.size();

            if (clickedSlotIndex >= PLAYER_INV_START)
            {
                // Player inventory / hotbar → blockling equipment
                if (!this.moveItemStackTo(clickedSlotStack, 0, PLAYER_INV_START, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (!this.moveItemStackTo(clickedSlotStack, PLAYER_INV_START, playerInvEnd, true))
            {
                // Blockling equipment → player inventory / hotbar
                return ItemStack.EMPTY;
            }

            if (clickedSlotStack.isEmpty())
            {
                clickedSlot.set(ItemStack.EMPTY);
            }
            else
            {
                clickedSlot.setChanged();
            }

            blockling.getEquipment().updateToolAttributes();
            this.broadcastChanges();
        }

        return remainder;
    }
}
