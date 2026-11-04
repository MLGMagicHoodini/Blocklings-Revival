package com.willr27.blocklings.entity;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingSpawnDiagnostics;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Spawn placement for Fabric.
 * Blocklings are not added to the creature biome lists. Those lists share one
 * small budget with every other animal and with dinosaur mods, so a high
 * blockling weight would replace them. Wild blocklings are placed by
 * {@link com.willr27.blocklings.entity.blockling.BlocklingNaturalSpawner}
 * instead, with a separate quota per player.
 */
public final class EntityGeneration {
    private EntityGeneration() {
    }

    public static void init() {
        BlocklingSpawnDiagnostics.enable(BlocklingsEntityTypes.BLOCKLING);

        SpawnPlacements.register(
                BlocklingsEntityTypes.BLOCKLING,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlocklingEntity::checkBlocklingSpawnRules);

        Blocklings.LOGGER.info("Blocklings natural spawns use a per-player quota and do not share the creature cap");
        BlocklingSpawnDiagnostics.LOG.info("[SpawnDebug] registration complete — dump runs on server start");
    }
}
