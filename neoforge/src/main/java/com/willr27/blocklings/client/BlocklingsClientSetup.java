package com.willr27.blocklings.client;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.client.gui.containers.BlocklingsMenus;
import com.willr27.blocklings.client.gui.screen.screens.EquipmentScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.willr27.blocklings.client.renderer.entity.model.BlocklingModel;
import com.willr27.blocklings.client.renderer.entity.model.BlocklingModelLayers;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Blocklings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BlocklingsClientSetup {
    private BlocklingsClientSetup() {
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BlocklingModelLayers.BLOCKLING, BlocklingModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(BlocklingsMenus.EQUIPMENT.get(), EquipmentScreen::new);
    }
}
