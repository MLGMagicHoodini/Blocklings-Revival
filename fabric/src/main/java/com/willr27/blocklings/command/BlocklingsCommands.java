package com.willr27.blocklings.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.blockling.BlocklingType;
import com.willr27.blocklings.entity.blockling.attribute.BlocklingAttributes.Level;
import com.willr27.blocklings.network.NetworkHandler;
import com.willr27.blocklings.network.messages.SetLevelCommandMessage;
import com.willr27.blocklings.network.messages.SetTypeCommandMessage;
import com.willr27.blocklings.network.messages.SetXpCommandMessage;
import com.willr27.blocklings.util.BlocklingsTranslationTextComponent;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class BlocklingsCommands {
    private BlocklingsCommands() {
    }

    public static void init() {
        ArgumentTypeRegistry.registerArgumentType(
                ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "blockling_type"),
                BlocklingTypeArgument.class,
                SingletonArgumentInfo.contextFree(BlocklingTypeArgument::new));

        ArgumentTypeRegistry.registerArgumentType(
                ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "blockling_level"),
                BlocklingLevelArgument.class,
                SingletonArgumentInfo.contextFree(BlocklingLevelArgument::new));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
        Blocklings.LOGGER.debug("Blocklings commands initialized");
    }

    private static void registerCommands(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("blockling").requires(source -> source.hasPermission(2)).then(
                        Commands.literal("set").then(
                                Commands.literal("type").then(
                                        Commands.literal("primary").then(
                                                Commands.argument("type", new BlocklingTypeArgument())
                                                        .executes(context -> executeTypeCommand(context, false)))).then(
                                        Commands.literal("natural").then(
                                                Commands.argument("type", new BlocklingTypeArgument())
                                                        .executes(context -> executeTypeCommand(context, true))))).then(
                                Commands.literal("level").then(
                                        Commands.argument("level", new BlocklingLevelArgument()).then(
                                                Commands.argument("value", IntegerArgumentType.integer(Level.MIN, Level.MAX))
                                                        .executes(context -> executeLevelCommand(context))))).then(
                                Commands.literal("xp").then(
                                        Commands.argument("level", new BlocklingLevelArgument()).then(
                                                Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(context -> executeXpCommand(context)))))));
    }

    private static int executeTypeCommand(@Nonnull CommandContext<CommandSourceStack> context, boolean natural) {
        CommandSourceStack source = context.getSource();
        Player player = source.getPlayer();
        if (player == null) {
            return 1;
        }

        BlocklingType blocklingType = context.getArgument("type", BlocklingType.class);
        NetworkHandler.sendToClient(player, new SetTypeCommandMessage(blocklingType.key, natural));
        return 1;
    }

    private static int executeLevelCommand(@Nonnull CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Player player = source.getPlayer();
        if (player == null) {
            return 1;
        }

        Level level = context.getArgument("level", Level.class);
        int value = context.getArgument("value", Integer.class);
        NetworkHandler.sendToClient(player, new SetLevelCommandMessage(level, value));
        return 1;
    }

    private static int executeXpCommand(@Nonnull CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Player player = source.getPlayer();
        if (player == null) {
            return 1;
        }

        Level level = context.getArgument("level", Level.class);
        int value = context.getArgument("value", Integer.class);
        NetworkHandler.sendToClient(player, new SetXpCommandMessage(level, value));
        return 1;
    }

    public static class BlocklingTypeArgument implements ArgumentType<BlocklingType> {
        @Override
        public BlocklingType parse(StringReader reader) throws CommandSyntaxException {
            String key = reader.readString();
            return BlocklingType.TYPES.stream()
                    .filter(type -> type.key.equals(key))
                    .findFirst()
                    .orElseThrow(() -> BlocklingTypeArgument.ERROR_INVALID_VALUE.create(key));
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return SharedSuggestionProvider.suggest(
                    BlocklingType.TYPES.stream().map(type -> type.key),
                    builder);
        }

        @Override
        public Collection<String> getExamples() {
            return BlocklingType.TYPES.stream().map(type -> type.key).limit(3).collect(Collectors.toList());
        }

        public static final DynamicCommandExceptionType ERROR_INVALID_VALUE =
                new DynamicCommandExceptionType(obj -> BlocklingsTranslationTextComponent.of("argument.type.invalid", obj));
    }

    public static class BlocklingLevelArgument implements ArgumentType<Level> {
        @Override
        public Level parse(StringReader reader) throws CommandSyntaxException {
            String name = reader.readString();
            try {
                return Level.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw ERROR_INVALID_VALUE.create(name);
            }
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return SharedSuggestionProvider.suggest(
                    Arrays.stream(Level.values()).map(Enum::name),
                    builder);
        }

        @Override
        public Collection<String> getExamples() {
            return List.of("COMBAT", "MINING", "TOTAL");
        }

        public static final DynamicCommandExceptionType ERROR_INVALID_VALUE =
                new DynamicCommandExceptionType(obj -> BlocklingsTranslationTextComponent.of("argument.level.invalid", obj));
    }
}
