package com.willr27.blocklings.util;

import com.willr27.blocklings.loader.LoaderEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RegistryUtil {
    private RegistryUtil() {
    }

    /**
     * Full registry access including datapack registries (enchantments, etc.).
     * Never use {@link RegistryAccess#fromRegistryOfRegistries} alone for ItemStack codecs —
     * that omits {@code minecraft:enchantment} and crashes on enchanted tools.
     */
    @Nonnull
    public static RegistryAccess registryAccess() {
        RegistryAccess fromWorld = fromKnownWorld();
        if (fromWorld != null) {
            return fromWorld;
        }

        if (LoaderEnvironment.isClient()) {
            RegistryAccess fromClient = fromClientConnection();
            if (fromClient != null) {
                return fromClient;
            }
        }

        // Last resort — incomplete; callers that encode ItemStacks must pass a real world access.
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Nullable
    private static RegistryAccess fromKnownWorld() {
        Level world = BlockUtil.latestWorld;
        if (world != null) {
            try {
                return world.registryAccess();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    @Nullable
    private static RegistryAccess fromClientConnection() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) {
                return null;
            }
            if (minecraft.getConnection() != null) {
                return minecraft.getConnection().registryAccess();
            }
            if (minecraft.level != null) {
                return minecraft.level.registryAccess();
            }
        } catch (Throwable ignored) {
            // Dedicated server / no client class path edge cases
        }
        return null;
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
