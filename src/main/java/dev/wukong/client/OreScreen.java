package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Ore visuals are fully independent of entity ESP boxes/names. */
public final class OreScreen extends Screen {
    private final BenchConfig cfg=BenchConfig.INSTANCE;
    private int x,y,w;
    private Button rangeLabel;
    private interface Reader{boolean get();}
    private interface Writer{void set(boolean v);}
    public OreScreen(){super(Component.literal(UiLabels.t("WuKong Ore Finder")));}
    @Override protected void init() {
        w=Math.min(395,width-16);x=(width-w)/2;y=(height-271)/2;
        toggle(y+35,UiLabels.t("Ore Finder [")+WuKongKeys.ORE.getTranslatedKeyMessage().getString()+"]",
                ()->cfg.oreFinderEnabled,v->cfg.oreFinderEnabled=v);
        toggle(y+63,UiLabels.t("Ore vein boxes"),()->cfg.oreEspBoxes,v->cfg.oreEspBoxes=v);
        toggle(y+91,UiLabels.t("Ore names + amounts"),()->cfg.oreEspNames,v->cfg.oreEspNames=v);
        toggle(y+119,UiLabels.t("Individual ore dots"),()->cfg.oreDotsEnabled,v->cfg.oreDotsEnabled=v);
        int row=y+155;
        addRenderableWidget(Button.builder(Component.literal("-8"),b->adjustRange(-8))
                .bounds(x+10,row,60,20).build());
        rangeLabel=addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Ore range: ")+cfg.oreRange),b->{})
                .bounds(x+77,row,w-154,20).build());
        addRenderableWidget(Button.builder(Component.literal("+8"),b->adjustRange(8))
                .bounds(x+w-70,row,60,20).build());
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Select ore types...")),b->{
            cfg.save();Minecraft.getInstance().gui.setScreen(new OreFilterScreen());
        }).bounds(x+10,y+188,w-20,20).build());
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Back to ESP dashboard")),b->{
            cfg.save();Minecraft.getInstance().gui.setScreen(new EspScreen());
        }).bounds(x+10,y+226,w-20,20).build());
    }
    private void adjustRange(int delta){cfg.oreRange=Math.max(16,Math.min(64,cfg.oreRange+delta));cfg.save();
        rangeLabel.setMessage(Component.literal(UiLabels.t("Ore range: ")+cfg.oreRange));}
    private void toggle(int yy,String label,Reader get,Writer set) {
        addRenderableWidget(Button.builder(Component.literal(label+": "+(get.get()?UiLabels.t("ON"):UiLabels.t("OFF"))),b->{
            set.set(!get.get());cfg.save();
            b.setMessage(Component.literal(label+": "+(get.get()?UiLabels.t("ON"):UiLabels.t("OFF"))));
        }).bounds(x+10,yy,w-20,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial) {
        g.fill(x,y,x+w,y+271,0xE5111A24);
        g.fill(x,y,x+w,y+26,0xFF204051);
        g.text(font,UiLabels.t("WUKONG  /  ORE ESP"),x+10,y+9,0xFFFFFFFF,false);
        super.extractRenderState(g,mx,my,partial);
        g.text(font,UiLabels.t("Ore visuals ignore Mob / World box and name toggles"),x+10,y+215,0xFF9AC4D2,false);
        g.text(font,UiLabels.t("F7 master still controls all ESP modules"),x+10,y+258,0xFF9AC4D2,false);
    }
    @Override public void onClose(){cfg.save();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
