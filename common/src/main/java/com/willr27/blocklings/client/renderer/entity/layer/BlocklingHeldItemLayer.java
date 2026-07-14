package com.willr27.blocklings.client.renderer.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.willr27.blocklings.client.renderer.entity.model.BlocklingModel;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.util.ToolUtil;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;

/**
 * Renders tools/items held by a blockling, with Blocklings-specific hand offsets
 * (vanilla {@link net.minecraft.client.renderer.entity.layers.ItemInHandLayer} does not align to our arm cubes).
 */
@OnlyIn(Dist.CLIENT)
public class BlocklingHeldItemLayer extends RenderLayer<BlocklingEntity, BlocklingModel>
{
    @Nonnull
    private final ItemInHandRenderer itemInHandRenderer;

    public BlocklingHeldItemLayer(@Nonnull RenderLayerParent<BlocklingEntity, BlocklingModel> renderer,
                                  @Nonnull ItemInHandRenderer itemInHandRenderer)
    {
        super(renderer);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(@Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer, int packedLight,
                       @Nonnull BlocklingEntity blockling, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch)
    {
        ItemStack mainStack = blockling.getMainHandItem();
        ItemStack offStack = blockling.getOffhandItem();

        if (!mainStack.isEmpty())
        {
            renderItem(poseStack, mainStack, false, blockling, buffer, packedLight);
        }

        if (!offStack.isEmpty())
        {
            renderItem(poseStack, offStack, true, blockling, buffer, packedLight);
        }
    }

    private void renderItem(@Nonnull PoseStack poseStack, @Nonnull ItemStack stack, boolean isLeftHand,
                            @Nonnull BlocklingEntity blockling, @Nonnull MultiBufferSource buffer, int packedLight)
    {
        poseStack.pushPose();
        // Match body scale path in BlocklingModel.renderToBuffer (vanilla MobRenderer applies a 1.501 offset).
        poseStack.translate(0.0, 1.501, 0.0);
        float scale = blockling.getBlocklingScale();
        if (scale <= 0.0F)
        {
            scale = 1.0F;
        }
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0, -1.501, 0.0);

        getParentModel().translateToHand(isLeftHand ? HumanoidArm.LEFT : HumanoidArm.RIGHT, poseStack);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(190.0F));
        poseStack.translate((isLeftHand ? 1.0F : -1.0F) / 16.0F, -0.1F, getItemHandDisplacement(stack));

        itemInHandRenderer.renderItem(
                blockling,
                stack,
                isLeftHand ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                isLeftHand,
                poseStack,
                buffer,
                packedLight);

        poseStack.popPose();
    }

    private float getItemHandDisplacement(@Nonnull ItemStack stack)
    {
        if (ToolUtil.isWeapon(stack))
        {
            return -0.3044F;
        }

        return -0.3552F;
    }
}
