package dev.wukong.client.mixin;

import dev.wukong.client.SmartPreview;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientTooltipComponent.class)
public interface ClientTooltipMixin {
    @Inject(method="create(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",at=@At("HEAD"),cancellable=true,remap=false)
    private static void wukong$create(TooltipComponent component, CallbackInfoReturnable<ClientTooltipComponent> cir) {
        if(component instanceof SmartPreview preview) cir.setReturnValue(new SmartPreview.Renderer(preview));
    }
}
