package com.willr27.blocklings;

import com.willr27.blocklings.compat.ModCompatRegistry;
import com.willr27.blocklings.hybrid.HybridServerDetector;
import com.willr27.blocklings.platform.Services;

/**
 * Shared bootstrap invoked by each loader ({@code @Mod} / Fabric entrypoint).
 */
public final class BlocklingsCommon {

    private static boolean initialized;

    private BlocklingsCommon() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        BlocklingsConstants.LOG.info("Blocklings Revival — loader: {}", Services.PLATFORM.getClass().getSimpleName());
        if (HybridServerDetector.isHybridPresent()) {
            BlocklingsConstants.LOG.info("Blocklings: hybrid server detected ({})", Services.PLATFORM.hybridServerName());
        } else if (Services.PLATFORM.isDedicatedServer()) {
            BlocklingsConstants.LOG.info("Blocklings: dedicated NeoForge server — server-authoritative networking active");
        }
        BlocklingsConstants.LOG.info("Blocklings: integrations — {}", ModCompatRegistry.activeCompatSummary());

        Services.NETWORK.registerPackets();
    }
}
