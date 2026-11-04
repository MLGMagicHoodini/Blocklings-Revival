package com.willr27.blocklings.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import javax.annotation.Nonnull;

/**
 * Configurable tuning for blockling type abilities. Every passive has at least one config knob.
 */
public final class NeoForgeBlocklingAbilityConfig
{
    public final ModConfigSpec.BooleanValue enabled;

    public final FamilyConfig grass;
    public final FamilyConfig dirt;
    public final FamilyConfig wood;
    public final FamilyConfig stone;
    public final FamilyConfig iron;
    public final FamilyConfig gold;
    public final FamilyConfig diamond;
    public final FamilyConfig emerald;
    public final FamilyConfig lapis;
    public final FamilyConfig obsidian;
    public final FamilyConfig glowstone;
    public final FamilyConfig quartz;
    public final FamilyConfig netherite;
    public final FamilyConfig pumpkin;

    public NeoForgeBlocklingAbilityConfig(@Nonnull ModConfigSpec.Builder builder)
    {
        builder.push("Abilities");

        enabled = builder
                .comment("Master toggle for the modular type ability system.")
                .define("enabled", true);

        grass = new FamilyConfig(builder, "grass", 0.33D, 3, 60);
        dirt = new FamilyConfig(builder, "dirt", 0.33D, 3, 60);
        wood = new FamilyConfig(builder, "wood", 0.15D, 4, 90);
        stone = new FamilyConfig(builder, "stone", 0.0D, 8, 120);
        iron = new FamilyConfig(builder, "iron", 0.0D, 4, 60);
        gold = new FamilyConfig(builder, "gold", 0.10D, 8, 90);
        diamond = new FamilyConfig(builder, "diamond", 0.0D, 8, 120);
        emerald = new FamilyConfig(builder, "emerald", 0.05D, 8, 90);
        lapis = new FamilyConfig(builder, "lapis", 0.0D, 8, 90);
        obsidian = new FamilyConfig(builder, "obsidian", 0.0D, 6, 120);
        glowstone = new FamilyConfig(builder, "glowstone", 0.0D, 8, 60);
        quartz = new FamilyConfig(builder, "quartz", 0.0D, 6, 60);
        netherite = new FamilyConfig(builder, "netherite", 0.0D, 8, 180);
        pumpkin = new FamilyConfig(builder, "pumpkin", 0.35D, 4, 60);

        builder.pop();
    }

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
            case "pumpkin" -> pumpkin;
            default -> grass;
        };
    }

    public static final class FamilyConfig
    {
        public final ModConfigSpec.BooleanValue passiveEnabled;
        public final ModConfigSpec.BooleanValue activeEnabled;
        public final ModConfigSpec.DoubleValue passiveChance;
        public final ModConfigSpec.IntValue radius;
        public final ModConfigSpec.IntValue activeCooldownSeconds;
        public final ModConfigSpec.IntValue activeDurationSeconds;

        private FamilyConfig(@Nonnull ModConfigSpec.Builder builder, @Nonnull String key,
                             double defaultChance, int defaultRadius, int defaultCooldown)
        {
            builder.push(key);

            passiveEnabled = builder.define("passiveEnabled", true);
            activeEnabled = builder.define("activeEnabled", true);
            passiveChance = builder.defineInRange("passiveChance", defaultChance, 0.0D, 1.0D);
            radius = builder.defineInRange("radius", defaultRadius, 1, 32);
            activeCooldownSeconds = builder.defineInRange("activeCooldownSeconds", defaultCooldown, 5, 600);
            activeDurationSeconds = builder.defineInRange("activeDurationSeconds", 20, 5, 120);

            builder.pop();
        }
    }
}
