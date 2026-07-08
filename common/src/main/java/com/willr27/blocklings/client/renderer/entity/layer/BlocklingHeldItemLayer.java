package com.willr27.blocklings.client.renderer.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.willr27.blocklings.client.renderer.entity.model.BlocklingModel;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class BlocklingHeldItemLayer extends ItemInHandLayer<BlocklingEntity, BlocklingModel> {
    public BlocklingHeldItemLayer(@Nonnull RenderLayerParent<BlocklingEntity, BlocklingModel> renderer,
                                  @Nonnull ItemInHandRenderer itemInHandRenderer) {
        super(renderer, itemInHandRenderer);
    }

    @Override
    public void render(@Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer, int packedLight,
                       @Nonnull BlocklingEntity blockling, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        poseStack.pushPose();
        poseStack.translate(0.0, 1.501, 0.0);
        poseStack.scale(blockling.getBlocklingScale(), blockling.getBlocklingScale(), blockling.getBlocklingScale());
        poseStack.translate(0.0, -1.501, 0.0);
        super.render(poseStack, buffer, packedLight, blockling, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        poseStack.popPose();
    }
}
