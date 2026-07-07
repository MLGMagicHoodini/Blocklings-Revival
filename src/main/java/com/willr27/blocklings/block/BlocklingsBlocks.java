package com.willr27.blocklings.block;

import com.willr27.blocklings.Blocklings;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nonnull;

/**
 * Handles the registration of blocks.
 */
public class BlocklingsBlocks
{
    /**
     * The deferred block registry.
     */
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(NeoForgeRegistries.BLOCKS, Blocklings.MODID);

    public static final DeferredRegister<Block> LIGHT = BLOCKS.register("light", LightBlock::new);

    /**
     * Registers the blocks.
     */
    public static void register(@Nonnull IEventBus modEventBus)
    {
        BLOCKS.register(modEventBus);
    }
}
