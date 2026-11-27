package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** World object modules separated from mobs and ore veins. */
public final class WorldEspScreen extends Screen {
    private final BenchConfig cfg=BenchConfig.INSTANCE;
    private int x,y,w;
    private interface Reader{boolean get();}
    private interface Writer{void set(boolean value);}
    public WorldEspScreen(){super(Component.literal(UiLabels.t("WuKong World ESP")));}
    @Override protected void init() {
        w=Math.min(395,width-16);x=(width-w)/2;y=(height-230)/2;
        toggle(y+37,UiLabels.t("Storage ESP"),()->cfg.containers,v->cfg.containers=v);
        toggle(y+65,UiLabels.t("Storage ESP+ colors"),()->cfg.storageEspPlus,v->cfg.storageEspPlus=v);
        toggle(y+93,UiLabels.t("Dropped Item ESP"),()->cfg.itemEspEnabled,v->cfg.itemEspEnabled=v);
        toggle(y+121,UiLabels.t("Spawner Finder"),()->cfg.spawnerEspEnabled,v->cfg.spawnerEspEnabled=v);
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("World ESP styles / range...")),b->{
            cfg.save();Minecraft.getInstance().gui.setScreen(new VisualEspScreen());
        }).bounds(x+10,y+158,w-20,20).build());
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Back to ESP dashboard")),b->{
            cfg.save();Minecraft.getInstance().gui.setScreen(new EspScreen());
        }).bounds(x+10,y+191,w-20,20).build());
    }
    private void toggle(int yy,String label,Reader get,Writer set) {
        addRenderableWidget(Button.builder(Component.literal(label+": "+(get.get()?UiLabels.t("ON"):UiLabels.t("OFF"))),b->{
            set.set(!get.get());cfg.save();
            b.setMessage(Component.literal(label+": "+(get.get()?UiLabels.t("ON"):UiLabels.t("OFF"))));
        }).bounds(x+10,yy,w-20,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial) {
        g.fill(x,y,x+w,y+230,0xE5111A24);
        g.fill(x,y,x+w,y+26,0xFF204051);
        g.text(font,UiLabels.t("WUKONG  /  WORLD ESP"),x+10,y+9,0xFFFFFFFF,false);
        super.extractRenderState(g,mx,my,partial);
        g.text(font,UiLabels.t("Boxes & labels: World ESP visual settings"),x+10,y+147,0xFF9AC4D2,false);
        g.text(font,UiLabels.t("Hotkeys: Controls > Key Binds > WuKong"),x+10,y+220,0xFF9AC4D2,false);
    }
    @Override public void onClose(){cfg.save();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
