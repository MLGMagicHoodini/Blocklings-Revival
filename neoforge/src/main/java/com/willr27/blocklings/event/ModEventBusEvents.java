package com.willr27.blocklings.event;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.BlocklingsEntityTypes;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingType;
import com.willr27.blocklings.entity.blockling.ability.BlocklingAbilityRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

import javax.annotation.Nonnull;

@EventBusSubscriber(modid = Blocklings.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents
{
    @SubscribeEvent
    public static void onCommonSetup(@Nonnull FMLCommonSetupEvent event)
    {
        BlocklingType.init();
        BlocklingAbilityRegistry.init();
    }

    @SubscribeEvent
    public static void addEntityAttributes(@Nonnull EntityAttributeCreationEvent event)
    {
        event.put(BlocklingsEntityTypes.BLOCKLING.get(), BlocklingEntity.createAttributes().build());
    }
}
