package dev.wukong.client.mixin;
import dev.wukong.client.DynamicZoom;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Camera.class)
public abstract class ZoomCameraMixin {
    @Inject(method="calculateFov(F)F",at=@At("RETURN"),cancellable=true,remap=false,require=1)
    private void wukong$zoom(float partial,CallbackInfoReturnable<Float> cir){cir.setReturnValue(DynamicZoom.fov(cir.getReturnValue()));}
}
