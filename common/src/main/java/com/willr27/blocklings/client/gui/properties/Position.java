package com.willr27.blocklings.client.gui.properties;

import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;

/**
 * Represents a position.
 */
@OnlyIn(Dist.CLIENT)
public class Position
{
    /**
     * The x position.
     */
    public double x;

    /**
     * The y position.
     */
    public double y;

    /**
     * @param x the x position.
     * @param y the y position.
     */
    public Position(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    /**
     * Copy constructor.
     *
     * @param position the position.
     */
    public Position(@Nonnull Position position)
    {
        this(position.x, position.y);
    }
}
