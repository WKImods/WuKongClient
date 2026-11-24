package dev.wukong.client.mixin;

import dev.wukong.client.BrightnessController;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Force extraction *and* GPU upload whenever Fullbright changes. */
@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapMixin {
    @Shadow(remap = false)
    private boolean needsUpdate;

    @Inject(method = "extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V",
            at = @At("HEAD"), remap = false, require = 1)
    private void wukong$refreshFullbright(LightmapRenderState state, float partialTicks, CallbackInfo ci) {
        if (BrightnessController.shouldRefresh()) {
            this.needsUpdate = true;
        }
    }

    @Inject(method = "extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V",
            at = @At("TAIL"), remap = false, require = 1)
    private void wukong$applyFullbright(LightmapRenderState state, float partialTicks, CallbackInfo ci) {
        BrightnessController.apply(state);
    }
}
