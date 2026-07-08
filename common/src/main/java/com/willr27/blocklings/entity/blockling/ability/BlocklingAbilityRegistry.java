package com.willr27.blocklings.entity.blockling.ability;

import com.willr27.blocklings.config.BlocklingsConfig;
import com.willr27.blocklings.entity.blockling.ability.handlers.BlocklingAbilityHandlers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public final class BlocklingAbilityRegistry
{
    private static final Map<TypeFamily, BlocklingAbilityHandler> HANDLERS = new EnumMap<>(TypeFamily.class);
    private static boolean initialized;

    private BlocklingAbilityRegistry()
    {
    }

    public static void init()
    {
        if (initialized)
        {
            return;
        }

        initialized = true;
        register(BlocklingAbilityHandlers.GRASS);
        register(BlocklingAbilityHandlers.DIRT);
        register(BlocklingAbilityHandlers.WOOD);
        register(BlocklingAbilityHandlers.STONE);
        register(BlocklingAbilityHandlers.IRON);
        register(BlocklingAbilityHandlers.GOLD);
        register(BlocklingAbilityHandlers.DIAMOND);
        register(BlocklingAbilityHandlers.EMERALD);
        register(BlocklingAbilityHandlers.LAPIS);
        register(BlocklingAbilityHandlers.OBSIDIAN);
        register(BlocklingAbilityHandlers.GLOWSTONE);
        register(BlocklingAbilityHandlers.QUARTZ);
        register(BlocklingAbilityHandlers.NETHERITE);
    }

    public static void register(@Nonnull BlocklingAbilityHandler handler)
    {
        HANDLERS.put(handler.family(), handler);
    }

    @Nullable
    public static BlocklingAbilityHandler get(@Nonnull TypeFamily family)
    {
        return HANDLERS.get(family);
    }

    @Nullable
    public static BlocklingTypeProfile profile(@Nonnull TypeFamily family)
    {
        BlocklingAbilityHandler handler = get(family);
        return handler != null ? handler.profile() : null;
    }

    public static int defaultActiveCooldownTicks(@Nonnull TypeFamily family)
    {
        return switch (family)
        {
            case GRASS -> BlocklingsConfig.COMMON.abilities.grass.activeCooldownSeconds.get() * 20;
            case DIRT -> BlocklingsConfig.COMMON.abilities.dirt.activeCooldownSeconds.get() * 20;
            case WOOD -> BlocklingsConfig.COMMON.abilities.wood.activeCooldownSeconds.get() * 20;
            case STONE -> BlocklingsConfig.COMMON.abilities.stone.activeCooldownSeconds.get() * 20;
            case IRON -> BlocklingsConfig.COMMON.abilities.iron.activeCooldownSeconds.get() * 20;
            case GOLD -> BlocklingsConfig.COMMON.abilities.gold.activeCooldownSeconds.get() * 20;
            case DIAMOND -> BlocklingsConfig.COMMON.abilities.diamond.activeCooldownSeconds.get() * 20;
            case EMERALD -> BlocklingsConfig.COMMON.abilities.emerald.activeCooldownSeconds.get() * 20;
            case LAPIS -> BlocklingsConfig.COMMON.abilities.lapis.activeCooldownSeconds.get() * 20;
            case OBSIDIAN -> BlocklingsConfig.COMMON.abilities.obsidian.activeCooldownSeconds.get() * 20;
            case GLOWSTONE -> BlocklingsConfig.COMMON.abilities.glowstone.activeCooldownSeconds.get() * 20;
            case QUARTZ -> BlocklingsConfig.COMMON.abilities.quartz.activeCooldownSeconds.get() * 20;
            case NETHERITE -> BlocklingsConfig.COMMON.abilities.netherite.activeCooldownSeconds.get() * 20;
        };
    }
}
