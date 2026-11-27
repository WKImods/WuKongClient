package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Fine-grained toggles for each OreKind, saved in wukong.json. */
public final class OreFilterScreen extends Screen {
    private final BenchConfig cfg=BenchConfig.INSTANCE;
    private int x,y,w;
    public OreFilterScreen(){super(Component.literal(UiLabels.t("WuKong Ore Filters")));}
    @Override protected void init(){
        w=Math.min(420,width-16);x=(width-w)/2;y=(height-293)/2;
        OreKind[] kinds=OreKind.values();
        for (int i=0;i<kinds.length;i++) {
            OreKind kind=kinds[i];
            int col=i%2,row=i/2;
            int xx=x+10+col*((w-30)/2+10),yy=y+35+row*30;
            int ww=(w-30)/2;
            addRenderableWidget(Button.builder(Component.literal(kind.displayName+" : "+(cfg.oreVisible(kind)?UiLabels.t("ON"):UiLabels.t("OFF"))),b->{
                cfg.setOreVisible(kind,!cfg.oreVisible(kind));cfg.save();
                b.setMessage(Component.literal(kind.displayName+" : "+(cfg.oreVisible(kind)?UiLabels.t("ON"):UiLabels.t("OFF"))));
            }).bounds(xx,yy,ww,20).build());
        }
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Enable all")),b->{
            for(OreKind kind:OreKind.values())cfg.setOreVisible(kind,true);cfg.save();
            Minecraft.getInstance().gui.setScreen(new OreFilterScreen());
        }).bounds(x+10,y+198,(w-30)/2,20).build());
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Disable all")),b->{
            for(OreKind kind:OreKind.values())cfg.setOreVisible(kind,false);cfg.save();
            Minecraft.getInstance().gui.setScreen(new OreFilterScreen());
        }).bounds(x+20+(w-30)/2,y+198,(w-30)/2,20).build());
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Back to Ore Finder")),b->
            Minecraft.getInstance().gui.setScreen(new OreScreen()))
            .bounds(x+10,y+235,w-20,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        g.fill(x,y,x+w,y+293,0xE5111A24);g.fill(x,y,x+w,y+26,0xFF204051);
        g.text(font,UiLabels.t("WUKONG / ORE FILTERS"),x+10,y+9,0xFFFFFFFF,false);
        super.extractRenderState(g,mx,my,partial);
        g.text(font,UiLabels.t("Colored boxes show selected ore types only"),x+10,y+270,0xFF9AC4D2,false);
        g.text(font,UiLabels.t("Ore hotkeys: Controls -> Key Binds -> WuKong"),x+10,y+282,0xFF9AC4D2,false);
    }
    @Override public void onClose(){cfg.save();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
