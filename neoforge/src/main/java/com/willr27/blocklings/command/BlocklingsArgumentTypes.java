package com.willr27.blocklings.command;

import com.willr27.blocklings.Blocklings;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlocklingsArgumentTypes {
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, Blocklings.MODID);

    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, ArgumentTypeInfo<BlocklingsCommands.BlocklingTypeArgument, ?>> BLOCKLING_TYPE =
            ARGUMENT_TYPES.register("blockling_type", () -> ArgumentTypeInfos.registerByClass(
                    BlocklingsCommands.BlocklingTypeArgument.class,
                    SingletonArgumentInfo.contextFree(BlocklingsCommands.BlocklingTypeArgument::new)));

    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, ArgumentTypeInfo<BlocklingsCommands.BlocklingLevelArgument, ?>> BLOCKLING_LEVEL =
            ARGUMENT_TYPES.register("blockling_level", () -> ArgumentTypeInfos.registerByClass(
                    BlocklingsCommands.BlocklingLevelArgument.class,
                    SingletonArgumentInfo.contextFree(BlocklingsCommands.BlocklingLevelArgument::new)));

    private BlocklingsArgumentTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ARGUMENT_TYPES.register(modEventBus);
    }
}
