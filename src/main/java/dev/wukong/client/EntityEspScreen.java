package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Dedicated entity rendering controls; never changes Ore Finder styles. */
public final class EntityEspScreen extends Screen {
    private final BenchConfig cfg=BenchConfig.INSTANCE;
    private int x,y,w;
    private interface Reader { boolean get(); }
    private interface Writer { void set(boolean value); }
    public EntityEspScreen(){ super(Component.literal(UiLabels.t("WuKong Entity ESP"))); }
    @Override protected void init() {
        w=Math.min(400,width-16);x=(width-w)/2;y=(height-286)/2;
        toggle(y+34,UiLabels.t("Player ESP"),()->cfg.players,v->cfg.players=v);
        toggle(y+62,UiLabels.t("Mob ESP [")+WuKongKeys.MOBS.getTranslatedKeyMessage().getString()+"]",
                ()->cfg.mobEspEnabled,v->cfg.mobEspEnabled=v);
        toggle(y+90,UiLabels.t("Hostile mobs"),()->cfg.hostile,v->cfg.hostile=v);
        toggle(y+118,UiLabels.t("Passive mobs"),()->cfg.passive,v->cfg.passive=v);
        toggle(y+152,UiLabels.t("Mob wireframe boxes"),()->cfg.mobEspBoxes,v->cfg.mobEspBoxes=v);
        toggle(y+180,UiLabels.t("Mob names / distances"),()->cfg.mobEspNames,v->cfg.mobEspNames=v);
        toggle(y+208,UiLabels.t("Enhanced nametags"),()->cfg.enhancedNametags,v->cfg.enhancedNametags=v);
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Back to ESP dashboard")),b->{
            cfg.save();Minecraft.getInstance().gui.setScreen(new EspScreen());
        }).bounds(x+10,y+250,w-20,20).build());
    }
    private void toggle(int yy,String label,Reader get,Writer set) {
        addRenderableWidget(Button.builder(Component.literal(label+": "+(get.get()?UiLabels.t("ON"):UiLabels.t("OFF"))),b->{
            set.set(!get.get());cfg.save();
            b.setMessage(Component.literal(label+": "+(get.get()?UiLabels.t("ON"):UiLabels.t("OFF"))));
        }).bounds(x+10,yy,w-20,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial) {
        g.fill(x,y,x+w,y+286,0xE5111A24);
        g.fill(x,y,x+w,y+26,0xFF204051);
        g.text(font,UiLabels.t("WUKONG  /  ENTITY ESP"),x+10,y+9,0xFFFFFFFF,false);
        super.extractRenderState(g,mx,my,partial);
        g.text(font,UiLabels.t("MOB VISUALS"),x+10,y+140,0xFF9AC4D2,false);
        g.text(font,UiLabels.t("Mob OFF hides mob markers (Ore ESP stays visible)"),x+10,y+237,0xFF9AC4D2,false);
    }
    @Override public void onClose(){cfg.save();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
