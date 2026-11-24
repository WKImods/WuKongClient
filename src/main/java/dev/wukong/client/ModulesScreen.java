package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

/** Utility/HUD modules; ESP groups have separate settings pages. */
public final class ModulesScreen extends Screen {
    private final BenchConfig cfg=BenchConfig.INSTANCE;
    private int x,y,w;
    private interface Getter{boolean get();}
    private interface Setter{void set(boolean v);}
    public ModulesScreen(int ignoredPage){super(Component.literal("WuKong Utilities"));}
    @Override protected void init(){
        w=Math.min(405,width-16);x=(width-w)/2;y=(height-307)/2;
        toggle(y+36,"Fullbright (world lightmap)",()->cfg.fullbrightEnabled,v->cfg.fullbrightEnabled=v);
        toggle(y+64,"Waypoint world icons",()->cfg.waypointsEnabled,v->cfg.waypointsEnabled=v);
        toggle(y+92,"Death marker world icons",()->cfg.deathMarkerEnabled,v->cfg.deathMarkerEnabled=v);
        toggle(y+120,"Inventory HUD",()->cfg.inventoryHudEnabled,v->cfg.inventoryHudEnabled=v);
        addRenderableWidget(Button.builder(Component.literal("Manage waypoints / death markers..."),b->
                Minecraft.getInstance().gui.setScreen(new WaypointListScreen()))
                .bounds(x+10,y+160,w-20,20).build());
        addRenderableWidget(Button.builder(Component.literal("New waypoint (name, coords, icon)..."),b->
                Minecraft.getInstance().gui.setScreen(new WaypointEditScreen(null)))
                .bounds(x+10,y+188,w-20,20).build());
        int c=(w-30)/2;
        addRenderableWidget(Button.builder(Component.literal("Keybinds..."),b->
                Minecraft.getInstance().gui.setScreen(new KeyBindsScreen(this,Minecraft.getInstance().options)))
                .bounds(x+10,y+236,c,20).build());
        addRenderableWidget(Button.builder(Component.literal("Back to ESP"),b->
                Minecraft.getInstance().gui.setScreen(new EspScreen()))
                .bounds(x+20+c,y+236,c,20).build());
    }
    private void toggle(int yy,String label,Getter get,Setter set){
        addRenderableWidget(Button.builder(Component.literal(label+": "+(get.get()?"ON":"OFF")),b->{
            set.set(!get.get());cfg.save();
            b.setMessage(Component.literal(label+": "+(get.get()?"ON":"OFF")));
        }).bounds(x+10,yy,w-20,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        g.fill(x,y,x+w,y+307,0xE5111A24);
        g.fill(x,y,x+w,y+26,0xFF204051);
        g.text(font,"WUKONG  /  HUD & UTILITIES",x+10,y+9,0xFFFFFFFF,false);
        super.extractRenderState(g,mx,my,partial);
        g.text(font,"WAYPOINTS  /  WORLD ICONS",x+10,y+150,0xFF9AC4D2,false);
        g.text(font,"F7 ESP Master hides waypoint world icons",x+10,y+292,0xFF9AC4D2,false);
    }
    @Override public void onClose(){cfg.save();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
