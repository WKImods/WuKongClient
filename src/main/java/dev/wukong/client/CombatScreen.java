package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compact two-tab combat controls; the old home screen no longer overlaps sliders. */
public final class CombatScreen extends Screen {
    private final BenchConfig cfg = BenchConfig.INSTANCE;
    private final int tab;
    private int x,y,w,h;
    private static final int AMBER = 0xFFFFB965;
    private static final int CYAN = 0xFF65DBE7;

    public CombatScreen() { this(0); }
    public CombatScreen(int tab) {
        super(Component.literal("WuKong / Combat"));
        this.tab = tab;
    }

    @Override protected void init() {
        w = Math.min(472, width - 20);
        h = 378;
        x = (width-w)/2;
        y = Math.max(6, (height-h)/2);
        int half = (w-30)/2;
        page(x+10,y+38,half,"AIM / TARGETS",0);
        page(x+20+half,y+38,half,"BOW / CONTROL",1);
        if (tab == 0) aimPage(); else bowPage();
        addRenderableWidget(Button.builder(Component.literal("Back to WuKong"), b -> {
            cfg.save(); Minecraft.getInstance().gui.setScreen(new BenchScreen());
        }).bounds(x+10,y+342,w-20,21).build());
    }

    private void page(int xx,int yy,int ww,String text,int targetTab) {
        addRenderableWidget(Button.builder(Component.literal((targetTab==tab?"[ ":"  ")+text+(targetTab==tab?" ]":"")), b -> {
            cfg.save(); Minecraft.getInstance().gui.setScreen(new CombatScreen(targetTab));
        }).bounds(xx,yy,ww,21).build());
    }

