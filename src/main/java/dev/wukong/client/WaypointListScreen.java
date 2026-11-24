package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Full manager: every death marker is editable, individually hideable and deletable. */
public final class WaypointListScreen extends Screen {
    private final BenchConfig cfg=BenchConfig.INSTANCE;
    private int page, x,y,w,h,rowCount;
    public WaypointListScreen(){super(Component.literal("WuKong waypoints"));}
    @Override protected void init() {
        w=Math.min(490,width-12);h=Math.min(316,height-12);x=(width-w)/2;y=(height-h)/2;
        rowCount=Math.max(1,Math.min(6,(h-104)/31));
        int maxPage=(Math.max(0,cfg.waypoints.size()-1))/rowCount;
        page=Math.min(page,maxPage);
        int rowY=y+50;
        for(int i=page*rowCount;i<cfg.waypoints.size() && i<(page+1)*rowCount;i++){
            final BenchConfig.Waypoint wp=cfg.waypoints.get(i);
            int ry=rowY+(i-page*rowCount)*31;
            int editW=46,iconW=59,deleteW=24;
            int right=x+w-12;
            addRenderableWidget(Button.builder(Component.literal("Edit"),b->Minecraft.getInstance().gui.setScreen(new WaypointEditScreen(wp)))
                    .bounds(right-(editW+iconW+deleteW+8),ry,editW,20).build());
            addRenderableWidget(Button.builder(Component.literal(wp.showInWorld?"Icon ON":"Icon OFF"),b->{
                wp.showInWorld=!wp.showInWorld;cfg.save();
                b.setMessage(Component.literal(wp.showInWorld?"Icon ON":"Icon OFF"));
            }).bounds(right-(iconW+deleteW+4),ry,iconW,20).build());
            addRenderableWidget(Button.builder(Component.literal("X"),b->{WaypointManager.remove(wp);rebuild();})
                    .bounds(right-deleteW,ry,deleteW,20).build());
        }
        int footer=y+h-33;
        int side=52,small=78;
        addRenderableWidget(Button.builder(Component.literal("< Prev"),b->{page=Math.max(0,page-1);rebuild();})
            .bounds(x+12,footer,side,20).build());
        addRenderableWidget(Button.builder(Component.literal("Next >"),b->{page++;rebuild();})
            .bounds(x+70,footer,side+4,20).build());
        addRenderableWidget(Button.builder(Component.literal("+ New"),b->Minecraft.getInstance().gui.setScreen(new WaypointEditScreen(null)))
            .bounds(x+w-188,footer,small,20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"),b->Minecraft.getInstance().gui.setScreen(new ModulesScreen(1)))
            .bounds(x+w-101,footer,88,20).build());
    }
    private void rebuild(){clearWidgets();init();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        g.fill(x,y,x+w,y+h,0xF1161E29);
        g.fill(x,y,x+w,y+30,0xFF254553);
        g.fill(x,y+29,x+w,y+31,0xFF5BDCE7);
        g.text(font,"WUKONG  /  WAYPOINT MANAGER",x+12,y+11,0xFFFFFFFF,false);
        g.text(font,"Icon | Name / Coordinates                 Edit    World Icon   Delete",x+12,y+35,0xFF9BBECB,false);
        int right=x+w-12;
        for(int i=page*rowCount;i<cfg.waypoints.size() && i<(page+1)*rowCount;i++){
            BenchConfig.Waypoint wp=cfg.waypoints.get(i);
            int ry=y+50+(i-page*rowCount)*31;
            g.fill(x+9,ry-3,x+w-9,ry+25,0x88314554);
            g.fill(x+9,ry-3,x+12,ry+25,WaypointIcons.color(wp.icon));
            String name=(wp.name==null?"Waypoint":wp.name);
            if(name.length()>22) name=name.substring(0,19)+"...";
            String label=WaypointIcons.glyph(wp.icon)+" "+name+(wp.death?" [DEATH]":"");
            g.text(font,label,x+18,ry-1,WaypointIcons.color(wp.icon),false);
            g.text(font,wp.x+", "+wp.y+", "+wp.z+" | "+wp.dimension,x+18,ry+11,0xFFB0C1CC,false);
        }
        super.extractRenderState(g,mx,my,partial);
        g.text(font,(cfg.waypoints.size())+" stored  |  page "+(page+1)+" / "+Math.max(1,(cfg.waypoints.size()+rowCount-1)/rowCount),
                x+12,y+h-46,0xFF9BBECB,false);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){cfg.save();Minecraft.getInstance().gui.setScreen(new ModulesScreen(1));}
}
