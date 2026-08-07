package com.willr27.blocklings;

import com.willr27.blocklings.capabilities.ContainerConfigureCapability;
import com.willr27.blocklings.command.BlocklingsCommands;
import com.willr27.blocklings.entity.BlocklingsBlocks;
import com.willr27.blocklings.entity.BlocklingsCreativeTabs;
import com.willr27.blocklings.entity.BlocklingsEntityTypes;
import com.willr27.blocklings.entity.BlocklingsItems;
import com.willr27.blocklings.entity.BlocklingsMenus;
import com.willr27.blocklings.entity.BlocklingsSounds;
import com.willr27.blocklings.entity.EntityGeneration;
import com.willr27.blocklings.entity.blockling.BlocklingEntity;
import com.willr27.blocklings.entity.blockling.BlocklingSpawnDiagnostics;
import com.willr27.blocklings.entity.blockling.BlocklingType;
import com.willr27.blocklings.entity.blockling.ability.BlocklingAbilityRegistry;
import com.willr27.blocklings.event.FabricDimensionEvents;
import com.willr27.blocklings.interop.ModProxies;
import com.willr27.blocklings.loader.BlocklingsRegistryBinding;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.LoaderEnvironment;
import com.willr27.blocklings.network.NetworkHandler;
import com.willr27.blocklings.platform.FabricConfigBridge;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;

public class BlocklingsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        LoaderEnvironment.init(
                FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? Dist.CLIENT : Dist.SERVER,
                FabricLoader.getInstance().getConfigDir());

        BlocklingsEntityTypes.register();
        BlocklingsBlocks.register();
        BlocklingsItems.register();
        BlocklingsCreativeTabs.register();
        BlocklingsSounds.register();
        BlocklingsMenus.register();

        BlocklingsRegistryBinding.bindFabric();
        FabricConfigBridge.bind();

        FabricDefaultAttributeRegistry.register(BlocklingsEntityTypes.BLOCKLING, BlocklingEntity.createAttributes());

        BlocklingType.init();
        BlocklingAbilityRegistry.init();

        BlocklingsCommon.init();
        NetworkHandler.init();
        ModProxies.init();
        BlocklingsCommands.init();
        ContainerConfigureCapability.register();
        EntityGeneration.init();
        FabricDimensionEvents.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            com.willr27.blocklings.util.EntityUtil.onWorldAvailable(server.overworld());
            com.willr27.blocklings.util.ToolUtil.init();
            Blocklings.LOGGER.info("Blocklings server starting — integrations: {}",
                    com.willr27.blocklings.compat.ModCompatRegistry.activeCompatSummary());
            BlocklingSpawnDiagnostics.dumpBiomeRegistrations(server);
        });
    }
}
