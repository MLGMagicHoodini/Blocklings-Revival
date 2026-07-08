package com.willr27.blocklings.client.gui.properties;

import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;

/**
 * Represents a size.
 */
@OnlyIn(Dist.CLIENT)
public class Size
{
    /**
     * The width.
     */
    public double width;

    /**
     * The height.
     */
    public double height;

    /**
     * @param width  the width.
     * @param height the height.
     */
    public Size(double width, double height)
    {
        this.width = width;
        this.height = height;
    }

    /**
     * Copy constructor.
     *
     * @param size the size.
     */
    public Size(@Nonnull Size size)
    {
        this(size.width, size.height);
    }
}
