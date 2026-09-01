package com.willr27.blocklings.item;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.blockling.BlocklingType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlocklingsCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Blocklings.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.blocklings"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> BlocklingItem.createPreview(BlocklingType.GRASS))
                    .displayItems((parameters, output) -> {
                        output.accept(BlocklingsItems.BLOCKLING_SPAWN_EGG.get());
                        // One preview per type (typed icon + name). No bare generic item — it looked identical to grass.
                        for (BlocklingType type : BlocklingType.TYPES)
                        {
                            if (type.isShownInCreativeTab())
                            {
                                output.accept(BlocklingItem.createPreview(type));
                            }
                        }
                        output.accept(BlocklingsItems.BLOCKLING_WHISTLE.get());
                    })
                    .build());

    private BlocklingsCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
    }
}
