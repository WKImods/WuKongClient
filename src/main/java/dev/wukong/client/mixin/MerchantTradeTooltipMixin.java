package dev.wukong.client.mixin;
import dev.wukong.client.BenchConfig;
import dev.wukong.client.TradeCalculator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MerchantScreen.class)
public abstract class MerchantTradeTooltipMixin extends AbstractContainerScreen<MerchantMenu> {
    @Shadow private int scrollOff;
    protected MerchantTradeTooltipMixin(MerchantMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @Inject(method="extractContents",at=@At("TAIL"),remap=false)
    private void wukong$tradeDetails(GuiGraphicsExtractor g,int mx,int my,float partial,CallbackInfo ci) {
        if(!BenchConfig.INSTANCE.tooltipsMasterEnabled || !BenchConfig.INSTANCE.tradeCalculatorEnabled || !minecraft.hasShiftDown() || !menu.getCarried().isEmpty()) return;
        int x=mx-leftPos-5, y=my-topPos-18;
        if(x<0 || x>=88 || y<0 || y>=140) return;
        int index=scrollOff+y/20;
        var offers=menu.getOffers();
        if(index<0 || index>=offers.size()) return;
        g.setComponentTooltipForNextFrame(font,TradeCalculator.describe(offers.get(index),offers),mx,my);
    }
}
