package com.willr27.blocklings;

import com.willr27.blocklings.client.BlocklingItemClientRegistration;
import com.willr27.blocklings.client.gui.control.controls.config.ContainerControlEvents;
import com.willr27.blocklings.client.gui.screen.screens.EquipmentScreen;
import com.willr27.blocklings.client.renderer.entity.BlocklingRenderer;
import com.willr27.blocklings.client.renderer.entity.model.BlocklingModel;
import com.willr27.blocklings.client.renderer.entity.model.BlocklingModelLayers;
import com.willr27.blocklings.entity.BlocklingsEntityTypes;
import com.willr27.blocklings.entity.BlocklingsMenus;
import com.willr27.blocklings.platform.FabricNetworkBridge;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public class BlocklingsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(BlocklingsEntityTypes.BLOCKLING, BlocklingRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(BlocklingModelLayers.BLOCKLING, BlocklingModel::createBodyLayer);
        MenuScreens.register(BlocklingsMenus.EQUIPMENT, EquipmentScreen::new);
        BlocklingItemClientRegistration.registerItemModelsProperties();
        ContainerControlEvents.register();
        FabricNetworkBridge.registerClientReceivers();
    }
}
