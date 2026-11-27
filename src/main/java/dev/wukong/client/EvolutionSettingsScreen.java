package dev.wukong.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

final class EvolutionSettingsScreen extends Screen {
    private final Screen parent; private int x,w;
    EvolutionSettingsScreen(Screen parent){super(Component.literal("WuKong / Client"));this.parent=parent;}
    private static String tr(String de,String en){return ModuleCatalog.tr(de,en);}
    private void open(Screen s){Minecraft.getInstance().gui.setScreen(s);}
    @Override protected void init(){w=Math.min(400,width-12);x=(width-w)/2;int h=Math.max(14,Math.min(22,(height-57)/7)),y=31;
        button(tr("Theme","Theme")+": "+EvolutionUi.theme(),y,h,b->{EvolutionUi.nextTheme();b.setMessage(Component.literal(UiLabels.t("Theme: ")+EvolutionUi.theme()));});y+=h;
        button(tr("Automatische Updatesuche","Automatic update check")+": "+(EvolutionUi.autoCheck()?UiLabels.t("ON"):UiLabels.t("OFF")),y,h,b->{EvolutionUi.toggleAutoCheck();b.setMessage(Component.literal(tr("Automatische Updatesuche","Automatic update check")+": "+(EvolutionUi.autoCheck()?UiLabels.t("ON"):UiLabels.t("OFF"))));});y+=h;
        button(tr("GitHub-Releases pr\u00FCfen","Check GitHub releases"),y,h,b->EvolutionUi.checkNow());y+=h;
        button(tr("Benachrichtigungen","Notification Center"),y,h,b->open(new NotificationScreen(this)));y+=h;
        button(tr("Tastenbelegung","Keybinds"),y,h,b->open(new KeyBindsScreen(this,Minecraft.getInstance().options)));y+=h;
        button(tr("Extras / Hilfe / Sprache","Extras / Help / Language"),y,h,b->open(new ExtrasScreen()));y+=h;
        button(tr("Zur\u00FCck","Back"),y,h,b->onClose());
    }
    private void button(String text,int y,int h,Button.OnPress click){addRenderableWidget(Button.builder(Component.literal(text),click).bounds(x+8,y,w-16,h-2).build());}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float p){
        g.fill(x,4,x+w,height-3,EvolutionUi.panel());g.fill(x,4,x+w,6,EvolutionUi.accent());
        g.text(font,"WUKONG / CLIENT "+EvolutionUi.installed(),x+8,9,EvolutionUi.accent(),true);
        g.text(font,font.plainSubstrByWidth(UiLabels.message(EvolutionUi.updateStatus()),w-16),x+8,20,0xFFCDD9E3,false);super.extractRenderState(g,mx,my,p);
    }
    @Override public void onClose(){open(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
