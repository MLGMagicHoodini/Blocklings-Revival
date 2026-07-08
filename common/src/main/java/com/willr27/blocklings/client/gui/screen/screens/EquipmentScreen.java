package com.willr27.blocklings.client.gui.screen.screens;

import com.willr27.blocklings.client.gui.control.controls.EntityControl;
import com.willr27.blocklings.client.gui.control.controls.TabbedUIControl;
import com.willr27.blocklings.client.gui.BlocklingGuiHandler;
import com.willr27.blocklings.client.gui.containers.EquipmentContainer;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;

/**
 * A screen that displays the blockling's equipment.
 */
@OnlyIn(Dist.CLIENT)
public class EquipmentScreen extends TabbedContainerScreen<EquipmentContainer>
{
    public EquipmentScreen(@Nonnull EquipmentContainer container, @Nonnull Inventory inventory, @Nonnull Component title)
    {
        super(container.blockling, container, TabbedUIControl.Tab.EQUIPMENT);
        container.blockling.guiHandler.setRecentGuiId(BlocklingGuiHandler.EQUIPMENT_ID);
        tabbedUIControl.contentControl.setInteractive(false);
        initEntityPreview(container.blockling);
    }

    private void initEntityPreview(@Nonnull BlocklingEntity blockling)
    {
        EntityControl entityControl = new EntityControl();
        entityControl.setParent(tabbedUIControl.contentControl);
        entityControl.setWidth(48);
        entityControl.setHeight(48);
        entityControl.setHorizontalAlignment(0.0);
        entityControl.setVerticalAlignment(0.0);
        entityControl.setEntity(blockling);
        entityControl.setEntityScale(0.7f);
        entityControl.setScaleToBoundingBox(true);
        entityControl.setOffsetY(-3.0f);
        entityControl.setClipContentsToBounds(false);
        entityControl.setPassThroughMouseClicks(true);
    }
}
