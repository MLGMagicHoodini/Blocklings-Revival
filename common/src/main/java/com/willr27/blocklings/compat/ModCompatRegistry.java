package com.willr27.blocklings.compat;

import com.willr27.blocklings.hybrid.BukkitDetector;
import com.willr27.blocklings.platform.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ModCompatRegistry {

    public enum Kind {
        NEOFORGE_MOD,
        BUKKIT_PLUGIN,
        HYBRID_SERVER
    }

    public record Entry(String id, String displayName, Kind kind, String benefit) {
    }

    private static final List<Entry> KNOWN = List.of(
            new Entry("luckperms", "LuckPerms", Kind.NEOFORGE_MOD, "Permissions for blockling commands."),
            new Entry("LuckPerms", "LuckPerms (plugin)", Kind.BUKKIT_PLUGIN, "Permissions on hybrid servers."),
            new Entry("spark", "spark", Kind.NEOFORGE_MOD, "Performance profiling on dedicated servers."),
            new Entry("youer", "Youer", Kind.HYBRID_SERVER, "NeoForge + Paper/Bukkit hybrid."),
            new Entry("mohist", "Mohist", Kind.HYBRID_SERVER, "NeoForge + Bukkit hybrid."),
            new Entry("arclight", "Arclight", Kind.HYBRID_SERVER, "NeoForge + Bukkit hybrid."),
            new Entry("spigot", "Spigot / Paper", Kind.HYBRID_SERVER, "Bukkit plugin ecosystem.")
    );

    private ModCompatRegistry() {
    }

    public static List<Entry> knownCompat() {
        return KNOWN;
    }

    public static boolean isActive(Entry entry) {
        return switch (entry.kind()) {
            case NEOFORGE_MOD -> Services.PLATFORM.isModLoaded(entry.id());
            case BUKKIT_PLUGIN -> BukkitDetector.isBukkitPresent() && BukkitDetector.hasPlugin(entry.id());
            case HYBRID_SERVER -> isHybridActive(entry.id());
        };
    }

    private static boolean isHybridActive(String id) {
        if (!BukkitDetector.isBukkitPresent()) {
            return false;
        }
        String name = BukkitDetector.serverSoftwareName().toLowerCase(Locale.ROOT);
        if (name.isBlank()) {
            return "spigot".equalsIgnoreCase(id);
        }
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "youer" -> name.contains("youer");
            case "mohist" -> name.contains("mohist");
            case "arclight" -> name.contains("arclight");
            case "spigot" -> name.contains("spigot") || name.contains("paper") || name.contains("purpur") || name.contains("bukkit");
            default -> false;
        };
    }

    public static String activeCompatSummary() {
        List<String> active = new ArrayList<>();
        for (Entry entry : KNOWN) {
            if (isActive(entry)) {
                active.add(entry.displayName());
            }
        }
        return active.isEmpty() ? "standalone" : String.join(", ", active);
    }
}
