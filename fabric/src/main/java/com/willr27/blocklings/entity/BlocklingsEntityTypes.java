package com.willr27.blocklings.entity;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class BlocklingsEntityTypes {
    public static final EntityType<BlocklingEntity> BLOCKLING = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "blockling"),
            EntityType.Builder.of(BlocklingEntity::new, MobCategory.CREATURE)
                    .sized(1.0f, 1.0f)
                    .build(ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "blockling").toString()));

    private BlocklingsEntityTypes() {
    }

    public static void register() {
        Blocklings.LOGGER.debug("Registered blockling entity type");
    }
}
