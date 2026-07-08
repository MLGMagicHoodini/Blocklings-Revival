package com.willr27.blocklings.client.renderer.entity.model;

import com.willr27.blocklings.Blocklings;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public final class BlocklingModelLayers {
    public static final ModelLayerLocation BLOCKLING = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "blockling"), "main");

    private BlocklingModelLayers() {
    }
}
