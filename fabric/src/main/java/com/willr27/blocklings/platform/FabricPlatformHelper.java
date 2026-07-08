package com.willr27.blocklings.platform;

import com.willr27.blocklings.hybrid.BukkitDetector;
import com.willr27.blocklings.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDedicatedServer() {
        return FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER;
    }

    @Override
    public boolean isHybridServer() {
        return BukkitDetector.isBukkitPresent();
    }

    @Override
    public String hybridServerName() {
        return BukkitDetector.serverSoftwareName();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean allowAnimalTame(Animal animal, Player player) {
        return true;
    }
}
