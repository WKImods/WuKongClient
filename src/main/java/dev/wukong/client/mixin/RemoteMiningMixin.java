package dev.wukong.client.mixin;

import dev.wukong.client.RemoteMining;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hand control back to vanilla whenever there is no active automated block interaction. */
@Mixin(Minecraft.class)
public abstract class RemoteMiningMixin {
    @Inject(method="startAttack()Z",at=@At("HEAD"),remap=false,require=1)
    private void wukong$manualAttack(CallbackInfoReturnable<Boolean> ci){
        RemoteMining.beforeManualAttack(Minecraft.getInstance());
    }
    @Inject(method="continueAttack(Z)V",at=@At("HEAD"),cancellable=true,remap=false,require=1)
    private void wukong$continueAttack(boolean down,CallbackInfo ci){
        if(RemoteMining.tickAttack(Minecraft.getInstance(),down))ci.cancel();
    }
}
