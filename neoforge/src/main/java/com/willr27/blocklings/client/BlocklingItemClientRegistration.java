package com.willr27.blocklings.client;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.BlocklingsItems;
import net.minecraft.client.renderer.item.ItemProperties;

/**
 * Fabric client registration for blockling item model overrides.
 */
public final class BlocklingItemClientRegistration {
    private BlocklingItemClientRegistration() {
    }

    public static void registerItemModelsProperties() {
        ItemProperties.register(
                BlocklingsItems.BLOCKLING,
                BlocklingItemModelProperties.TYPE_PROPERTY,
                BlocklingItemModelProperties.createTypeProperty());
        Blocklings.LOGGER.debug("Registered item property {}", BlocklingItemModelProperties.TYPE_PROPERTY);
    }
}
