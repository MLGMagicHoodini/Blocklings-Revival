package com.willr27.blocklings.entity;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nonnull;

public final class BlocklingsEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Blocklings.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BlocklingEntity>> BLOCKLING =
            ENTITY_TYPES.register("blockling", () -> EntityType.Builder
                    .of(BlocklingEntity::new, MobCategory.CREATURE)
                    .sized(1.0f, 1.0f)
                    .build(ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "blockling").toString()));

    private BlocklingsEntityTypes() {
    }

    public static void register(@Nonnull IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
