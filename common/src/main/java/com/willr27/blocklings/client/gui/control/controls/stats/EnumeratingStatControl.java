package com.willr27.blocklings.client.gui.control.controls.stats;

import net.minecraft.client.gui.GuiGraphics;
import com.willr27.blocklings.client.gui.control.controls.EnumeratingControl;
import com.willr27.blocklings.client.gui.util.GuiUtil;
import com.willr27.blocklings.util.BlocklingsTranslationTextComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An enumerating control specifically for stats.
 */
@OnlyIn(Dist.CLIENT)
public class EnumeratingStatControl extends EnumeratingControl<StatControl>
{
    /**
     * The name of the enumerating control to display in the tooltip.
     */
    @Nonnull
    private final Component name;

    /**
     * @param name the name of the enumerating control to display in the tooltip.
     */
    public EnumeratingStatControl(@Nonnull Component name)
    {
        super();
        this.name = name;
    }

    @Override
    public void onRenderTooltip(@Nonnull GuiGraphics matrixStack, double mouseX, double mouseY, float partialTicks)
    {
        renderTooltip(matrixStack, mouseX, mouseY, getPixelScaleX(), getPixelScaleY(), prependNameToTooltip(combineTooltips()).stream().map(t -> t.getVisualOrderText()).collect(Collectors.toList()));
    }

    private List<Component> prependNameToTooltip(@Nonnull List<Component> tooltip)
    {
//        tooltip.add(0, Component.literal("").getVisualOrderText());
        tooltip.add(0, name);

        return tooltip;
    }

    @Nonnull
    private List<Component> combineTooltips()
    {
        List<Component> tooltip = new ArrayList<>();

        for (int i = 0; i < displayConditions.size(); i++)
        {
            if (!displayConditions.get(i).get())
            {
                continue;
            }

            List<Component> subTooltip = controls.get(i).tooltipSupplier.get();

            if (i == getIndexOfCurrentChild())
            {
                subTooltip.set(0, Component.literal(subTooltip.get(0).getString().substring(0, 2) + ChatFormatting.ITALIC + subTooltip.get(0).getString().substring(2)));
            }

            if (GuiUtil.get().isCrouchKeyDown())
            {
                tooltip.addAll(subTooltip);
            }
            else
            {
                tooltip.add(subTooltip.get(0));
            }
        }

        if (!GuiUtil.get().isCrouchKeyDown())
        {
            tooltip.add(Component.literal(ChatFormatting.DARK_GRAY + "" + ChatFormatting.ITALIC + BlocklingsTranslationTextComponent.create("gui.more_info", Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage().getString()).getString()));
        }

        return tooltip;
    }
}

