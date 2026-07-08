package com.willr27.blocklings.client.gui.control.controls;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.willr27.blocklings.client.gui.control.Control;
import com.willr27.blocklings.client.gui.util.ScissorStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;
import org.joml.Quaternionf;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class BlockControl extends Control
{
    private double previousMouseX = 0.0;
    private double previousMouseY = 0.0;
    protected double dragAmount = 0.0;

    @Nonnull
    protected Quaternionf rotationQuat = new Quaternionf();

    @Nonnull
    private Block block = Blocks.AIR;

    private float blockScale = 0.6f;
    private boolean canMouseRotate = false;

    protected float x;
    protected float y;
    protected float z;
    protected float scale;

    public BlockControl()
    {
        super();
        rotationQuat.rotateAxis((float) Math.toRadians(30.0f), 1.0f, 0.0f, 0.0f);
        rotationQuat.rotateAxis((float) Math.toRadians(45.0f), 0.0f, 1.0f, 0.0f);
    }

    @Override
    protected void onRender(@Nonnull GuiGraphics guiGraphics, @Nonnull ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
    {
        super.onRender(guiGraphics, scissorStack, mouseX, mouseY, partialTicks);

        double pixelMouseDeltaX = mouseX - previousMouseX;
        double pixelMouseDeltaY = mouseY - previousMouseY;
        float mouseDeltaX = (float) (pixelMouseDeltaX / getPixelScaleX());
        float mouseDeltaY = (float) (pixelMouseDeltaY / getPixelScaleY());

        if (isPressed() && canMouseRotate())
        {
            Quaternionf quat = new Quaternionf().rotateAxis(
                    (float) Math.toRadians((float) (mouseDeltaX * getPixelScaleX()) * 0.4f), 0.0f, 1.0f, 0.0f);
            quat.rotateAxis((float) Math.toRadians((float) (mouseDeltaY * getPixelScaleY()) * 0.4f), 1.0f, 0.0f, 0.0f);
            quat.mul(rotationQuat);
            rotationQuat = quat;
            dragAmount += Math.abs(mouseDeltaX) + Math.abs(mouseDeltaY);
        }
        else
        {
            dragAmount = 0.0;
        }

        z = isDraggingOrAncestor() ? (float) getDraggedControl().getDragZ() : (float) getRenderZ();
        z += guiGraphics.pose().last().pose().m32();

        scale = (float) (Math.min(getWidth(), getHeight()) * getScaleX()) * getBlockScale();
        float width = scale / 2.0f;
        double cubeDiagFromCenterToCorner = Math.sqrt(3 * width * width);
        x = (float) ((getPixelX() / getPixelScaleX()) * getScaleX());
        y = (float) ((getPixelY() / getPixelScaleY()) * getScaleY());
        z += (float) cubeDiagFromCenterToCorner;
        double extraX = (((getPixelWidth() / 2.0) / scale) / getPixelScaleX()) * getScaleX();
        double extraY = (((getPixelHeight() / 2.0) / scale) / getPixelScaleY()) * getScaleY();

        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(scale, -scale, scale);
        pose.translate(extraX, -extraY, 0.0);
        pose.mulPose(rotationQuat);
        if (getBlockState().getRenderShape() == RenderShape.MODEL)
        {
            pose.mulPose(Axis.YP.rotationDegrees(180.0f));
        }
        pose.translate(-0.5f, -0.5f, -0.5f);

        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.enableBlend();
        blockRenderer.renderSingleBlock(getBlockState(), pose, bufferSource, 15728880, OverlayTexture.NO_OVERLAY);
        bufferSource.endBatch();
        pose.popPose();

        previousMouseX = mouseX;
        previousMouseY = mouseY;
    }

    @Nonnull
    public BlockState getBlockState()
    {
        return block.defaultBlockState();
    }

    @Nonnull
    public Block getBlock()
    {
        return block;
    }

    public void setBlock(@Nonnull Block block)
    {
        this.block = block;
    }

    public float getBlockScale()
    {
        return blockScale;
    }

    public void setBlockScale(float blockScale)
    {
        this.blockScale = blockScale;
    }

    public boolean canMouseRotate()
    {
        return canMouseRotate;
    }

    public void setCanMouseRotate(boolean canMouseRotate)
    {
        this.canMouseRotate = canMouseRotate;
    }

    @Nonnull
    public Quaternionf getRotationQuat()
    {
        return rotationQuat;
    }

    public void setRotationQuat(@Nonnull Quaternionf rotationQuat)
    {
        this.rotationQuat = rotationQuat;
    }
}
