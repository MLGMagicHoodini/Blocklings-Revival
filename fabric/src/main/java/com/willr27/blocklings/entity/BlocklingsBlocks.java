package com.willr27.blocklings.entity;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.block.LightBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public final class BlocklingsBlocks {
    public static final Block LIGHT = Registry.register(
            BuiltInRegistries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "light"),
            new LightBlock());

    private BlocklingsBlocks() {
    }

    public static void register() {
        Blocklings.LOGGER.debug("Registered blocklings blocks");
    }
}
