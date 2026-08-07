package com.willr27.blocklings.entity;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingSpawnDiagnostics;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Natural spawn registration for Fabric.
 * Mirrors NeoForge biome modifiers:
 * <ul>
 *   <li>overworld — weight 180</li>
 *   <li>plains / forest — extra weight 80 (stacked on overworld)</li>
 *   <li>nether — weight 80, end — weight 60</li>
 * </ul>
 * Some biomes (oceans, beaches, stony shore, etc.) have creatureSpawnProbability = 0, which means
 * adding a CREATURE spawn entry alone never produces pack spawning. We bump that probability so
 * blocklings (and other creatures) can actually attempt to spawn there.
 */
public final class EntityGeneration {
    private static final int SPAWN_WEIGHT = 24;
    private static final int BIOME_BONUS_WEIGHT = 10;
    private static final int NETHER_WEIGHT = 12;
    private static final int END_WEIGHT = 10;
    private static final int SPAWN_MIN = 1;
    private static final int SPAWN_MAX = 2;
    private static final float MIN_CREATURE_SPAWN_PROBABILITY = 0.1f;

    private EntityGeneration() {
    }

    public static void init() {
        BlocklingSpawnDiagnostics.enable(BlocklingsEntityTypes.BLOCKLING);

        SpawnPlacements.register(
                BlocklingsEntityTypes.BLOCKLING,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlocklingEntity::checkBlocklingSpawnRules);

        // Ensure creature packs can roll in biomes that disable creature spawning by default
        // (oceans, beaches, stony_shore, etc. have creature_spawn_probability = 0).
        BiomeModifications.create(ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "creature_spawn_density"))
                .add(ModificationPhase.ADDITIONS, BiomeSelectors.foundInOverworld(), context ->
                        context.getSpawnSettings().setCreatureSpawnProbability(MIN_CREATURE_SPAWN_PROBABILITY));

        addSpawns(BiomeTags.IS_OVERWORLD, SPAWN_WEIGHT);
        addSpawns(BiomeTags.IS_NETHER, NETHER_WEIGHT);
        addSpawns(BiomeTags.IS_END, END_WEIGHT);
        // Match NeoForge plains/forest biome_modifier extras (weight 80 stacked on overworld).
        // BiomeTags.IS_PLAINS does not exist in 1.21.1 — use the same #minecraft:is_plains tag as NeoForge JSON.
        addSpawns(TagKey.create(net.minecraft.core.registries.Registries.BIOME,
                ResourceLocation.withDefaultNamespace("is_plains")), BIOME_BONUS_WEIGHT);
        addSpawns(BiomeTags.IS_FOREST, BIOME_BONUS_WEIGHT);

        Blocklings.LOGGER.info("Blocklings natural spawns registered (overworld {}, plains/forest +{}, nether {}, end {}, groups {}-{})",
                SPAWN_WEIGHT, BIOME_BONUS_WEIGHT, NETHER_WEIGHT, END_WEIGHT, SPAWN_MIN, SPAWN_MAX);
        BlocklingSpawnDiagnostics.LOG.info("[SpawnDebug] registration complete — dump runs on server start");
    }

    private static void addSpawns(TagKey<Biome> tag, int weight) {
        BiomeModifications.addSpawn(
                BiomeSelectors.tag(tag),
                MobCategory.CREATURE,
                BlocklingsEntityTypes.BLOCKLING,
                weight,
                SPAWN_MIN,
                SPAWN_MAX);
    }
}
