package com.willr27.blocklings.entity;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.blockling.BlocklingType;
import com.willr27.blocklings.item.BlocklingItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

public final class BlocklingsCreativeTabs {
    public static final CreativeModeTab MAIN = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "main"),
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.blocklings"))
                    .icon(() -> BlocklingItem.createPreview(BlocklingType.GRASS))
                    .displayItems((parameters, output) -> {
                        output.accept(BlocklingsItems.BLOCKLING_SPAWN_EGG);
                        // One preview per type (typed icon + name). Same as NeoForge.
                        for (BlocklingType type : BlocklingType.TYPES) {
                            if (type.isShownInCreativeTab()) {
                                output.accept(BlocklingItem.createPreview(type));
                            }
                        }
                        output.accept(BlocklingsItems.BLOCKLING_WHISTLE);
                    })
                    .build());

    private BlocklingsCreativeTabs() {
    }

    public static void register() {
        Blocklings.LOGGER.debug("Registered blocklings creative tab");
    }
}
