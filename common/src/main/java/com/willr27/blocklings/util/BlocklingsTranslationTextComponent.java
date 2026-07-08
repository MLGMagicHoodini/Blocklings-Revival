package com.willr27.blocklings.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class BlocklingsTranslationTextComponent
{
    private BlocklingsTranslationTextComponent()
    {
    }

    public static MutableComponent create(String key, Object... objects)
    {
        return Component.translatable("blocklings." + key, objects);
    }

    public static Component of(String key)
    {
        return Component.translatable("blocklings." + key);
    }

    public static Component of(String key, Object... objects)
    {
        return Component.translatable("blocklings." + key, objects);
    }
}
