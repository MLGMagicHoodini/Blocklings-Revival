package com.willr27.blocklings.platform;

import com.willr27.blocklings.hybrid.BukkitDetector;
import com.willr27.blocklings.platform.services.IPlatformHelper;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.EventHooks;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDedicatedServer() {
        return FMLEnvironment.dist.isDedicatedServer();
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
        return !FMLEnvironment.production;
    }

    @Override
    public boolean allowAnimalTame(Animal animal, Player player) {
        return !EventHooks.onAnimalTame(animal, player);
    }
}
