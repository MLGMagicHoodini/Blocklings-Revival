package com.willr27.blocklings.client.gui.containers;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlocklingsMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Blocklings.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<EquipmentContainer>> EQUIPMENT =
            MENUS.register("equipment", () -> IMenuTypeExtension.create(
                    (windowId, inv, buf) -> {
                        int entityId = buf.readVarInt();
                        BlocklingEntity blockling = (BlocklingEntity) inv.player.level().getEntity(entityId);
                        if (blockling == null) {
                            throw new IllegalStateException("Unknown blockling entity id: " + entityId);
                        }
                        return new EquipmentContainer(windowId, inv.player, blockling);
                    }));

    private BlocklingsMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
