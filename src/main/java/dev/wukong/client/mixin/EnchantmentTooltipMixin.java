package dev.wukong.client.mixin;
import dev.wukong.client.EnchantmentInfo;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemStack.class)
public abstract class EnchantmentTooltipMixin {
    @Inject(method="getTooltipLines",at=@At("RETURN"),cancellable=true,remap=false)
    private void wukong$enchantmentInfo(Item.TooltipContext context, Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        var original=cir.getReturnValue();
        var updated=EnchantmentInfo.append((ItemStack)(Object)this,original);
        if(updated!=original) cir.setReturnValue(updated);
    }
}
