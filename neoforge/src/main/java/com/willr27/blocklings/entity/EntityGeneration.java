package com.willr27.blocklings.entity;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = Blocklings.MODID)
public final class EntityGeneration
{
    private EntityGeneration()
    {
    }

    public static void init()
    {
        Blocklings.LOGGER.debug("EntityGeneration initialized (spawn placements on mod bus)");
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event)
    {
        event.register(
                BlocklingsEntityTypes.BLOCKLING.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlocklingEntity::checkBlocklingSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }
}
