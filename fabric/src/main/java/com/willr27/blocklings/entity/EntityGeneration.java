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
 * Weight matches the legacy Forge port (100) so blocklings appear roughly as often as common animals.
 * <p>
 * Some biomes (oceans, beaches, stony shore, etc.) have creatureSpawnProbability = 0, which means
 * adding a CREATURE spawn entry alone never produces pack spawning. We bump that probability so
 * blocklings (and other creatures) can actually attempt to spawn there.
 */
public final class EntityGeneration {
    private static final int SPAWN_WEIGHT = 100;
    private static final int SPAWN_MIN = 1;
    private static final int SPAWN_MAX = 3;
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

        addSpawns(BiomeTags.IS_OVERWORLD);
        addSpawns(BiomeTags.IS_NETHER);
        addSpawns(BiomeTags.IS_END);

        Blocklings.LOGGER.info("Blocklings natural spawns registered (weight {}, groups {}-{})", SPAWN_WEIGHT, SPAWN_MIN, SPAWN_MAX);
        BlocklingSpawnDiagnostics.LOG.info("[SpawnDebug] registration complete — dump runs on server start");
    }

    private static void addSpawns(TagKey<Biome> tag) {
        BiomeModifications.addSpawn(
                BiomeSelectors.tag(tag),
                MobCategory.CREATURE,
                BlocklingsEntityTypes.BLOCKLING,
                SPAWN_WEIGHT,
                SPAWN_MIN,
                SPAWN_MAX);
    }
}
