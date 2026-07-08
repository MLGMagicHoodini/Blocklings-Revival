package com.willr27.blocklings.hybrid;

/**
 * Reflection-based Bukkit detection for hybrid servers (Mohist, Youer, Arclight).
 * Never throws — safe on pure NeoForge dedicated servers.
 */
public final class BukkitDetector {

    private BukkitDetector() {
    }

    public static boolean isBukkitPresent() {
        try {
            Class.forName("org.bukkit.Bukkit");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean hasPlugin(String pluginName) {
        if (!isBukkitPresent()) {
            return false;
        }
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object pluginManager = bukkitClass.getMethod("getPluginManager").invoke(null);
            Object plugin = pluginManager.getClass().getMethod("getPlugin", String.class).invoke(pluginManager, pluginName);
            if (plugin == null) {
                return false;
            }
            return (boolean) plugin.getClass().getMethod("isEnabled").invoke(plugin);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String serverSoftwareName() {
        if (!isBukkitPresent()) {
            return "";
        }
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object server = bukkit.getMethod("getServer").invoke(null);
            if (server == null) {
                return "";
            }
            Object name = server.getClass().getMethod("getName").invoke(server);
            return name == null ? "" : name.toString();
        } catch (Throwable ignored) {
            return "";
        }
    }
}
