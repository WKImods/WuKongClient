package dev.wukong.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Session-only messages: toggles, release lookups and configuration recovery. */
final class NotificationScreen extends Screen {
    private final Screen parent; private int page,x,w,rows;
    NotificationScreen(Screen parent){super(Component.literal(UiLabels.t("Notification Center")));this.parent=parent;}
    @Override protected void init(){w=Math.min(500,width-12);x=(width-w)/2;rows=Math.max(1,(height-62)/22);int y=height-25,unit=(w-24)/4;
        addRenderableWidget(Button.builder(Component.literal("<"),b->page=Math.max(0,page-1)).bounds(x+6,y,unit,20).build());
        addRenderableWidget(Button.builder(Component.literal(">"),b->page=Math.min(Math.max(0,(EvolutionUi.notifications().size()-1)/rows),page+1)).bounds(x+10+unit,y,unit,20).build());
        addRenderableWidget(Button.builder(Component.literal(ModuleCatalog.tr("Leeren","Clear")),b->{EvolutionUi.clearNotifications();page=0;}).bounds(x+14+unit*2,y,unit,20).build());
        addRenderableWidget(Button.builder(Component.literal(ModuleCatalog.tr("Zur\u00FCck","Back")),b->onClose()).bounds(x+18+unit*3,y,unit,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float p){
        g.fill(x,4,x+w,height-3,EvolutionUi.panel());g.fill(x,4,x+w,6,EvolutionUi.accent());
        g.text(font,UiLabels.t("WUKONG / NOTIFICATIONS"),x+8,10,EvolutionUi.accent(),true);var list=EvolutionUi.notifications();page=Math.max(0,Math.min(page,Math.max(0,(list.size()-1)/rows)));
        if(list.isEmpty())g.text(font,ModuleCatalog.tr("Keine Meldungen","No notifications"),x+8,35,0xFFCDD9E3,false);
        for(int i=0;i<rows&&page*rows+i<list.size();i++){int y=28+i*22;String s=list.get(page*rows+i);g.fill(x+6,y,x+w-6,y+20,0xFF263440);g.text(font,font.plainSubstrByWidth(UiLabels.message(s),w-22),x+11,y+6,0xFFE3EDF5,false);}
        g.text(font,(page+1)+" / "+Math.max(1,(list.size()+rows-1)/rows),x+8,height-37,0xFFCDD9E3,false);super.extractRenderState(g,mx,my,p);
    }
    @Override public void onClose(){Minecraft.getInstance().gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
