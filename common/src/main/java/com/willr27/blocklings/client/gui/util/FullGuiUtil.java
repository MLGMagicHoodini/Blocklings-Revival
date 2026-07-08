package com.willr27.blocklings.client.gui.util;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.willr27.blocklings.client.gui.texture.Texture;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class FullGuiUtil extends GuiUtil
{
    @Nonnull
    private static final Minecraft mc = Minecraft.getInstance();

    @Override
    public float getGuiScale()
    {
        return (float) mc.getWindow().getGuiScale();
    }

    @Override
    public float getMaxGuiScale()
    {
        return (float) mc.getWindow().calculateScale(0, mc.isEnforceUnicode());
    }

    @Override
    public int getPixelMouseX()
    {
        return (int) mc.mouseHandler.xpos();
    }

    @Override
    public int getPixelMouseY()
    {
        return (int) mc.mouseHandler.ypos();
    }

    @Override
    public boolean isKeyDown(int key)
    {
        return InputConstants.isKeyDown(mc.getWindow().getWindow(), key);
    }

    @Override
    public boolean isKeyDown(@Nonnull KeyMapping key)
    {
        return key.isDown();
    }

    @Override
    public boolean isControlKeyDown()
    {
        return isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) || isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL);
    }

    @Override
    public boolean isCrouchKeyDown()
    {
        return isKeyDown(mc.options.keyShift);
    }

    @Override
    public boolean isCloseKey(int key)
    {
        return key == GLFW.GLFW_KEY_ESCAPE || mc.options.keyInventory.isDown();
    }

    @Override
    public boolean isUnfocusTextFieldKey(int key)
    {
        return key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER;
    }

    @Nonnull
    @Override
    public FormattedText trimWithEllipsis(@Nonnull FormattedText text, int width)
    {
        if (text.getString().equals(trim(text, width).getString()))
        {
            return text;
        }

        return FormattedText.composite(trim(text, width - mc.font.width("...")), Component.literal("..."));
    }

    @Nonnull
    @Override
    public FormattedText trim(@Nonnull FormattedText text, int width)
    {
        return mc.font.substrByWidth(text, width);
    }

    @Nonnull
    @Override
    public List<FormattedCharSequence> split(@Nonnull FormattedText text, int width)
    {
        return new ArrayList<>(mc.font.split(text, width));
    }

    @Nonnull
    @Override
    public List<String> split(@Nonnull String text, int width)
    {
        return mc.font.getSplitter().splitLines(text, width, Style.EMPTY).stream().map(FormattedText::getString).collect(Collectors.toList());
    }

    @Override
    public int getTextWidth(@Nonnull String text)
    {
        return mc.font.width(text);
    }

    @Override
    public int getTextWidth(@Nonnull FormattedCharSequence text)
    {
        return mc.font.width(text);
    }

    @Override
    public int getLineHeight()
    {
        return mc.font.lineHeight;
    }

    @Override
    public void renderShadowedText(@Nonnull GuiGraphics guiGraphics, @Nonnull FormattedCharSequence text, int x, int y, int color)
    {
        guiGraphics.drawString(mc.font, text, x, y, color, true);
    }

    @Override
    public void renderText(@Nonnull GuiGraphics guiGraphics, @Nonnull FormattedCharSequence text, int x, int y, int color)
    {
        guiGraphics.drawString(mc.font, text, x, y, color, false);
    }

    @Override
    public void bindTexture(@Nonnull ResourceLocation texture)
    {
        mc.getTextureManager().bindForSetup(texture);
    }

    @Override
    public void bindTexture(@Nonnull Texture texture)
    {
        bindTexture(texture.resourceLocation);
    }

    @Override
    public void renderEntityOnScreen(@Nonnull GuiGraphics guiGraphics, @Nonnull LivingEntity entity, int screenX, int screenY, float screenMouseX, float screenMouseY, float scale, boolean scaleToBoundingBox, boolean faceCamera, boolean centerVertically)
    {
        Component name = entity.getCustomName();
        entity.setCustomName(null);

        float f = (float) Math.atan((screenX - screenMouseX) / 40.0F);
        float f1 = (float) Math.atan((screenY - screenMouseY) / 40.0F);

        float guiScale = (float) mc.getWindow().getGuiScale();
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        // ScreenControl scales the gui by 1/guiScale; restore legacy coordinate space for entity rendering.
        poseStack.scale(guiScale, guiScale, 1.0F);
        poseStack.translate(screenX, screenY, 1050.0F);
        poseStack.scale(1.0F, 1.0F, -1.0F);
        poseStack.translate(0.0D, 0.0D, 1000.0D);

        float scale2 = scaleToBoundingBox ? 16.0f / Math.max(entity.getBbWidth(), entity.getBbHeight()) : 16.0f;
        poseStack.scale(scale * scale2, scale * scale2, scale * scale2);

        if (centerVertically)
        {
            poseStack.translate(0.0F, entity.getBbHeight() / 2.0F, 0.0F);
        }

        Quaternionf poseRotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf pitchRotation = new Quaternionf().rotateX(f1 * 20.0F * ((float) Math.PI / 180F));
        poseRotation.mul(pitchRotation);
        poseStack.mulPose(poseRotation);

        float bodyRot = entity.yBodyRot;
        float yRot = entity.getYRot();
        float xRot = entity.getXRot();
        float headRotO = entity.yHeadRotO;
        float headRot = entity.yHeadRot;

        entity.yBodyRot = 180.0F + f * 20.0F;
        entity.setYRot(180.0F + f * 40.0F);
        entity.setXRot(-f1 * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        Quaternionf cameraOverride = new Quaternionf(pitchRotation).conjugate();
        dispatcher.overrideCameraOrientation(cameraOverride);
        dispatcher.setRenderShadow(false);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, poseStack, bufferSource, 15728880);
        bufferSource.endBatch();
        dispatcher.setRenderShadow(true);

        entity.yBodyRot = bodyRot;
        entity.setYRot(yRot);
        entity.setXRot(xRot);
        entity.yHeadRotO = headRotO;
        entity.yHeadRot = headRot;

        poseStack.popPose();
        entity.setCustomName(name);
    }

    @Override
    public void renderItemStack(@Nonnull GuiGraphics guiGraphics, @Nonnull ItemStack stack, int x, int y, double z, float scale)
    {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(scale, scale, 1.0f);
        guiGraphics.renderItem(stack, 0, 0);
        poseStack.popPose();
    }
}
