package dev.wukong.client;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/** Calculates only from currently visible merchant data. Never executes trades. */
public final class TradeCalculator {
    private static String tr(String de,String en){return WuKongLanguage.text(de,en);}
    private static Component item(ItemStack s){return Component.literal(s.getCount()+" × ").append(s.getHoverName());}
    private static String ratio(ItemStack cost,ItemStack result){return String.format(WuKongLanguage.german()?Locale.GERMANY:Locale.US,"%.2f",(double)cost.getCount()/result.getCount());}
    public static List<Component> describe(MerchantOffer offer,MerchantOffers offers) {
        var lines=new ArrayList<Component>();
        var a=offer.getCostA(); var b=offer.getCostB(); var result=offer.getResult(); var base=offer.getBaseCostA();
        lines.add(Component.literal(tr("WuKong • Handelsrechner","WuKong • Trade Calculator")).withStyle(ChatFormatting.AQUA));
        var cost=Component.literal(tr("Preis: ","Cost: ")).append(item(a));
        if(!b.isEmpty()) cost.append(Component.literal(" + ")).append(item(b));
        lines.add(cost); lines.add(Component.literal(tr("Ertrag: ","Receive: ")).append(item(result)));
        int delta=a.getCount()-base.getCount();
        if(delta!=0) lines.add(Component.literal(tr("Grundpreis: ","Base price: ")).append(item(base)).append(Component.literal(" ("+(delta>0?"+":"")+delta+")")));
        int remaining=Math.max(0,offer.getMaxUses()-offer.getUses());
        lines.add(Component.literal(tr("Restliche Nutzungen: ","Remaining uses: ")+remaining+"/"+offer.getMaxUses()).withStyle(remaining==0?ChatFormatting.RED:ChatFormatting.GRAY));
        if(!a.isEmpty() && !result.isEmpty()) {
            lines.add(Component.literal(tr("Pro Ausgabeeinheit: ","Per output item: ")+ratio(a,result)+" ").append(a.getHoverName()));
            if(!b.isEmpty()) lines.add(Component.literal(" + "+ratio(b,result)+" ").append(b.getHoverName()));
            if(b.isEmpty()) {
                MerchantOffer best=null;
                for(var other:offers) {
                    if(other.isOutOfStock() || !other.getCostB().isEmpty() || other.getResult().isEmpty() || other.getCostA().isEmpty()) continue;
                    if(!ItemStack.isSameItemSameComponents(a,other.getCostA()) || !ItemStack.isSameItemSameComponents(result,other.getResult())) continue;
                    if(best==null || (long)other.getCostA().getCount()*best.getResult().getCount() < (long)best.getCostA().getCount()*other.getResult().getCount()) best=other;
                }
                if(best!=null) {
                    long comparison=(long)a.getCount()*best.getResult().getCount()-(long)best.getCostA().getCount()*result.getCount();
                    lines.add(Component.literal(tr("Bester vergleichbarer Stückpreis: ","Best comparable unit price: ")+ratio(best.getCostA(),best.getResult())).withStyle(comparison==0&&!offer.isOutOfStock()?ChatFormatting.GREEN:ChatFormatting.YELLOW));
                }
            }
        }
        return lines;
    }
    private TradeCalculator(){}
}
