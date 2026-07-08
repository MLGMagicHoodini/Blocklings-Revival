package com.willr27.blocklings.platform.services;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

public interface IPlatformHelper {
    boolean isModLoaded(String modId);

    boolean isDedicatedServer();

    boolean isHybridServer();

    String hybridServerName();

    boolean isDevelopmentEnvironment();

    /** @return false if a loader event cancelled taming */
    boolean allowAnimalTame(Animal animal, Player player);
}
