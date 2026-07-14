package com.willr27.blocklings.client.gui.control.controls.config;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;

public final class ContainerControlEvents {
    private ContainerControlEvents() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // Always final so server isConfiguring is cleared even when only MAIN_HAND fires.
            if (ContainerControl.handleContainerSelect(player, true, hitResult.getBlockPos())) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (ContainerControl.handleContainerSelect(player, true, null)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ContainerControl.handleContainerSelect(player, true, null);
            return InteractionResult.PASS;
        });
    }
}
