package com.willr27.blocklings.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ObjectUtil {
    private ObjectUtil() {
    }

    public static <T> T coalesce(@Nullable T obj1, @Nonnull T obj2) {
        return obj1 == null ? obj2 : obj1;
    }
}
