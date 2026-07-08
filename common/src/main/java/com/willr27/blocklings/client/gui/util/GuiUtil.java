package com.willr27.blocklings.client.gui.util;

import com.mojang.blaze3d.platform.Window;
import com.willr27.blocklings.client.gui.texture.Texture;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public abstract class GuiUtil
{
    @Nullable
    private static GuiUtil instance;

    @Nonnull
    public static GuiUtil get()
    {
        if (instance == null)
        {
            if (Minecraft.getInstance() != null)
            {
                instance = new FullGuiUtil();
            }
            else
            {
                instance = new TestGuiUtil();
            }
        }

        return instance;
    }

    public abstract float getGuiScale();

    public abstract float getMaxGuiScale();

    public abstract int getPixelMouseX();

    public abstract int getPixelMouseY();

    public abstract boolean isKeyDown(int key);

    public abstract boolean isKeyDown(@Nonnull KeyMapping key);

    public abstract boolean isControlKeyDown();

    public abstract boolean isCrouchKeyDown();

    public abstract boolean isCloseKey(int key);

    public abstract boolean isUnfocusTextFieldKey(int key);

    @Nonnull
    public abstract FormattedText trimWithEllipsis(@Nonnull FormattedText text, int width);

    @Nonnull
    public abstract FormattedText trim(@Nonnull FormattedText text, int width);

    @Nonnull
    public abstract List<FormattedCharSequence> split(@Nonnull FormattedText text, int width);

    @Nonnull
    public abstract List<String> split(@Nonnull String text, int width);

    public abstract int getTextWidth(@Nonnull String text);

    public abstract int getTextWidth(@Nonnull FormattedCharSequence text);

    public abstract int getLineHeight();

    public abstract void renderShadowedText(@Nonnull GuiGraphics guiGraphics, @Nonnull FormattedCharSequence text, int x, int y, int color);

    public abstract void renderText(@Nonnull GuiGraphics guiGraphics, @Nonnull FormattedCharSequence text, int x, int y, int color);

    public abstract void bindTexture(@Nonnull ResourceLocation texture);

    public abstract void bindTexture(@Nonnull Texture texture);

    public abstract void renderEntityOnScreen(@Nonnull GuiGraphics guiGraphics, @Nonnull LivingEntity entity, int screenX, int screenY, float screenMouseX, float screenMouseY, float scale, boolean scaleToBoundingBox, boolean faceCamera, boolean centerVertically);

    public void renderEntityOnScreen(@Nonnull GuiGraphics guiGraphics, @Nonnull LivingEntity entity, int screenX, int screenY, float screenMouseX, float screenMouseY, float scale, boolean scaleToBoundingBox, boolean faceCamera)
    {
        renderEntityOnScreen(guiGraphics, entity, screenX, screenY, screenMouseX, screenMouseY, scale, scaleToBoundingBox, faceCamera, false);
    }

    public void renderEntityOnScreen(@Nonnull GuiGraphics guiGraphics, @Nonnull LivingEntity entity, int screenX, int screenY, float screenMouseX, float screenMouseY, float scale, boolean scaleToBoundingBox)
    {
        renderEntityOnScreen(guiGraphics, entity, screenX, screenY, screenMouseX, screenMouseY, scale, scaleToBoundingBox, false, false);
    }

    public abstract void renderItemStack(@Nonnull GuiGraphics guiGraphics, @Nonnull ItemStack stack, int x, int y, double z, float scale);

    public static void enableScissor()
    {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }

    public static void disableScissor()
    {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static void scissor(float x, float y, int width, int height)
    {
        Window window = Minecraft.getInstance().getWindow();
        float scale = 1.0f;

        int scissorX = (int) (x * scale);
        int scissorY = (int) (window.getHeight() - ((y + height) * scale));
        int scissorWidth = (int) (width * scale);
        int scissorHeight = (int) (height * scale);

        enableScissor();
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }
}
