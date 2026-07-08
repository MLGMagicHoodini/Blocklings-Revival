package com.willr27.blocklings.entity.blockling.task.config.range;

import com.willr27.blocklings.client.gui.control.BaseControl;
import com.willr27.blocklings.client.gui.control.controls.config.FloatRangeControl;
import com.willr27.blocklings.entity.blockling.goal.BlocklingGoal;
import com.willr27.blocklings.util.Version;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;

public class FloatRangeProperty extends RangeProperty<Float> {
    public FloatRangeProperty(@Nonnull String id, @Nonnull BlocklingGoal goal, @Nonnull Component name, @Nonnull Component desc, float min, float max, float startingValue) {
        super(id, goal, name, desc, min, max, startingValue);
    }

    @Override
    public CompoundTag writeToNBT(@Nonnull CompoundTag propertyTag) {
        propertyTag.putFloat("value", value);
        return super.writeToNBT(propertyTag);
    }

    @Override
    public void readFromNBT(@Nonnull CompoundTag propertyTag, @Nonnull Version tagVersion) {
        value = propertyTag.getFloat("value");
        super.readFromNBT(propertyTag, tagVersion);
    }

    @Override
    public void encode(@Nonnull FriendlyByteBuf buf) {
        super.encode(buf);
        buf.writeFloat(min);
        buf.writeFloat(max);
        buf.writeFloat(value);
    }

    @Override
    public void decode(@Nonnull FriendlyByteBuf buf) {
        super.decode(buf);
        min = buf.readFloat();
        max = buf.readFloat();
        value = buf.readFloat();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    @Nonnull
    public BaseControl createControl() {
        return new FloatRangeControl(min, max, value);
    }
}
