package com.willr27.blocklings.capabilities;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player-side container configuration flag.
 */
public final class ContainerConfigureCapability {
    private static final Map<UUID, ContainerConfigureCapability> INSTANCES = new ConcurrentHashMap<>();

    public boolean isConfiguring = false;

    private ContainerConfigureCapability() {
    }

    public static ContainerConfigureCapability get(Player player) {
        return INSTANCES.computeIfAbsent(player.getUUID(), ignored -> new ContainerConfigureCapability());
    }

    public static void register() {
    }
}
