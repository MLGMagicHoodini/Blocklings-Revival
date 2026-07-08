package com.willr27.blocklings.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * Loader-neutral reflection helper (replaces NeoForge {@code ObfuscationReflectionHelper}).
 */
public final class ReflectionHelper
{
    private ReflectionHelper()
    {
    }

    @Nullable
    public static <T, C> T getPrivateValue(@Nonnull Class<C> clazz, @Nonnull C instance, @Nonnull String fieldName)
    {
        try
        {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            T value = (T) field.get(instance);
            return value;
        }
        catch (ReflectiveOperationException ex)
        {
            throw new IllegalStateException("Failed to read " + clazz.getName() + "." + fieldName, ex);
        }
    }
}
