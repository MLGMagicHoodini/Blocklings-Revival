package com.willr27.blocklings.client.gui.control;

import com.willr27.blocklings.client.gui.texture.Texture;
import com.willr27.blocklings.client.gui.util.GuiUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Rendering helpers for {@link Control} implementations.
 */
@OnlyIn(Dist.CLIENT)
public abstract class GuiControl
{
    public void renderRectangle(@Nonnull GuiGraphics guiGraphics, double x, double y, int width, int height, int colour)
    {
        guiGraphics.fill(
                (int) Math.round(x),
                (int) Math.round(y),
                (int) Math.round(x + width),
                (int) Math.round(y + height),
                colour);
    }

    public void renderCenteredRectangle(@Nonnull GuiGraphics guiGraphics, double x, double y, double width, double height, int colour)
    {
        renderRectangle(
                guiGraphics,
                x - width / 2.0,
                y - height / 2.0,
                (int) Math.round(width),
                (int) Math.round(height),
                colour);
    }

    protected void renderTexture(@Nonnull GuiGraphics guiGraphics, @Nonnull Texture texture)
    {
        guiGraphics.blit(texture.resourceLocation, 0, 0, texture.x, texture.y, texture.width, texture.height, 256, 256);
    }

    protected void renderTexture(@Nonnull GuiGraphics guiGraphics, @Nonnull Texture texture, double x, double y, double scaleX, double scaleY)
    {
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate((int) Math.round(x), (int) Math.round(y), 0.0);
        pose.scale((float) scaleX, (float) scaleY, 1.0f);
        renderTexture(guiGraphics, texture);
        pose.popPose();
    }

    protected void renderShadowedText(@Nonnull GuiGraphics guiGraphics, @Nonnull FormattedCharSequence text, int colour)
    {
        GuiUtil.get().renderShadowedText(guiGraphics, text, 0, 0, colour);
    }

    protected void renderShadowedText(@Nonnull GuiGraphics guiGraphics, @Nonnull FormattedCharSequence text, int x, int y, int colour)
    {
        GuiUtil.get().renderShadowedText(guiGraphics, text, x, y, colour);
    }

    protected void renderText(@Nonnull GuiGraphics guiGraphics, @Nonnull FormattedCharSequence text, int colour)
    {
        GuiUtil.get().renderText(guiGraphics, text, 0, 0, colour);
    }

    public void renderTooltip(@Nonnull GuiGraphics guiGraphics, double mouseX, double mouseY, double pixelScaleX, double pixelScaleY, @Nonnull Component tooltip)
    {
        List<FormattedCharSequence> tooltipLines = new ArrayList<>();
        tooltipLines.add(tooltip.getVisualOrderText());
        renderTooltip(guiGraphics, mouseX, mouseY, pixelScaleX, pixelScaleY, tooltipLines);
    }

    public void renderTooltip(@Nonnull GuiGraphics guiGraphics, double mouseX, double mouseY, double pixelScaleX, double pixelScaleY, @Nonnull List<FormattedCharSequence> tooltip)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null)
        {
            guiGraphics.renderTooltip(minecraft.font, tooltip, (int) (mouseX / pixelScaleX), (int) (mouseY / pixelScaleY));
        }
    }
}
