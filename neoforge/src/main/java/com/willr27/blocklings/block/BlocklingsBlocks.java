package com.willr27.blocklings.block;

import com.willr27.blocklings.Blocklings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nonnull;

public final class BlocklingsBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, Blocklings.MODID);

    public static final DeferredHolder<Block, Block> LIGHT = BLOCKS.register("light", LightBlock::new);

    private BlocklingsBlocks() {
    }

    public static void register(@Nonnull IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
