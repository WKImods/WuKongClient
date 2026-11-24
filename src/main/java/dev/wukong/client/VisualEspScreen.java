package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shared style for players, item and storage; independent of ore and mob boxes/names. */
public final class VisualEspScreen extends Screen {
    private final BenchConfig cfg=BenchConfig.INSTANCE;
    private int x,y,w;
    private Button rangeLabel;
    private interface Reader{boolean get();}
    private interface Writer{void set(boolean value);}
    public VisualEspScreen(){super(Component.literal("WuKong ESP Visuals"));}
    @Override protected void init() {
        w=Math.min(395,width-16);x=(width-w)/2;y=(height-282)/2;
        toggle(y+36,"World / Player boxes",()->cfg.espBoxes,v->cfg.espBoxes=v);
        toggle(y+64,"World / Player names",()->cfg.espNames,v->cfg.espNames=v);
        toggle(y+92,"Tracers (all ESP modules)",()->cfg.espTracers,v->cfg.espTracers=v);
        toggle(y+120,"Distance labels",()->cfg.distanceLabels,v->cfg.distanceLabels=v);
        toggle(y+148,"Through Walls",()->cfg.espThroughWalls,v->cfg.espThroughWalls=v);
        int row=y+184;
        addRenderableWidget(Button.builder(Component.literal("-16"),b->adjust(-16))
                .bounds(x+10,row,65,20).build());
        rangeLabel=addRenderableWidget(Button.builder(Component.literal("World ESP range: "+cfg.range),b->{})
                .bounds(x+82,row,w-164,20).build());
        addRenderableWidget(Button.builder(Component.literal("+16"),b->adjust(16))
                .bounds(x+w-75,row,65,20).build());
        addRenderableWidget(Button.builder(Component.literal("Back to ESP dashboard"),b->{
            cfg.save();Minecraft.getInstance().gui.setScreen(new EspScreen());
        }).bounds(x+10,y+239,w-20,20).build());
    }
    private void adjust(int delta){cfg.range=Math.max(16,Math.min(128,cfg.range+delta));cfg.save();
        rangeLabel.setMessage(Component.literal("World ESP range: "+cfg.range));}
    private void toggle(int yy,String label,Reader get,Writer set) {
        addRenderableWidget(Button.builder(Component.literal(label+": "+(get.get()?"ON":"OFF")),b->{
            set.set(!get.get());cfg.save();
            b.setMessage(Component.literal(label+": "+(get.get()?"ON":"OFF")));
        }).bounds(x+10,yy,w-20,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial) {
        g.fill(x,y,x+w,y+282,0xE5111A24);
        g.fill(x,y,x+w,y+26,0xFF204051);
        g.text(font,"WUKONG  /  VISUAL SETTINGS",x+10,y+9,0xFFFFFFFF,false);
        super.extractRenderState(g,mx,my,partial);
        g.text(font,"Ore / Mob box & name styles are configured separately",x+10,y+225,0xFF9AC4D2,false);
    }
    @Override public void onClose(){cfg.save();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
