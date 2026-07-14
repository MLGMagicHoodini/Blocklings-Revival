package com.willr27.blocklings.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Loader-neutral lazy value (replaces NeoForge {@code Lazy}).
 */
public final class Memoized<T> implements Supplier<T>
{
    private final Supplier<T> factory;
    private T value;
    private boolean initialized;

    private Memoized(@Nonnull Supplier<T> factory)
    {
        this.factory = factory;
    }

    @Nonnull
    public static <T> Memoized<T> of(@Nonnull Supplier<T> factory)
    {
        return new Memoized<>(factory);
    }

    @Override
    public synchronized T get()
    {
        if (!initialized)
        {
            value = factory.get();
            // Never permanently cache empty collections from pre-tag / pre-world init
            // (ores list, attack targets, etc. — otherwise mining whitelist stays blank forever).
            if (value instanceof Map<?, ?> map && map.isEmpty())
            {
                return value;
            }
            if (value instanceof java.util.Collection<?> collection && collection.isEmpty())
            {
                return value;
            }
            initialized = true;
        }
        return value;
    }

    /** Clears the cached value so the next {@link #get()} rebuilds it. */
    public synchronized void reset()
    {
        initialized = false;
        value = null;
    }

    @Nullable
    public synchronized T peek()
    {
        return initialized ? value : null;
    }
}
