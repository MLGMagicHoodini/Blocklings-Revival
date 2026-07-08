package com.willr27.blocklings.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.OnlyIn;

import javax.annotation.Nonnull;
import java.util.UUID;

public abstract class Message {
    public abstract void handle(@Nonnull Player player);

    @OnlyIn(Dist.CLIENT)
    @Nonnull
    protected UUID getClientPlayerId() {
        Player player = Minecraft.getInstance().player;
        return player != null ? player.getUUID() : new UUID(0L, 0L);
    }

    @OnlyIn(Dist.CLIENT)
    @Nonnull
    protected Player getClientPlayer() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            throw new IllegalStateException("Client player is not available");
        }
        return player;
    }
}
