package com.willr27.blocklings.interop;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.platform.Services;

import java.lang.reflect.Field;

public final class ModProxies
{
    private ModProxies()
    {
    }

    public static void init()
    {
        activateProxy(TinkersConstructProxy.class);
    }

    private static void activateProxy(Class<? extends ModProxy> proxyClass)
    {
        Proxy proxyAnnotation = proxyClass.getAnnotation(Proxy.class);
        if (proxyAnnotation == null)
        {
            return;
        }

        if (!Services.PLATFORM.isModLoaded(proxyAnnotation.modid()))
        {
            return;
        }

        try
        {
            String activeClassName = proxyAnnotation.activeClassName();
            if (activeClassName.isEmpty())
            {
                activeClassName = proxyClass.getPackage().getName() + ".Active" + proxyClass.getSimpleName();
            }

            Field instanceField = proxyClass.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, Class.forName(activeClassName).asSubclass(proxyClass).getDeclaredConstructor().newInstance());
        }
        catch (Exception ex)
        {
            Blocklings.LOGGER.error("Failed to create mod proxy for \"{}\": {}", proxyClass.getName(), ex.toString());
        }
    }
}
