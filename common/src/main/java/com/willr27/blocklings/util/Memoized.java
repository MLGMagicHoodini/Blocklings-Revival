package com.willr27.blocklings.util;

import javax.annotation.Nonnull;
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
            initialized = true;
        }
        return value;
    }
}
