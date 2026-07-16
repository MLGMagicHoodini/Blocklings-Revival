package com.willr27.blocklings.client.gui.control.controls.config;

import com.willr27.blocklings.Blocklings;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import javax.annotation.Nonnull;

/**
 * World click handlers for Deposit/Take container configuration.
 * Registered on both sides: client applies the selection, server cancels chest opening while configuring.
 */
@EventBusSubscriber(modid = Blocklings.MODID)
public final class ContainerControlEvents
{
    private ContainerControlEvents()
    {
    }

    @SubscribeEvent
    public static void onPlayerContainerSelect(@Nonnull PlayerInteractEvent.RightClickBlock event)
    {
        // Always treat as final: RightClickBlock is often cancelled on MAIN_HAND before OFF_HAND
        // runs, which previously left isConfiguring stuck true and blocked chest GUIs forever.
        boolean handled = ContainerControl.handleContainerSelect(event.getEntity(), true, event.getPos());

        if (!handled)
        {
            handled = FarmingAreaSelection.handleSelect(event.getEntity(), true, event.getPos());
        }

        if (handled)
        {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setUseBlock(TriState.FALSE);
            event.setUseItem(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onPlayerContainerSelectCancel(@Nonnull PlayerInteractEvent.LeftClickBlock event)
    {
        boolean handled = ContainerControl.handleContainerSelect(event.getEntity(), true, null);

        if (!handled)
        {
            handled = FarmingAreaSelection.handleSelect(event.getEntity(), true, null);
        }

        if (handled)
        {
            event.setCanceled(true);
            event.setUseBlock(TriState.FALSE);
            event.setUseItem(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onPlayerContainerSelectCancel(@Nonnull PlayerInteractEvent.EntityInteract event)
    {
        if (!ContainerControl.handleContainerSelect(event.getEntity(), true, null))
        {
            FarmingAreaSelection.handleSelect(event.getEntity(), true, null);
        }
    }
}
