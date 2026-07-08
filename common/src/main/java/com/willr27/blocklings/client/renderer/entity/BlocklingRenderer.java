package com.willr27.blocklings.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.willr27.blocklings.client.renderer.entity.layer.BlocklingHeldItemLayer;
import com.willr27.blocklings.client.renderer.entity.model.BlocklingModel;
import com.willr27.blocklings.client.renderer.entity.model.BlocklingModelLayers;
import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class BlocklingRenderer extends MobRenderer<BlocklingEntity, BlocklingModel> {
    public BlocklingRenderer(EntityRendererProvider.Context context) {
        super(context, new BlocklingModel(context.bakeLayer(BlocklingModelLayers.BLOCKLING)), 1.0F);
        addLayer(new BlocklingHeldItemLayer(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(@Nonnull BlocklingEntity blockling, float entityYaw, float partialTicks, @Nonnull PoseStack poseStack,
                       @Nonnull MultiBufferSource buffer, int packedLight) {
        shadowRadius = blockling.getBlocklingScale() * 0.5F;
        super.render(blockling, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    @Nonnull
    public ResourceLocation getTextureLocation(@Nonnull BlocklingEntity blockling) {
        if (blockling.getBlocklingType() == blockling.getNaturalBlocklingType()
                || BlocklingsConfig.CLIENT.disableDirtyBlocklings.get()) {
            return blockling.getBlocklingType().entityTexture;
        }

        return blockling.getNaturalBlocklingType().getCombinedTexture(
                blockling.getBlocklingType(), blockling.getBlocklingTypeVariant());
    }
}
