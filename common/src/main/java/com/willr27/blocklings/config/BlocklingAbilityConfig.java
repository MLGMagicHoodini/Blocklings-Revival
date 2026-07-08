package com.willr27.blocklings.config;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * Configurable tuning for blockling type abilities. Populated by each loader at startup.
 */
public final class BlocklingAbilityConfig
{
    public Supplier<Boolean> enabled = () -> true;

    public FamilyConfig grass = new FamilyConfig();
    public FamilyConfig dirt = new FamilyConfig();
    public FamilyConfig wood = new FamilyConfig();
    public FamilyConfig stone = new FamilyConfig();
    public FamilyConfig iron = new FamilyConfig();
    public FamilyConfig gold = new FamilyConfig();
    public FamilyConfig diamond = new FamilyConfig();
    public FamilyConfig emerald = new FamilyConfig();
    public FamilyConfig lapis = new FamilyConfig();
    public FamilyConfig obsidian = new FamilyConfig();
    public FamilyConfig glowstone = new FamilyConfig();
    public FamilyConfig quartz = new FamilyConfig();
    public FamilyConfig netherite = new FamilyConfig();

    @Nonnull
    public FamilyConfig forKey(@Nonnull String key)
    {
        return switch (key)
        {
            case "grass" -> grass;
            case "dirt" -> dirt;
            case "wood", "oak_log" -> wood;
            case "stone" -> stone;
            case "iron" -> iron;
            case "gold" -> gold;
            case "diamond" -> diamond;
            case "emerald" -> emerald;
            case "lapis" -> lapis;
            case "obsidian" -> obsidian;
            case "glowstone" -> glowstone;
            case "quartz" -> quartz;
            case "netherite" -> netherite;
            default -> grass;
        };
    }

    public static final class FamilyConfig
    {
        public Supplier<Boolean> passiveEnabled = () -> true;
        public Supplier<Boolean> activeEnabled = () -> true;
        public Supplier<Double> passiveChance = () -> 0.0D;
        public Supplier<Integer> radius = () -> 4;
        public Supplier<Integer> activeCooldownSeconds = () -> 60;
        public Supplier<Integer> activeDurationSeconds = () -> 20;
    }
}
