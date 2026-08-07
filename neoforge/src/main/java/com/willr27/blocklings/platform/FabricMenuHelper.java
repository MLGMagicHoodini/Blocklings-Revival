package com.willr27.blocklings.platform;

import com.willr27.blocklings.client.gui.containers.EquipmentContainer;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.platform.services.IMenuHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

import javax.annotation.Nonnull;

public final class FabricMenuHelper implements IMenuHelper {
    @Override
    public void openEquipmentMenu(@Nonnull ServerPlayer player, @Nonnull BlocklingEntity blockling) {
        FabricMenuOpeningHelper.openMenu(player, new SimpleMenuProvider(
                        (id, inv, p) -> new EquipmentContainer(id, p, blockling),
                        Component.empty()),
                buf -> buf.writeVarInt(blockling.getId()));
    }
}
