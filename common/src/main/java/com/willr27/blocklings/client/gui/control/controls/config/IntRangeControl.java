package com.willr27.blocklings.client.gui.control.controls.config;

import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;

/**
 * Displays and edits an int range.
 */
@OnlyIn(Dist.CLIENT)
public class IntRangeControl extends RangeControl<Integer>
{
    /**
     * @param min           the minimum value.
     * @param max           the maximum value.
     * @param startingValue the starting value.
     */
    public IntRangeControl(@Nonnull Integer min, @Nonnull Integer max, @Nonnull Integer startingValue)
    {
        super(min, max, startingValue);
    }

    @Nonnull
    @Override
    protected Integer parseValue(@Nonnull String text)
    {
        return Integer.parseInt(text);
    }

    @Nonnull
    @Override
    protected Integer calculateValue(double percentage)
    {
        return Math.toIntExact(Math.round(min + ((max - min) * percentage)));
    }
}
