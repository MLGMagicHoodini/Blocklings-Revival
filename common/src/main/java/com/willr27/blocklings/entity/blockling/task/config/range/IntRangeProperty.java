package com.willr27.blocklings.entity.blockling.task.config.range;

import com.willr27.blocklings.client.gui.control.BaseControl;
import com.willr27.blocklings.client.gui.control.controls.config.IntRangeControl;
import com.willr27.blocklings.client.gui.util.GuiUtil;
import com.willr27.blocklings.entity.blockling.goal.BlocklingGoal;
import com.willr27.blocklings.util.Version;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.ChatFormatting;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Configures an int range property (e.g. follow start/stop range).
 */
public class IntRangeProperty extends RangeProperty<Integer>
{
    public IntRangeProperty(@Nonnull String id, @Nonnull BlocklingGoal goal, @Nonnull Component name, @Nonnull Component desc, int min, int max, int startingValue)
    {
        super(id, goal, name, desc, min, max, startingValue);
    }

    @Override
    public CompoundTag writeToNBT(@Nonnull CompoundTag propertyTag)
    {
        propertyTag.putInt("value", value);
        return super.writeToNBT(propertyTag);
    }

    @Override
    public void readFromNBT(@Nonnull CompoundTag propertyTag, @Nonnull Version tagVersion)
    {
        value = propertyTag.getInt("value");
        super.readFromNBT(propertyTag, tagVersion);
    }

    @Override
    public void encode(@Nonnull FriendlyByteBuf buf)
    {
        super.encode(buf);
        buf.writeInt(min);
        buf.writeInt(max);
        buf.writeInt(value);
    }

    @Override
    public void decode(@Nonnull FriendlyByteBuf buf)
    {
        super.decode(buf);
        min = buf.readInt();
        max = buf.readInt();
        value = buf.readInt();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    @Nonnull
    public BaseControl createControl()
    {
        return new IntRangeControl(min, max, value)
        {
            @Override
            public void onRenderTooltip(@Nonnull GuiGraphics matrixStack, double mouseX, double mouseY, float partialTicks)
            {
                if (!grabberControl.isPressed())
                {
                    List<FormattedCharSequence> tooltip = new ArrayList<>(
                            GuiUtil.get().split(desc.copy().withStyle(ChatFormatting.GRAY), 200));
                    tooltip.add(0, name.copy().withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)).getVisualOrderText());
                    renderTooltip(matrixStack, mouseX, mouseY, tooltip);
                }
            }

            @Override
            public void setValue(@Nonnull Integer value, boolean updateGrabberPosition, boolean postEvent)
            {
                super.setValue(value, updateGrabberPosition, postEvent);

                if (postEvent)
                {
                    IntRangeProperty.this.setValue(getValue(), true);
                }
            }
        };
    }
}
