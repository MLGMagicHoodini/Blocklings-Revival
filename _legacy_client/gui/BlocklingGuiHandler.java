package com.willr27.blocklings.client.gui;

import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;

/** Client GUI stub — full custom UI port pending (GuiGraphics migration). */
public final class BlocklingGuiHandler {
    public static final BlocklingGuiHandler INSTANCE = new BlocklingGuiHandler();

    private BlocklingGuiHandler() {
    }

    public void open(@Nonnull Player player, @Nonnull BlocklingEntity blockling) {
        // TODO: restore full GUI from _legacy_client after GuiGraphics port
    }
}
