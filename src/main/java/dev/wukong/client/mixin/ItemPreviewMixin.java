package dev.wukong.client.mixin;

import dev.wukong.client.BenchConfig;
import dev.wukong.client.SmartPreview;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import java.util.ArrayList;
import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemPreviewMixin {
    @Inject(method="getTooltipImage",at=@At("RETURN"),cancellable=true,remap=false)
    private void wukong$preview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        ItemStack stack=(ItemStack)(Object)this;
        var cfg=BenchConfig.INSTANCE;
        if(!cfg.inventoryMasterEnabled) return;
        if (cfg.shulkerPreviewEnabled && stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock) {
            var contents=stack.get(DataComponents.CONTAINER);
            var items=new ArrayList<ItemStack>(27);
            if(contents!=null) contents.itemCopies().limit(27).forEach(items::add);
            while(items.size()<27) items.add(ItemStack.EMPTY);
            cir.setReturnValue(Optional.of(new SmartPreview(items,9)));
        } else if(cfg.bundlePreviewEnabled) {
            var contents=stack.get(DataComponents.BUNDLE_CONTENTS);
            if(contents!=null && !contents.isEmpty()) {
                // Vanilla bundle image remains in place when the module is disabled.
                var items=contents.itemCopies().limit(64).toList();
                cir.setReturnValue(Optional.of(new SmartPreview(items,Math.min(9,Math.max(1,items.size())))));
            }
        }
    }
}
