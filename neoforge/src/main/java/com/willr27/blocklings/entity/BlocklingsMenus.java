package com.willr27.blocklings.entity;

import com.willr27.blocklings.Blocklings;
import com.willr27.blocklings.client.gui.containers.EquipmentContainer;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.platform.FabricMenuOpeningHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

public final class BlocklingsMenus {
    public static final MenuType<EquipmentContainer> EQUIPMENT = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(Blocklings.MODID, "equipment"),
            FabricMenuOpeningHelper.create((windowId, inv, buf) -> {
                int entityId = buf.readVarInt();
                BlocklingEntity blockling = (BlocklingEntity) inv.player.level().getEntity(entityId);
                if (blockling == null) {
                    throw new IllegalStateException("Unknown blockling entity id: " + entityId);
                }
                return new EquipmentContainer(windowId, inv.player, blockling);
            }));

    private BlocklingsMenus() {
    }

    public static void register() {
        Blocklings.LOGGER.debug("Registered blocklings menus");
    }
}
