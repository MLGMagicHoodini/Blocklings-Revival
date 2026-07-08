package com.willr27.blocklings.util.event;

import com.willr27.blocklings.util.event.IEvent;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An event used when a value is changed.
 */
@OnlyIn(Dist.CLIENT)
public class ValueChangedEvent<T> implements IEvent
{
    /** The old value. */
    public final T oldValue;

    /** The new value. */
    public final T newValue;

    /**
     * @param oldValue the old value.
     * @param newValue the new value.
     */
    public ValueChangedEvent(@Nullable T oldValue, @Nullable T newValue)
    {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
