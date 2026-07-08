package com.willr27.blocklings.hybrid;

import com.willr27.blocklings.platform.Services;

import java.util.Locale;

/**
 * Detects Mohist, Youer, Arclight and other NeoForge + Bukkit hybrid servers.
 */
public final class HybridServerDetector {

    private HybridServerDetector() {
    }

    public static boolean isHybridPresent() {
        return Services.PLATFORM.isHybridServer();
    }

    public static boolean isMohist() {
        return serverNameContains("mohist");
    }

    public static boolean isYouer() {
        return serverNameContains("youer");
    }

    public static boolean isArclight() {
        return serverNameContains("arclight");
    }

    public static boolean isPaperLike() {
        String name = Services.PLATFORM.hybridServerName().toLowerCase(Locale.ROOT);
        return name.contains("paper") || name.contains("spigot") || name.contains("purpur") || name.contains("bukkit");
    }

    private static boolean serverNameContains(String token) {
        return Services.PLATFORM.hybridServerName().toLowerCase(Locale.ROOT).contains(token);
    }
}
