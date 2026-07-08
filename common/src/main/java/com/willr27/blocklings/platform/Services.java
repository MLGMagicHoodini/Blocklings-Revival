package com.willr27.blocklings.platform;

import com.willr27.blocklings.platform.services.IInventoryHelper;
import com.willr27.blocklings.platform.services.IMenuHelper;
import com.willr27.blocklings.platform.services.INetworkBridge;
import com.willr27.blocklings.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

public final class Services {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final INetworkBridge NETWORK = load(INetworkBridge.class);
    public static final IInventoryHelper INVENTORY = load(IInventoryHelper.class);
    public static final IMenuHelper MENUS = load(IMenuHelper.class);

    private Services() {
    }

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No service implementation for " + clazz.getName()));
    }
}
