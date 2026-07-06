package com.willr27.blocklings.block;

/*import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;*/

import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An invisible block that emits light.
 * Used by blocklings to emit light.
 */
public class LightBlock extends Block
{
    /**
     * Default constructor.
     */
    public LightBlock()
    {
        super(AbstractBlock.Properties.of(new Material.Builder(MaterialColor.NONE)
                .replaceable()
                .nonSolid()
                .build())
                .noDrops()
                .noOcclusion()
                .noCollission()
                .lightLevel((blockState) -> 15));
    }

    @Override
    public VoxelShape getShape(BlockState blockState, IBlockReader blockReader, BlockPos blockPos, ISelectionContext selectionContext)
    {
        return VoxelShapes.empty();
    }
}
