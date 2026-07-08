package com.willr27.blocklings.client.renderer.entity;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.PigModel;

/** Renderer stub — replace with BlocklingModel when assets are ported. */
public class BlocklingRenderer extends MobRenderer<BlocklingEntity, PigModel<BlocklingEntity>> {
    private static final ResourceLocation TEX =
            ResourceLocation.withDefaultNamespace("textures/entity/pig/pig.png");

    public BlocklingRenderer(EntityRendererProvider.Context context) {
        super(context, new PigModel<>(context.bakeLayer(PigModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(BlocklingEntity entity) {
        return TEX;
    }
}
