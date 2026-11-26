package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;

/** Purely local lighting override for the 26.3 lightmap. */
public final class BrightnessController {
    private static boolean previouslyEnabled = false;

    /**
     * Force vanilla's extractor to re-compute the lightmap whenever Fullbright is
     * active and once after switching OFF, so the original lightmap is restored.
     * Called at extract() HEAD, before Minecraft checks its dirty flag.
     */
    public static boolean shouldRefresh() {
        return Minecraft.getInstance().level != null
                && ((BenchConfig.INSTANCE.visualMasterEnabled && BenchConfig.INSTANCE.fullbrightEnabled) || previouslyEnabled);
    }

    /** Called at extract() TAIL after the original vanilla data was populated. */
    public static void apply(LightmapRenderState state) {
        boolean enabled = Minecraft.getInstance().level != null
                && BenchConfig.INSTANCE.visualMasterEnabled && BenchConfig.INSTANCE.fullbrightEnabled;
        if (!enabled) {
            if (previouslyEnabled) {
                // The extractor ran with its dirty flag set at HEAD. Its output
                // now contains the vanilla state; upload that state once more.
                state.needsUpdate = true;
                System.out.println("[WuKong] Fullbright OFF: vanilla lightmap restored");
            }
            previouslyEnabled = false;
            return;
        }

        // Vanilla brightness is clamped to its normal menu range. A high shader
        // factor alone cannot create a light floor at light level 0. Force the
        // lightmap's ambient/night-vision inputs to full white as well.
        state.ambientColor = LightmapRenderStateExtractor.WHITE;
        state.nightVisionColor = LightmapRenderStateExtractor.WHITE;
        state.nightVisionEffectIntensity = 1.0F;
        state.brightness = 8.0F;
        state.darknessEffectScale = 0.0F;
        state.bossOverlayWorldDarkening = 0.0F;
        state.needsUpdate = true; // Required: otherwise 26.3 skips GPU upload.

        if (!previouslyEnabled) {
            System.out.println("[WuKong] Fullbright ON: lightmap update active");
        }
        previouslyEnabled = true;
    }

    private BrightnessController() {}
}
