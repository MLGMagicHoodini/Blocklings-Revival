package com.willr27.blocklings.entity.blockling.ability;

import com.willr27.blocklings.entity.blockling.BlocklingType;

import javax.annotation.Nonnull;

/**
 * Groups {@link BlocklingType} entries that share the same ability profile.
 * Add a new enum constant + handler registration to introduce a new blockling family.
 */
public enum TypeFamily {
    GRASS,
    DIRT,
    WOOD,
    STONE,
    IRON,
    GOLD,
    DIAMOND,
    EMERALD,
    LAPIS,
    OBSIDIAN,
    GLOWSTONE,
    QUARTZ,
    NETHERITE;

    @Nonnull
    public static TypeFamily from(@Nonnull BlocklingType type)
    {
        if (type.key.endsWith("_log"))
        {
            return WOOD;
        }

        return switch (type.key)
        {
            case "grass" -> GRASS;
            case "dirt" -> DIRT;
            case "stone" -> STONE;
            case "iron" -> IRON;
            case "gold" -> GOLD;
            case "diamond" -> DIAMOND;
            case "emerald" -> EMERALD;
            case "lapis" -> LAPIS;
            case "obsidian" -> OBSIDIAN;
            case "glowstone" -> GLOWSTONE;
            case "quartz" -> QUARTZ;
            case "netherite" -> NETHERITE;
            default -> GRASS;
        };
    }
}
