package com.willr27.blocklings.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Loader-neutral reflection helper (replaces NeoForge {@code ObfuscationReflectionHelper}).
 * Tries multiple field names so NeoForge (Mojang) and Fabric (intermediary) both work.
 */
public final class ReflectionHelper
{
    private ReflectionHelper()
    {
    }

    @Nullable
    public static <T, C> T getPrivateValue(@Nonnull Class<C> clazz, @Nonnull C instance, @Nonnull String fieldName)
    {
        return getPrivateValue(clazz, instance, new String[] { fieldName });
    }

    @Nullable
    public static <T, C> T getPrivateValue(@Nonnull Class<C> clazz, @Nonnull C instance, @Nonnull String... fieldNames)
    {
        ReflectiveOperationException last = null;

        for (String fieldName : fieldNames)
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
                last = ex;
            }
        }

        throw new IllegalStateException(
                "Failed to read " + clazz.getName() + "." + Arrays.toString(fieldNames), last);
    }

    /**
     * Reads the {@code index}-th declared field of {@code fieldType} from {@code clazz}.
     * <p>
     * Name-independent — use when the obfuscated field name differs between loaders/versions
     * (e.g. {@code Mob} has two {@code GoalSelector} fields: goalSelector [0] then targetSelector [1]).
     *
     * @return the field value, or {@code null} if not found / inaccessible.
     */
    @Nullable
    public static <T, C> T getFieldByType(@Nonnull Class<C> clazz, @Nonnull C instance,
                                          @Nonnull Class<T> fieldType, int index)
    {
        int seen = 0;
        for (Field field : clazz.getDeclaredFields())
        {
            if (!fieldType.isAssignableFrom(field.getType()))
            {
                continue;
            }

            if (seen == index)
            {
                try
                {
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    T value = (T) field.get(instance);
                    return value;
                }
                catch (ReflectiveOperationException ex)
                {
                    return null;
                }
            }

            seen++;
        }

        return null;
    }
}
