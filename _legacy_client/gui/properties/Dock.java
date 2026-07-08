package com.willr27.blocklings.client.gui.properties;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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
