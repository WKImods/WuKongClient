package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class IntelligentTooltipsScreen extends Screen {
    private final BenchConfig cfg = BenchConfig.INSTANCE;
    private int x,y,w;
    public IntelligentTooltipsScreen() { super(Component.literal(UiLabels.t("Intelligent Tooltips"))); }
    private static String tr(String de,String en) { return WuKongLanguage.text(de,en); }
    @Override protected void init() {
        w=Math.min(360,width-16); x=(width-w)/2; y=Math.max(4,(height-168)/2);
        toggle(30,tr("Verzauberungsinfo","Enchantment Info"),()->cfg.enchantmentInfoEnabled,v->cfg.enchantmentInfoEnabled=v);
        toggle(54,tr("Details nur mit SHIFT","Details only with SHIFT"),()->cfg.enchantmentInfoShiftOnly,v->cfg.enchantmentInfoShiftOnly=v);
        toggle(78,tr("Handelsrechner","Trade Calculator"),()->cfg.tradeCalculatorEnabled,v->cfg.tradeCalculatorEnabled=v);
        addRenderableWidget(Button.builder(Component.literal(tr("Zurück","Back")),b->Minecraft.getInstance().gui.setScreen(new SmartInventoryScreen())).bounds(x+10,y+132,w-20,20).build());
    }
    private void toggle(int pos,String name,BooleanSupplier get,Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(Component.literal(name+": "+(get.getAsBoolean()?UiLabels.t("ON"):UiLabels.t("OFF"))),b->{set.accept(!get.getAsBoolean());cfg.save();b.setMessage(Component.literal(name+": "+(get.getAsBoolean()?UiLabels.t("ON"):UiLabels.t("OFF"))));}).bounds(x+10,y+pos,w-20,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial) {
        g.fill(x,y,x+w,y+168,0xE5111A24);g.fill(x,y,x+w,y+25,0xFF204051);
        g.text(font,UiLabels.t("WUKONG / INTELLIGENT TOOLTIPS"),x+10,y+8,0xFFFFFFFF,false);
        super.extractRenderState(g,mx,my,partial);
        g.text(font,tr("SHIFT über einem Handelsangebot: Details","SHIFT over a trade offer: details"),x+10,y+108,0xFF9AC4D2,false);
    }
    @Override public boolean isPauseScreen(){return false;}
}
