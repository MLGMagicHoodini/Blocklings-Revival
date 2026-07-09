package com.willr27.blocklings.client;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.item.BlocklingsItems;
import net.minecraft.client.renderer.item.ItemProperties;

/**
 * NeoForge client registration for blockling item model overrides.
 */
public final class BlocklingItemClientRegistration {
    private BlocklingItemClientRegistration() {
    }

    public static void registerItemModelsProperties() {
        ItemProperties.register(
                BlocklingsItems.BLOCKLING.get(),
                BlocklingItemModelProperties.TYPE_PROPERTY,
                BlocklingItemModelProperties.createTypeProperty());
        Blocklings.LOGGER.debug("Registered item property {}", BlocklingItemModelProperties.TYPE_PROPERTY);
    }
}
