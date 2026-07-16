package com.willr27.blocklings.config;

import com.willr27.blocklings.entity.blockling.BlocklingType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Spawn tuning: global caps and per-type weights / extra biomes.
 */
public final class BlocklingSpawnConfig
{
    public enum BiomeMode
    {
        AND,
        OVERRIDE;

        @Nonnull
        public static BiomeMode parse(@Nonnull String raw)
        {
            try
            {
                return BiomeMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException ignored)
            {
                return AND;
            }
        }
    }

    public static final class TypeConfig
    {
        public Supplier<Boolean> enabled = () -> true;
        public Supplier<Integer> spawnWeight = () -> 100;
        public Supplier<List<? extends String>> extraBiomes = ArrayList::new;
        public Supplier<String> biomeMode = () -> "AND";

        public boolean isEnabled()
        {
            return Boolean.TRUE.equals(enabled.get());
        }

        public int weight()
        {
            return Math.max(0, spawnWeight.get());
        }

        @Nonnull
        public List<? extends String> biomes()
        {
            List<? extends String> list = extraBiomes.get();
            return list != null ? list : Collections.emptyList();
        }

        @Nonnull
        public BiomeMode mode()
        {
            return BiomeMode.parse(biomeMode.get() != null ? biomeMode.get() : "AND");
        }
    }

    public Supplier<Boolean> enabled = () -> true;
    /** Max untamed blocklings in {@link #nearbyRadius} before natural spawn stops. */
    public Supplier<Integer> nearbyCap = () -> 6;
    public Supplier<Double> nearbyRadius = () -> 40.0D;
    /** If true, skip a type when another wild of that type is already in radius (makes spawns feel sparse). */
    public Supplier<Boolean> preventDuplicateNearbyType = () -> false;

    /** One-time pack of wild blocklings near a player on first join (not on reconnect). */
    public Supplier<Boolean> starterSpawnEnabled = () -> true;
    public Supplier<Integer> starterSpawnCount = () -> 5;
    public Supplier<Integer> starterSpawnRadius = () -> 12;
    public Supplier<Integer> starterSpawnDelayTicks = () -> 40;

    @Nonnull
    private final Map<String, TypeConfig> types = new LinkedHashMap<>();

    public boolean isEnabled()
    {
        return Boolean.TRUE.equals(enabled.get());
    }

    public int cap()
    {
        return Math.max(0, nearbyCap.get());
    }

    public double radius()
    {
        return Math.max(1.0D, nearbyRadius.get());
    }

    public boolean preventDuplicates()
    {
        return Boolean.TRUE.equals(preventDuplicateNearbyType.get());
    }

    public boolean starterEnabled()
    {
        return Boolean.TRUE.equals(starterSpawnEnabled.get());
    }

    public int starterCount()
    {
        return Math.max(0, starterSpawnCount.get());
    }

    public int starterRadius()
    {
        return Math.max(2, starterSpawnRadius.get());
    }

    public int starterDelayTicks()
    {
        return Math.max(1, starterSpawnDelayTicks.get());
    }

    @Nonnull
    public Map<String, TypeConfig> types()
    {
        ensureTypeEntries();
        return types;
    }

    @Nonnull
    public TypeConfig forType(@Nonnull BlocklingType type)
    {
        return forKey(type.key, type.spawnRateReduction);
    }

    @Nonnull
    public TypeConfig forKey(@Nonnull String key)
    {
        return forKey(key, 1);
    }

    @Nonnull
    private TypeConfig forKey(@Nonnull String key, int spawnRateReduction)
    {
        ensureTypeEntries();
        return types.computeIfAbsent(key, k -> createDefaultTypeConfig(spawnRateReduction));
    }

    private void ensureTypeEntries()
    {
        if (!types.isEmpty() || BlocklingType.TYPES.isEmpty())
        {
            return;
        }

        for (BlocklingType type : BlocklingType.TYPES)
        {
            types.putIfAbsent(type.key, createDefaultTypeConfig(type.spawnRateReduction));
        }
    }

    @Nonnull
    public static TypeConfig createDefaultTypeConfig(int spawnRateReduction)
    {
        TypeConfig cfg = new TypeConfig();
        int reduction = Math.max(1, spawnRateReduction);
        int defaultWeight = Math.max(1, 100 / reduction);
        cfg.spawnWeight = () -> defaultWeight;
        return cfg;
    }

    /**
     * Default spawn weight derived from the type's legacy spawnRateReduction.
     */
    public static int defaultWeightFor(@Nonnull BlocklingType type)
    {
        return Math.max(1, 100 / Math.max(1, type.spawnRateReduction));
    }

    /**
     * @return true if the biome matches any entry in {@code entries} (biome id or {@code #tag}).
     */
    public static boolean matchesBiomeList(@Nonnull Holder<Biome> biome, @Nonnull List<? extends String> entries)
    {
        if (entries.isEmpty())
        {
            return true;
        }

        for (String raw : entries)
        {
            if (raw == null || raw.isBlank())
            {
                continue;
            }

            String entry = raw.trim();
            if (entry.startsWith("#"))
            {
                ResourceLocation tagId = ResourceLocation.tryParse(entry.substring(1));
                if (tagId != null && biome.is(TagKey.create(Registries.BIOME, tagId)))
                {
                    return true;
                }
            }
            else
            {
                ResourceLocation biomeId = ResourceLocation.tryParse(entry);
                if (biomeId != null && biome.is(biomeId))
                {
                    return true;
                }
            }
        }

        return false;
    }
}
