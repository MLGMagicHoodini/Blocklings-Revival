package com.willr27.blocklings.loader;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Loader-populated registry holders so common gameplay code stays loader-agnostic.
 */
public final class BlocklingsRegistries
{
    @Nullable
    public static Supplier<EntityType<BlocklingEntity>> BLOCKLING_ENTITY;

    @Nullable
    public static Supplier<Item> BLOCKLING_ITEM;

    @Nullable
    public static Supplier<Item> BLOCKLING_WHISTLE;

    @Nullable
    public static Supplier<Item> BLOCKLING_SPAWN_EGG;

    @Nullable
    public static Supplier<Block> LIGHT_BLOCK;

    @Nullable
    public static Supplier<SoundEvent> BLOCKLING_WHISTLE_SOUND;

    @Nullable
    public static Supplier<MenuType<?>> EQUIPMENT_MENU;

    private BlocklingsRegistries()
    {
    }

    public static EntityType<BlocklingEntity> blocklingEntity()
    {
        return BLOCKLING_ENTITY.get();
    }

    public static Item blocklingItem()
    {
        return BLOCKLING_ITEM.get();
    }

    public static Item blocklingWhistle()
    {
        return BLOCKLING_WHISTLE.get();
    }

    public static ItemStack blocklingItemStack()
    {
        return new ItemStack(blocklingItem());
    }

    public static SoundEvent blocklingWhistleSound()
    {
        return BLOCKLING_WHISTLE_SOUND.get();
    }

    public static MenuType<?> equipmentMenu()
    {
        return EQUIPMENT_MENU.get();
    }
}
