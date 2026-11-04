package com.willr27.blocklings.platform;

import com.willr27.blocklings.network.BlocklingNetworkPayload;
import com.willr27.blocklings.network.NetworkHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client packet registration. Kept out of {@link FabricNetworkBridge} so a dedicated server
 * never loads Fabric's client networking classes.
 */
public final class FabricClientNetwork {
    private FabricClientNetwork() {
    }

    public static void register() {
        FabricNetworkBridge.ensurePayloadTypes();
        FabricNetworkBridge.setServerboundSender(ClientPlayNetworking::send);
        ClientPlayNetworking.registerGlobalReceiver(
                BlocklingNetworkPayload.TYPE,
                (payload, context) -> context.client().execute(
                        () -> NetworkHandler.handlePayload(payload, context.player())));
    }
}
