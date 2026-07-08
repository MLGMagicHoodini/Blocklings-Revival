package com.willr27.blocklings.client.gui.control.controls.config;

import com.willr27.blocklings.Blocklings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import javax.annotation.Nonnull;

@EventBusSubscriber(modid = Blocklings.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ContainerControlEvents {
    private ContainerControlEvents() {
    }

    @SubscribeEvent
    public static void onPlayerContainerSelect(@Nonnull PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(ContainerControl.handleContainerSelect(
                event.getEntity(), event.getHand() == InteractionHand.OFF_HAND, event.getPos()));
    }

    @SubscribeEvent
    public static void onPlayerContainerSelectCancel(@Nonnull PlayerInteractEvent.LeftClickBlock event) {
        event.setCanceled(ContainerControl.handleContainerSelect(event.getEntity(), true, null));
    }

    @SubscribeEvent
    public static void onPlayerContainerSelectCancel(@Nonnull PlayerInteractEvent.EntityInteract event) {
        ContainerControl.handleContainerSelect(event.getEntity(), true, null);
    }
}
