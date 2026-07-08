package com.willr27.blocklings.loader;

import javax.annotation.Nonnull;
import java.nio.file.Path;

/**
 * Loader-populated environment (replaces {@code FMLEnvironment} / Fabric env checks in common code).
 */
public final class LoaderEnvironment
{
    private static Dist dist = Dist.CLIENT;
    private static Path configDir = Path.of("config");

    private LoaderEnvironment()
    {
    }

    public static void init(@Nonnull Dist loaderDist, @Nonnull Path loaderConfigDir)
    {
        dist = loaderDist;
        configDir = loaderConfigDir;
    }

    @Nonnull
    public static Dist getDist()
    {
        return dist;
    }

    public static boolean isClient()
    {
        return dist == Dist.CLIENT;
    }

    @Nonnull
    public static Path getConfigDir()
    {
        return configDir;
    }
}
