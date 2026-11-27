package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Small non-pausing waypoint editor opened from a keybind or the waypoint list. */
public final class WaypointEditScreen extends Screen {
    private final BenchConfig.Waypoint editing;
    private final boolean quickMode;
    private EditBox nameField,xField,yField,zField;
    private String dimension, icon;
    private boolean visible;
    private String message="";
    private int x,y,w,h;
    public WaypointEditScreen(BenchConfig.Waypoint existing) { this(existing, false); }
    public WaypointEditScreen(BenchConfig.Waypoint existing, boolean quickMode) {
        super(Component.literal(existing==null ? UiLabels.t("Create waypoint") : UiLabels.t("Edit waypoint")));
        this.editing=existing;
        this.quickMode=quickMode;
    }
    @Override protected void init() {
        Minecraft mc=Minecraft.getInstance();
        BenchConfig.Waypoint draft;
        if (editing==null) {
            if (mc.player==null || mc.level==null) { mc.gui.setScreen(new WaypointListScreen()); return; }
            draft=WaypointManager.newAtPlayer(mc);
        } else draft=editing;
        dimension=draft.dimension;
        icon=WaypointIcons.isValid(draft.icon) ? draft.icon : "PIN";
        visible=draft.showInWorld;
        w=Math.min(326,width-14); h=252; x=(width-w)/2; y=Math.max(4,(height-h)/2);
        int left=x+14, rw=w-28;
        nameField=field(left,y+48,rw, "Name", draft.name, 44);
        int third=(rw-12)/3;
        xField=field(left,y+98,third,"X",Integer.toString(draft.x),11);
        yField=field(left+third+6,y+98,third,"Y",Integer.toString(draft.y),11);
        zField=field(left+2*(third+6),y+98,third,"Z",Integer.toString(draft.z),11);
        int half=(rw-6)/2;
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Icon: ") + WaypointIcons.glyph(icon) + "  " + icon),b->{
            icon=WaypointIcons.next(icon);
            b.setMessage(Component.literal(UiLabels.t("Icon: ") + WaypointIcons.glyph(icon) + "  " + icon));
        }).bounds(left,y+137,half,20).build());
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("World icon: ")+(visible?UiLabels.t("ON"):UiLabels.t("OFF"))),b->{
            visible=!visible;b.setMessage(Component.literal(UiLabels.t("World icon: ")+(visible?UiLabels.t("ON"):UiLabels.t("OFF"))));
        }).bounds(left+half+6,y+137,half,20).build());
        addRenderableWidget(Button.builder(Component.literal(editing==null?UiLabels.t("Create waypoint"):UiLabels.t("Save changes")),b->save())
                .bounds(left,y+198,half,20).build());
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Cancel")),b->mc.gui.setScreen(quickMode ? null : new WaypointListScreen()))
                .bounds(left+half+6,y+198,half,20).build());
    }
    private EditBox field(int xx,int yy,int ww,String label,String value,int length) {
        EditBox e=new EditBox(font,xx,yy,ww,20,Component.literal(label));
        e.setMaxLength(length); e.setValue(value==null?"":value);
        return addRenderableWidget(e);
    }
    private void save(){
        String name=nameField.getValue().trim();
        if(name.isEmpty()) { message=UiLabels.t("Please enter a name.");return; }
        int xx,yy,zz;
        try {
            xx=Integer.parseInt(xField.getValue().trim());
            yy=Integer.parseInt(yField.getValue().trim());
            zz=Integer.parseInt(zField.getValue().trim());
            if(Math.abs((long)xx)>30000000 || Math.abs((long)zz)>30000000 || Math.abs((long)yy)>4096)
                throw new NumberFormatException(UiLabels.t("Out of bounds"));
        } catch(NumberFormatException ex){ message=UiLabels.t("Invalid coordinates.");return; }
        BenchConfig.Waypoint wp=editing==null
                ? new BenchConfig.Waypoint(name,dimension,xx,yy,zz) : editing;
        wp.name=name;wp.dimension=dimension;wp.x=xx;wp.y=yy;wp.z=zz;
        wp.icon=icon;wp.showInWorld=visible;
        if(editing==null) {
            WaypointManager.add(wp);
            BenchConfig.INSTANCE.waypointsEnabled=true;
        }
        BenchConfig.INSTANCE.save();
        Minecraft.getInstance().gui.setScreen(quickMode ? null : new WaypointListScreen());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        g.fill(x,y,x+w,y+h,0xF1161E29);
        g.fill(x,y,x+w,y+30,0xFF254553);
        g.fill(x,y+29,x+w,y+31,0xFF5BDCE7);
        g.text(font,editing==null?UiLabels.t("WUKONG / CREATE WAYPOINT"):UiLabels.t("WUKONG / EDIT WAYPOINT"),x+12,y+11,0xFFFFFFFF,false);
        g.text(font,"Name",x+14,y+37,0xFFC3D8E3,false);
        g.text(font,UiLabels.t("Coordinates (X / Y / Z)"),x+14,y+86,0xFFC3D8E3,false);
        g.text(font,"Dimension: " + dimension,x+14,y+171,0xFF9CC3D4,false);
        if(!message.isBlank()) g.text(font,message,x+14,y+184,0xFFFFA6A6,false);
        super.extractRenderState(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){Minecraft.getInstance().gui.setScreen(quickMode ? null : new WaypointListScreen());}
}
