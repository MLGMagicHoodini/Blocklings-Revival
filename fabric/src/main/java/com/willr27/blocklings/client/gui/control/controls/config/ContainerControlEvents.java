package com.willr27.blocklings.client.gui.control.controls.config;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

public final class ContainerControlEvents {
    private ContainerControlEvents() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide()) {
                return InteractionResult.PASS;
            }
            if (ContainerControl.handleContainerSelect(player, hand == InteractionHand.OFF_HAND, hitResult.getBlockPos())) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClientSide()) {
                return InteractionResult.PASS;
            }
            if (ContainerControl.handleContainerSelect(player, true, null)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide()) {
                return InteractionResult.PASS;
            }
            ContainerControl.handleContainerSelect(player, true, null);
            return InteractionResult.PASS;
        });
    }
}
