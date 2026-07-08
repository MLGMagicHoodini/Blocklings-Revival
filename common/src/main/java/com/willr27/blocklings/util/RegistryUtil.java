package com.willr27.blocklings.util;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;

public final class RegistryUtil {
    private RegistryUtil() {
    }

    @Nonnull
    public static RegistryAccess registryAccess() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Nonnull
    public static ResourceLocation blockId(@Nonnull Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    @Nonnull
    public static ResourceLocation itemId(@Nonnull Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    @Nonnull
    public static ResourceLocation entityTypeId(@Nonnull EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }

    public static boolean biomeMatches(@Nonnull ResourceKey<net.minecraft.world.level.biome.Biome> holderKey,
                                       @Nonnull ResourceKey<net.minecraft.world.level.biome.Biome> expected) {
        return holderKey.equals(expected);
    }
}