    private interface Flag { boolean get(); }
    private interface Toggle { void set(boolean value); }
    private void toggle(int yy,String title, Flag get, Toggle set) {
        addRenderableWidget(Button.builder(Component.literal(title+" : "+(get.get()?"ON":"OFF")), b -> {
            set.set(!get.get());cfg.save();
            b.setMessage(Component.literal(title+" : "+(get.get()?"ON":"OFF")));
        }).bounds(x+12,yy,w-24,20).build());
    }
    private interface StringVal { String get(); }
    private void adjust(int yy,String title,StringVal val,Runnable down,Runnable up) {
        int side=Math.min(70,(w-32)/5);
        int mid=w-32-2*side-8;
        addRenderableWidget(Button.builder(Component.literal("-"), b->{down.run();cfg.save();refresh();})
                .bounds(x+12,yy,side,20).build());
        addRenderableWidget(Button.builder(Component.literal(title+" : "+val.get()),b->{})
                .bounds(x+16+side,yy,mid,20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b->{up.run();cfg.save();refresh();})
                .bounds(x+20+side+mid,yy,side,20).build());
    }
    private void refresh(){
        Minecraft.getInstance().gui.setScreen(new CombatScreen(tab));
    }
    private void cycle(int yy, String title, StringVal label, Runnable onClick) {
        addRenderableWidget(Button.builder(Component.literal(title+" : "+label.get()),b->{
            onClick.run();cfg.save();b.setMessage(Component.literal(title+" : "+label.get()));
        }).bounds(x+12,yy,w-24,20).build());
    }
    private void aimPage() {
        int row=y+90;
        toggle(row,"Aim Assist",()->cfg.aimAssist,v->cfg.aimAssist=v);row+=26;
        toggle(row,"Aim players",()->cfg.aimPlayers,v->cfg.aimPlayers=v);row+=26;
        toggle(row,"Aim hostile mobs",()->cfg.aimHostile,v->cfg.aimHostile=v);row+=26;
        toggle(row,"Aim passive mobs",()->cfg.aimPassive,v->cfg.aimPassive=v);row+=26;
        toggle(row,"Visibility check",()->cfg.aimRequireVisible,v->cfg.aimRequireVisible=v);row+=40;
        adjust(row,"Aim strength",()->Math.round(cfg.aimAggression*100)+"%",
                ()->cfg.aimAggression=clamp(cfg.aimAggression-0.1,0.1,1.0),
                ()->cfg.aimAggression=clamp(cfg.aimAggression+0.1,0.1,1.0));row+=28;
        adjust(row,"FOV",()->Math.round(cfg.aimFov)+" degrees",
                ()->cfg.aimFov=clamp(cfg.aimFov-5,2,90),
                ()->cfg.aimFov=clamp(cfg.aimFov+5,2,90));row+=28;
        adjust(row,"Aim range",()->cfg.aimRange+" blocks",
                ()->cfg.aimRange=Math.max(4,cfg.aimRange-4),
                ()->cfg.aimRange=Math.min(128,cfg.aimRange+4));
    }
    private void bowPage() {
        int row=y+90;
        toggle(row,"Bow prediction",()->cfg.aimPrediction,v->cfg.aimPrediction=v);row+=28;
        toggle(row,"Separate Camera",()->cfg.separateBowCamera,v->cfg.separateBowCamera=v);row+=28;
        toggle(row,"Steady Bow View",()->cfg.steadyBowView,v->cfg.steadyBowView=v);row+=28;
        toggle(row,"Auto release bow",()->cfg.aimTriggerBow,v->cfg.aimTriggerBow=v);row+=40;
        cycle(row,"Aim mode",()->switch(cfg.aimType){
            case "PRECISION" -> "Precision";
            case "FOCUS" -> "Focus";
            case "MELEE" -> "Melee";
            default -> "Standard";
        },()->{
            String[] modes={"STANDARD","PRECISION","FOCUS","MELEE"};
            int idx=java.util.Arrays.asList(modes).indexOf(cfg.aimType);
            cfg.aimType=modes[(idx+1)%modes.length];
        });row+=27;
        cycle(row,"Aim activation ["+WuKongKeys.AIM.getTranslatedKeyMessage().getString()+"]",
                ()->"TOGGLE".equals(cfg.aimActivationMode)?"Toggle":"Hold",
                ()->cfg.aimActivationMode="TOGGLE".equals(cfg.aimActivationMode)?"HOLD":"TOGGLE");row+=27;
        adjust(row,"Bow charge",()->cfg.aimBowMinChargeTicks+" ticks",
                ()->cfg.aimBowMinChargeTicks=Math.max(3,cfg.aimBowMinChargeTicks-2),
                ()->cfg.aimBowMinChargeTicks=Math.min(40,cfg.aimBowMinChargeTicks+2));row+=30;
        addRenderableWidget(Button.builder(Component.literal("Target priority / Aim Spot / Combat rules  >"), b -> {
            cfg.save();Minecraft.getInstance().gui.setScreen(new TargetRulesScreen());
        }).bounds(x+12,row,w-24,20).build());
    }
    private static double clamp(double d,double min,double max){return Math.max(min,Math.min(max,Math.round(d*100)/100.0));}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial) {
        g.fill(x,y,x+w,y+h,0xF0101924);
        g.fill(x,y,x+w,y+30,0xFF302B26);
        g.fill(x,y+28,x+w,y+30,AMBER);
        g.text(font,"WUKONG  /  COMBAT SETTINGS",x+12,y+10,0xFFFFFFFF,false);
        g.fill(x+8,y+69,x+w-8,y+328,0x77232B37);
        g.fill(x+9,y+76,x+12,y+326,AMBER);
        g.text(font,tab==0?"AIM FILTERS  /  RESPONSE":"BOW PREDICTION  /  CAMERA",
                x+20,y+74,tab==0?AMBER:CYAN,false);
        if(tab==0) g.text(font,"TRACKING",x+20,y+216,AMBER,false);
        else g.text(font,"CONTROL / ADVANCED",x+20,y+206,AMBER,false);
        super.extractRenderState(g,mx,my,partial);
        g.text(font,"ESC / BACK",x+12,y+h-7,0xFF9ABACB,false);
    }
    @Override public void onClose(){cfg.save();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
