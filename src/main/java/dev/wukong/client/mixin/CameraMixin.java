package dev.wukong.client.mixin;

import dev.wukong.client.BowViewSplit;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Interpolates the predicted player look angle in the local render camera. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow(remap = false)
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("TAIL"), remap = false, require = 1)
    private void wukong$predictionView(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (BowViewSplit.shouldApply()) {
            float partial = deltaTracker.getGameTimeDeltaPartialTick(false);
            setRotation(BowViewSplit.yaw(partial), BowViewSplit.pitch(partial));
        }
    }
}
