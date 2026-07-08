package com.willr27.blocklings.util;

import com.willr27.blocklings.Blocklings;
import net.minecraft.resources.ResourceLocation;

public final class BlocklingsResourceLocation
{
    private BlocklingsResourceLocation()
    {
    }

    public static ResourceLocation of(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, path);
    }
}
