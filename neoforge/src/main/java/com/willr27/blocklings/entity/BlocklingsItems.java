package com.willr27.blocklings.entity;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.item.BlocklingItem;
import com.willr27.blocklings.item.BlocklingWhistleItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public final class BlocklingsItems {
    public static final Item BLOCKLING_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "blockling_spawn_egg"),
            new SpawnEggItem(BlocklingsEntityTypes.BLOCKLING, 0x785439, 0x466f33, new Item.Properties()));

    public static final Item BLOCKLING = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "blockling"),
            new BlocklingItem());

    public static final Item BLOCKLING_WHISTLE = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "blockling_whistle"),
            new BlocklingWhistleItem());

    private BlocklingsItems() {
    }

    public static void register() {
        Blocklings.LOGGER.debug("Registered blocklings items");
    }
}
