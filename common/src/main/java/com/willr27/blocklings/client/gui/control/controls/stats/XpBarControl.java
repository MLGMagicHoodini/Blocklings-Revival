package com.willr27.blocklings.client.gui.control.controls.stats;

import net.minecraft.client.gui.GuiGraphics;
import com.willr27.blocklings.client.gui.control.Control;
import com.willr27.blocklings.client.gui.control.controls.TextBlockControl;
import com.willr27.blocklings.client.gui.control.controls.TexturedControl;
import com.willr27.blocklings.client.gui.control.controls.panels.StackPanel;
import com.willr27.blocklings.client.gui.properties.Direction;
import com.willr27.blocklings.client.gui.texture.Texture;
import com.willr27.blocklings.client.gui.texture.Textures;
import com.willr27.blocklings.client.gui.util.ScissorStack;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.attribute.Attribute;
import com.willr27.blocklings.entity.blockling.attribute.BlocklingAttributes;
import com.willr27.blocklings.util.BlocklingsTranslationTextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a blockling's xp bar.
 */
@OnlyIn(Dist.CLIENT)
public class XpBarControl extends Control
{
    /**
     * The blockling.
     */
    @Nonnull
    private final BlocklingEntity blockling;

    /**
     * The level.
     */
    @Nonnull
    private final BlocklingAttributes.Level level;

    /**
     * @param blockling the blockling.
     * @param level     the level.
     */
    public XpBarControl(@Nonnull BlocklingEntity blockling, @Nonnull BlocklingAttributes.Level level)
    {
        super();
        this.blockling = blockling;
        this.level = level;

        setFitWidthToContent(true);
        setFitHeightToContent(true);

        StackPanel stackPanel = new StackPanel();
        stackPanel.setParent(this);
        stackPanel.setFitWidthToContent(true);
        stackPanel.setFitHeightToContent(true);
        stackPanel.setDirection(Direction.LEFT_TO_RIGHT);
        stackPanel.setSpacing(5);
        stackPanel.setInteractive(false);

        LevelIconControl leftIconControl = new LevelIconControl(true);
        leftIconControl.setParent(stackPanel);

        Control xpBarControl = new Control();
        xpBarControl.setParent(stackPanel);
        xpBarControl.setFitWidthToContent(true);
        xpBarControl.setFitHeightToContent(true);
        xpBarControl.setVerticalAlignment(0.5);

        TexturedControl xpBarBackgroundControl = new TexturedControl(level.getXpBarBackgroundTexture());
        xpBarBackgroundControl.setParent(xpBarControl);

        TexturedControl xpBarForegroundControl = new TexturedControl(level.getXpBarForegroundTexture())
        {
            @Override
            public void onTick()
            {
                Texture full = level.getXpBarForegroundTexture();
                int filledWidth = Math.max(0, (int) Math.round(full.width * getXpPercentage()));
                setBackgroundTexture(full.width(filledWidth));
                setWidth(filledWidth);
            }
        };
        xpBarForegroundControl.setParent(xpBarControl);
        // Required so setWidth(xp%) is not overwritten by fit-to-full-texture (same as 1.18 / HealthBarControl).
        xpBarForegroundControl.setFitWidthToContent(false);
        xpBarForegroundControl.onTick();

        LevelIconControl rightIconControl = new LevelIconControl(false);
        rightIconControl.setParent(stackPanel);

        TextBlockControl levelTextControl = new TextBlockControl()
        {
            @Override
            public void onTick()
            {
                setText(String.valueOf(blockling.getStats().getLevelAttribute(level).getValue()));
            }
        };
        levelTextControl.setParent(this);
        levelTextControl.setFitWidthToContent(true);
        levelTextControl.setFitHeightToContent(true);
        levelTextControl.useDescenderlessLineHeight();
        levelTextControl.setTextColour(level.getLevelColour().argb());
        levelTextControl.setHorizontalAlignment(0.5);
        levelTextControl.setVerticalAlignment(0.5);
        levelTextControl.setInteractive(false);
        levelTextControl.onTick();
    }

    @Override
    public void onRenderTooltip(@Nonnull GuiGraphics matrixStack, double mouseX, double mouseY, float partialTicks)
    {
        Attribute<Integer> level = blockling.getStats().getLevelAttribute(this.level);

        List<FormattedCharSequence> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(ChatFormatting.GOLD + blockling.getStats().getLevelAttribute(this.level).displayStringNameSupplier.get()).getVisualOrderText());
        tooltip.add(Component.translatable("blocklings.gui.current_level",
                Component.literal(String.valueOf(level.getValue())).withStyle(ChatFormatting.WHITE)).getVisualOrderText());
        tooltip.add(Component.translatable("blocklings.gui.xp_required",
                Component.literal(String.valueOf(blockling.getStats().getLevelXpAttribute(this.level).getValue())).withStyle(ChatFormatting.WHITE),
                BlocklingAttributes.getXpForLevel(level.getValue())).getVisualOrderText());

        renderTooltip(matrixStack, mouseX, mouseY, getPixelScaleX(), getPixelScaleY(), tooltip);
    }

    /**
     * @return the percentage of the way to the next level.
     */
    private float getXpPercentage()
    {
        return Math.min(1.0f, (float) blockling.getStats().getLevelXpAttribute(level).getValue() / BlocklingAttributes.getXpForLevel(blockling.getStats().getLevelAttribute(level).getValue()));
    }

    /**
     * Displays an icon for a level.
     */
    private class LevelIconControl extends Control
    {
        /**
         * Whether the icon represents the current or next level.
         */
        private final boolean current;

        /**
         * The icons texture.
         */
        @Nonnull
        private final Texture iconsTexture = level.getLevelIconsTexture();

        /**
         * @param current whether the icon represents the current or next level.
         */
        public LevelIconControl(boolean current)
        {
            super();
            this.current = current;

            setWidth(Textures.Stats.LevelIconsTexture.ICON_SIZE);
            setHeight(Textures.Stats.LevelIconsTexture.ICON_SIZE);
        }

        @Override
        public void onRender(@Nonnull GuiGraphics matrixStack, @Nonnull ScissorStack scissorStack, double mouseX, double mouseY, float partialTicks)
        {
            int iconToRender = (int) (((float) (blockling.getStats().getLevelAttribute(level).getValue() + (current ? 0 : 1)) / BlocklingAttributes.Level.MAX) * (Textures.Stats.LevelIconsTexture.NUMBER_OF_ICONS - 1));

            int iconSize = Textures.Stats.LevelIconsTexture.ICON_SIZE;
            renderTextureAsBackground(matrixStack, iconsTexture.dx(iconToRender * iconSize).width(iconSize));
        }
    }
}
