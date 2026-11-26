package dev.wukong.client.mixin;
import dev.wukong.client.DynamicZoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
@Mixin(MouseHandler.class)
public abstract class ZoomMouseMixin {
    @Inject(method="onScroll(JDD)V",at=@At("HEAD"),cancellable=true,remap=false,require=1)
    private void wukong$scroll(long window,double x,double y,CallbackInfo ci){
        if(window!=0 && window==Minecraft.getInstance().getWindow().handle() && DynamicZoom.scroll(y))ci.cancel();
    }
    @ModifyArgs(method="turnPlayer(D)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;turn(DD)V",remap=false),remap=false,require=1)
    private void wukong$adaptive(Args args){
        args.set(0,DynamicZoom.mouseDelta((Double)args.get(0)));args.set(1,DynamicZoom.mouseDelta((Double)args.get(1)));
    }
}
