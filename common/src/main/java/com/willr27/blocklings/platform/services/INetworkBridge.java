package com.willr27.blocklings.platform.services;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Loader-specific network bootstrap (NeoForge PayloadRegistrar / Fabric networking).
 */
public interface INetworkBridge {
    void registerPackets();

    void sendToServer(Object message);

    void sendToPlayer(ServerPlayer player, Object message);
}
