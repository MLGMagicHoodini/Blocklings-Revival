package com.willr27.blocklings.client.gui.control.controls;

import net.minecraft.client.gui.GuiGraphics;
import com.willr27.blocklings.client.gui.control.Control;
import com.willr27.blocklings.client.gui.control.event.events.TryHoverEvent;
import com.willr27.blocklings.client.gui.control.event.events.input.MouseClickedEvent;
import com.willr27.blocklings.client.gui.properties.Visibility;
import com.willr27.blocklings.client.gui.util.ScissorStack;
import com.willr27.blocklings.client.gui.util.GuiUtil;
import net.minecraft.world.entity.LivingEntity;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A control used to display an entity.
 */
@OnlyIn(Dist.CLIENT)
public class EntityControl extends Control
{
    /**
     * The entity to display.
     */
    @Nonnull
    private LivingEntity entity;

    /**
     * The offset to display the entity at in the x-axis. Effected by {@link #entityScale}.
     * An offset of 1.0f at a scale of 1.0f would shift the entity by 1/16 of a block.
     */
    private float offsetX = 0.0f;

    /**
     * The offset to display the entity at in the y-axis. Effected by {@link #entityScale}.
     * An offset of 1.0f at a scale of 1.0f would shift the entity by 1/16 of a block.
     */
    private float offsetY = 0.0f;

    /**
     * The scale of the entity (1.0f means a block will fit the control).
     */
    private float entityScale = 1.0f;

    /**
     * The x-coordinate to look at. If null, the entity will look at the mouse.
     */
    @Nullable
    private Float lookX = null;

    /**
     * The y-coordinate to look at. If null, the entity will look at the mouse.
     */
    @Nullable
    private Float lookY = null;

    /**
     * Whether to scale the entity up/down based on its bounding box. Useful for rendering all entities at a
     * similar relative scale.
     */
    private boolean scaleToBoundingBox = true;

    /**
     * When true, the entity faces the camera and is vertically centered in the control.
     */
    private boolean faceCamera = false;

    /**
     * When true, the entity is anchored to the vertical center of the control instead of the bottom.
     */
    private boolean centerVertically = false;

    @Override
    public void onRender(@Nonnull GuiGraphics matrixStack, @Nonnull ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
    {
        double minDimension = Math.min(getWidth() * getScaleX(), getHeight() * getScaleY());
        double scale = getEntityScale() * minDimension / 16.0f;
        double centerX = (getPixelMidX() / getGuiScale()) + getOffsetX() * scale;
        double centerY = getPixelMidY() / getGuiScale();
        double bottomY = (getPixelY() + getPixelHeight()) / getGuiScale();
        double x = centerX;
        double y = (centerVertically ? centerY : bottomY) + getOffsetY() * scale;
        float lookX = getLookX() != null ? getLookX() : faceCamera ? (float) x : (float) (mouseX / getGuiScale());
        float lookY = getLookY() != null ? getLookY() : faceCamera ? (float) (y + 40.0) : (float) (mouseY / getGuiScale());

        GuiUtil.get().renderEntityOnScreen(matrixStack, entity, (int) x, (int) y, lookX, lookY, (float) scale, shouldScaleToBoundingBox(), faceCamera, centerVertically);
    }

    /**
     * When true, mouse clicks pass through to controls or slots below.
     */
    private boolean passThroughMouseClicks = true;

    @Override
    public void forwardHover(@Nonnull TryHoverEvent e)
    {
        if (!isHoverable() || getVisibility() == Visibility.COLLAPSED || isDragging())
        {
            return;
        }

        if (!contains(e.mouseX, e.mouseY))
        {
            return;
        }

        setIsHovered(true);
        // Pass hover to parent so EntryControl can show tooltips over the entity preview.
        if (!isInteractive())
        {
            return;
        }
        e.setIsHandled(true);
    }

    @Override
    public void forwardMouseClicked(@Nonnull MouseClickedEvent e)
    {
        if (passThroughMouseClicks)
        {
            return;
        }

        super.forwardMouseClicked(e);
    }

    @Override
    protected void onMouseClicked(@Nonnull MouseClickedEvent e)
    {
        if (!passThroughMouseClicks)
        {
            super.onMouseClicked(e);
        }
    }

    public void setPassThroughMouseClicks(boolean passThroughMouseClicks)
    {
        this.passThroughMouseClicks = passThroughMouseClicks;
    }

    public boolean passesThroughMouseClicks()
    {
        return passThroughMouseClicks;
    }

    /**
     * @return the entity to display.
     */
    @Nonnull
    public LivingEntity getEntity()
    {
        return entity;
    }

    /**
     * Sets the entity to display.
     */
    public void setEntity(@Nonnull LivingEntity entity)
    {
        this.entity = entity;
    }

    /**
     * Gets entity scale.
     */
    public float getEntityScale()
    {
        return entityScale;
    }

    /**
     * Sets entity scale.
     */
    public void setEntityScale(float entityScale)
    {
        this.entityScale = entityScale;
    }

    /**
     * Gets the entity offset in the x-axis.
     */
    public float getOffsetX()
    {
        return offsetX;
    }

    /**
     * Sets the entity offset in the x-axis.
     */
    public void setOffsetX(float offset)
    {
        this.offsetX = offset;
    }

    /**
     * Gets the entity offset in the y-axis.
     */
    public float getOffsetY()
    {
        return offsetY;
    }

    /**
     * Sets the entity offset in the y-axis.
     */
    public void setOffsetY(float offset)
    {
        this.offsetY = offset;
    }

    /**
     * @return whether to scale the entity relative to its bounding box.
     */
    public boolean shouldScaleToBoundingBox()
    {
        return scaleToBoundingBox;
    }

    /**
     * Sets whether to scale the entity relative to its bounding box.
     */
    public void setScaleToBoundingBox(boolean scaleToBoundingBox)
    {
        this.scaleToBoundingBox = scaleToBoundingBox;
    }

    /**
     * @return the x-coordinate to look at. If null, the entity will look at the mouse.
     */
    @Nullable
    public Float getLookX()
    {
        return lookX;
    }

    /**
     * Sets the x-coordinate to look at. If null, the entity will look at the mouse.
     */
    public void setLookX(@Nullable Float lookX)
    {
        this.lookX = lookX;
    }

    /**
     * @return the y-coordinate to look at. If null, the entity will look at the mouse.
     */
    @Nullable
    public Float getLookY()
    {
        return lookY;
    }

    /**
     * Sets the y-coordinate to look at. If null, the entity will look at the mouse.
     */
    public void setLookY(@Nullable Float lookY)
    {
        this.lookY = lookY;
    }

    public void setFaceCamera(boolean faceCamera)
    {
        this.faceCamera = faceCamera;
    }

    public boolean isFaceCamera()
    {
        return faceCamera;
    }

    public void setCenterVertically(boolean centerVertically)
    {
        this.centerVertically = centerVertically;
    }

    public boolean isCenterVertically()
    {
        return centerVertically;
    }
}
