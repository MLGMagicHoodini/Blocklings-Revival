package com.willr27.blocklings.client.renderer.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingHand;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class BlocklingModel extends EntityModel<BlocklingEntity> implements ArmedModel {
    public static final float BODY_BASE_ROT_X = 0.0872665F;
    public static final float RIGHT_LEG_BASE_ROT_X = -BODY_BASE_ROT_X;
    public static final float LEFT_LEG_BASE_ROT_X = -BODY_BASE_ROT_X;
    public static final float RIGHT_ARM_BASE_ROT_X = 0.785398F - BODY_BASE_ROT_X;
    public static final float LEFT_ARM_BASE_ROT_X = 0.785398F - BODY_BASE_ROT_X;

    private final ModelPart body;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    private float scaleX = 1.0F;
    private float scaleY = 1.0F;

    public BlocklingModel(ModelPart root) {
        this.body = root.getChild("body");
        this.rightLeg = body.getChild("right_leg");
        this.leftLeg = body.getChild("left_leg");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bodyPart = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 0).addBox(-6.0F, -3.0F, -6.0F, 12.0F, 12.0F, 12.0F),
                PartPose.offsetAndRotation(0.0F, 13.0F, 0.0F, BODY_BASE_ROT_X, 0.0F, 0.0F));

        bodyPart.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(16, 24).addBox(-1.5F, 1.0F, -3.5F, 5.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(-4.0F, 4.0F, 0.5F, -RIGHT_LEG_BASE_ROT_X, 0.0F, 0.0F));
        bodyPart.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(42, 24).addBox(-3.5F, 1.0F, -3.5F, 5.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(4.0F, 4.0F, 0.5F, -LEFT_LEG_BASE_ROT_X, 0.0F, 0.0F));
        bodyPart.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 12).addBox(0.0F, 0.0F, -7.0F, 2.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(-8.0F, 0.0F, 0.0F, RIGHT_ARM_BASE_ROT_X, 0.0F, 0.0F));
        bodyPart.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(64, 12).addBox(-2.0F, 0.0F, -7.0F, 2.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(8.0F, 0.0F, 0.0F, LEFT_ARM_BASE_ROT_X, 0.0F, 0.0F));
        bodyPart.addOrReplaceChild("right_eye",
                CubeListBuilder.create().texOffs(22, 8).addBox(-1.0F, -0.2F, 1.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offset(-2.0F, 3.0F, -8.0F));
        bodyPart.addOrReplaceChild("left_eye",
                CubeListBuilder.create().texOffs(52, 8).addBox(-1.0F, -0.2F, 1.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offset(2.0F, 3.0F, -8.0F));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(@Nonnull BlocklingEntity blockling, float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch) {
        float scale = blockling.getBlocklingScale();
        if (scale <= 0.0F)
        {
            scale = 1.0F;
        }
        scaleX = scale;
        scaleY = scale;

        float partialTicks = ageInTicks % 1.0F;

        // Use pathfinding walk cycle only — do not mix in ageInTicks (that caused a constant drunk wobble).
        float walkSwing = limbSwing * 0.6662F;
        float walkAmount = Math.min(limbSwingAmount, 1.0F);

        float rightLegSwingAmount = walkAmount;
        float leftLegSwingAmount = walkAmount;
        float rightArmSwingAmount = walkAmount;
        float leftArmSwingAmount = walkAmount;

        float bodySwing = 0.0F;
        float rightArmSwing = 0.0F;
        float leftArmSwing = 0.0F;
        float rightLegSwing = 0.0F;
        float leftLegSwing = 0.0F;

        float weaponBonusRotX = 0.7F;

        BlocklingHand hand = blockling.getStats().InteractionHand.getValue();
        BlocklingHand attackingHand = blockling.getEquipment().findAttackingHand();

        if (blockling.getTarget() != null) {
            if (attackingHand == BlocklingHand.MAIN || attackingHand == BlocklingHand.BOTH) {
                rightArmSwing -= blockling.getEquipment().getHandStack(InteractionHand.MAIN_HAND).isEmpty() ? 0.0F : weaponBonusRotX;
                rightArmSwingAmount /= 2.0F;
            }

            if (attackingHand == BlocklingHand.OFF || attackingHand == BlocklingHand.BOTH) {
                leftArmSwing += blockling.getEquipment().getHandStack(InteractionHand.OFF_HAND).isEmpty() ? 0.0F : weaponBonusRotX;
                leftArmSwingAmount /= 2.0F;
            }
        }

        if (blockling.getActions().attack.isRunning(BlocklingHand.MAIN)) {
            float percent = blockling.getActions().attack.percentThroughHandAction(-1)
                    + (blockling.getActions().attack.percentThroughHandAction()
                    - blockling.getActions().attack.percentThroughHandAction(-1)) * partialTicks;
            float attackSwing = Mth.cos(percent * (float) Math.PI / 2.0F) * 2.0F;
            rightArmSwing += blockling.getEquipment().getHandStack(InteractionHand.MAIN_HAND).isEmpty() ? -attackSwing : attackSwing;
        }

        if (blockling.getActions().attack.isRunning(BlocklingHand.OFF)) {
            float percent = blockling.getActions().attack.percentThroughHandAction(-1)
                    + (blockling.getActions().attack.percentThroughHandAction()
                    - blockling.getActions().attack.percentThroughHandAction(-1)) * partialTicks;
            float attackSwing = Mth.cos(percent * (float) Math.PI / 2.0F) * 2.0F;
            leftArmSwing -= blockling.getEquipment().getHandStack(InteractionHand.OFF_HAND).isEmpty() ? -attackSwing : attackSwing;
        }

        if (blockling.getActions().gather.isRunning()) {
            if (hand == BlocklingHand.MAIN || hand == BlocklingHand.BOTH) {
                rightArmSwing = Mth.cos(ageInTicks + (float) Math.PI) * 1.0F;
            }

            if (hand == BlocklingHand.OFF || hand == BlocklingHand.BOTH) {
                leftArmSwing = Mth.cos(ageInTicks + (float) Math.PI) * 1.0F;
            }
        }

        bodySwing += Mth.cos(walkSwing + (float) Math.PI) * walkAmount * 0.1F;
        rightArmSwing += Mth.cos(walkSwing + (float) Math.PI) * rightArmSwingAmount * 0.8F;
        leftArmSwing += Mth.cos(walkSwing) * leftArmSwingAmount * 0.8F;
        rightLegSwing += Mth.cos(walkSwing + (float) Math.PI) * rightLegSwingAmount * 0.5F;
        leftLegSwing += Mth.cos(walkSwing) * leftLegSwingAmount * 0.5F;

        rightArm.xRot = rightArmSwing + RIGHT_ARM_BASE_ROT_X;
        leftArm.xRot = LEFT_ARM_BASE_ROT_X - leftArmSwing;
        rightLeg.xRot = RIGHT_LEG_BASE_ROT_X - rightLegSwing;
        leftLeg.xRot = leftLegSwing + LEFT_LEG_BASE_ROT_X;

        body.zRot = bodySwing;
        rightLeg.zRot = -body.zRot;
        leftLeg.zRot = -body.zRot;
    }

    @Override
    public void renderToBuffer(@Nonnull PoseStack poseStack, @Nonnull VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        poseStack.pushPose();
        poseStack.translate(0.0, 1.501, 0.0);
        poseStack.scale(scaleX, scaleY, scaleX);
        poseStack.translate(0.0, -1.501, 0.0);
        body.render(poseStack, buffer, packedLight, packedOverlay, color);
        poseStack.popPose();
    }

    @Override
    public void translateToHand(@Nonnull HumanoidArm arm, @Nonnull PoseStack poseStack) {
        body.translateAndRotate(poseStack);
        if (arm == HumanoidArm.LEFT) {
            leftArm.translateAndRotate(poseStack);
        } else {
            rightArm.translateAndRotate(poseStack);
        }
    }
}
