package com.willr27.blocklings.item;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.BlocklingsEntityTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nonnull;

public final class BlocklingsItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, Blocklings.MODID);

    public static final DeferredHolder<Item, Item> BLOCKLING_SPAWN_EGG = ITEMS.register("blockling_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(
                    BlocklingsEntityTypes.BLOCKLING, 0x785439, 0x466f33, new Item.Properties()));

    public static final DeferredHolder<Item, Item> BLOCKLING = ITEMS.register("blockling", BlocklingItem::new);
    public static final DeferredHolder<Item, Item> BLOCKLING_WHISTLE = ITEMS.register("blockling_whistle", BlocklingWhistleItem::new);

    private BlocklingsItems() {
    }

    public static void register(@Nonnull IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
