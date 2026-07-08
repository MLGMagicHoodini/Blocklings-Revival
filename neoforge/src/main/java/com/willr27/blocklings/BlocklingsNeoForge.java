package com.willr27.blocklings;

import com.willr27.blocklings.block.BlocklingsBlocks;
import com.willr27.blocklings.client.renderer.entity.BlocklingRenderer;
import com.willr27.blocklings.command.BlocklingsArgumentTypes;
import com.willr27.blocklings.command.BlocklingsCommands;
import com.willr27.blocklings.config.NeoForgeBlocklingsConfig;
import com.willr27.blocklings.loader.BlocklingsRegistryBinding;
import com.willr27.blocklings.loader.Dist;
import com.willr27.blocklings.loader.LoaderEnvironment;
import com.willr27.blocklings.platform.NeoForgeConfigBridge;
import com.willr27.blocklings.client.gui.containers.BlocklingsMenus;
import com.willr27.blocklings.entity.BlocklingsEntityTypes;
import com.willr27.blocklings.entity.EntityGeneration;
import com.willr27.blocklings.interop.ModProxies;
import com.willr27.blocklings.client.BlocklingItemClientRegistration;
import com.willr27.blocklings.item.BlocklingsCreativeTabs;
import com.willr27.blocklings.item.BlocklingsItems;
import com.willr27.blocklings.network.NetworkHandler;
import com.willr27.blocklings.platform.NeoForgeNetworkBridge;
import com.willr27.blocklings.sound.BlocklingsSounds;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(BlocklingsConstants.MODID)
public class BlocklingsNeoForge {

    public BlocklingsNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        LoaderEnvironment.init(
                FMLEnvironment.dist.isClient() ? Dist.CLIENT : Dist.SERVER,
                FMLPaths.CONFIGDIR.get());

        BlocklingsCommon.init();
        BlocklingsRegistryBinding.bindNeoForge();
        NeoForgeConfigBridge.bind();

        BlocklingsEntityTypes.register(modEventBus);
        BlocklingsBlocks.register(modEventBus);
        BlocklingsItems.register(modEventBus);
        BlocklingsCreativeTabs.register(modEventBus);
        BlocklingsSounds.register(modEventBus);
        BlocklingsMenus.register(modEventBus);
        BlocklingsArgumentTypes.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(NeoForgeNetworkBridge::registerPayloads);

        NeoForge.EVENT_BUS.register(this);
        NeoForgeBlocklingsConfig.init(modContainer);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModProxies.init();
            NetworkHandler.init();
            BlocklingsCommands.init();
            com.willr27.blocklings.capabilities.ContainerConfigureCapability.register();
            EntityGeneration.init();
        });
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(BlocklingItemClientRegistration::registerItemModelsProperties);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BlocklingsEntityTypes.BLOCKLING.get(), BlocklingRenderer::new);
    }

    @SubscribeEvent
    public void onServerStarting(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
        Blocklings.LOGGER.info("Blocklings server starting — integrations: {}",
                com.willr27.blocklings.compat.ModCompatRegistry.activeCompatSummary());
    }
}
