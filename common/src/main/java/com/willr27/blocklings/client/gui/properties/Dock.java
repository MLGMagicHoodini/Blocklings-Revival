package com.willr27.blocklings.client.gui.properties;

import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

/**
 * Possible docking options.
 */
@OnlyIn(Dist.CLIENT)
public enum Dock
{
    LEFT,
    TOP,
    RIGHT,
    BOTTOM,
    FILL,
}
