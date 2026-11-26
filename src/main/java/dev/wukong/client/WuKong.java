package dev.wukong.client;

import net.fabricmc.api.ClientModInitializer;

/** Fabric 26.3 entry point, client-only. */
public final class WuKong implements ClientModInitializer {
    public static final String ID = "wukong";

    @Override
    public void onInitializeClient() {
        WuKongKeys.initialize();
        ServerProfiles.install();
        ClientHooks.install();
        SmartInventory.install();
        EvolutionUi.install();
    }
}
