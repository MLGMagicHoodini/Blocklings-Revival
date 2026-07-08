package com.willr27.blocklings.loader;

import com.willr27.blocklings.entity.BlocklingsBlocks;
import com.willr27.blocklings.entity.BlocklingsEntityTypes;
import com.willr27.blocklings.entity.BlocklingsItems;
import com.willr27.blocklings.entity.BlocklingsMenus;
import com.willr27.blocklings.entity.BlocklingsSounds;

public final class BlocklingsRegistryBinding {
    private BlocklingsRegistryBinding() {
    }

    public static void bindFabric() {
        BlocklingsRegistries.BLOCKLING_ENTITY = () -> BlocklingsEntityTypes.BLOCKLING;
        BlocklingsRegistries.BLOCKLING_ITEM = () -> BlocklingsItems.BLOCKLING;
        BlocklingsRegistries.BLOCKLING_WHISTLE = () -> BlocklingsItems.BLOCKLING_WHISTLE;
        BlocklingsRegistries.BLOCKLING_SPAWN_EGG = () -> BlocklingsItems.BLOCKLING_SPAWN_EGG;
        BlocklingsRegistries.LIGHT_BLOCK = () -> BlocklingsBlocks.LIGHT;
        BlocklingsRegistries.BLOCKLING_WHISTLE_SOUND = () -> BlocklingsSounds.BLOCKLING_WHISTLE;
        BlocklingsRegistries.EQUIPMENT_MENU = () -> BlocklingsMenus.EQUIPMENT;
    }
}
