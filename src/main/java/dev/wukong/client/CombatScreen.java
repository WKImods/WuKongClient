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
    private java.util.List<dev.wukong.client.combat.BowPredictionChecks.Check> checks = java.util.List.of();
    private int x,y,w,h;
    private static final int AMBER = 0xFFFFB965;
    private static final int CYAN = 0xFF65DBE7;

    public CombatScreen() { this(0); }
    public CombatScreen(int tab) {
        super(Component.literal(UiLabels.t("WuKong / Combat")));
        this.tab = tab;
    }

    @Override protected void init() {
        w = Math.min(472, width - 20);
        h = 378;
        x = (width-w)/2;
        y = Math.max(6, (height-h)/2);
        int third = (w-40)/3;
        page(x+10,y+38,third,UiLabels.t("AIM"),0);
        page(x+20+third,y+38,third,UiLabels.t("BOW"),1);
        page(x+30+2*third,y+38,third,UiLabels.t("PREDICTION"),2);
        if (tab == 0) aimPage(); else if(tab==1) bowPage(); else diagnosticsPage();
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Back to WuKong")), b -> {
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
        addRenderableWidget(Button.builder(Component.literal(title+" : "+(get.get()?UiLabels.t("ON"):UiLabels.t("OFF"))), b -> {
            set.set(!get.get());cfg.save();
            b.setMessage(Component.literal(title+" : "+(get.get()?UiLabels.t("ON"):UiLabels.t("OFF"))));
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
        toggle(row,UiLabels.t("Aim Assist"),()->cfg.aimAssist,v->cfg.aimAssist=v);row+=23;
        toggle(row,UiLabels.t("Aim players"),()->cfg.aimPlayers,v->cfg.aimPlayers=v);row+=23;
        toggle(row,UiLabels.t("Aim hostile mobs"),()->cfg.aimHostile,v->cfg.aimHostile=v);row+=23;
        toggle(row,UiLabels.t("Aim passive mobs"),()->cfg.aimPassive,v->cfg.aimPassive=v);row+=23;
        toggle(row,UiLabels.t("Visibility check"),()->cfg.aimRequireVisible,v->cfg.aimRequireVisible=v);row+=36;
        cycle(row,UiLabels.t("Aim response"),()->UiLabels.t(response().label()),()->{
            cfg.aimResponseProfile=response().next().name();
        });row+=24;
        adjust(row,UiLabels.t("Custom strength"),()->Math.round(cfg.aimAggression*100)+"%",
                ()->{cfg.aimAggression=clamp(cfg.aimAggression-0.1,0.1,1.0);cfg.aimResponseProfile="CUSTOM";},
                ()->{cfg.aimAggression=clamp(cfg.aimAggression+0.1,0.1,1.0);cfg.aimResponseProfile="CUSTOM";});row+=24;
        adjust(row,UiLabels.t("FOV"),()->Math.round(cfg.aimFov)+UiLabels.t(" degrees"),
                ()->cfg.aimFov=clamp(cfg.aimFov-5,2,360),
                ()->cfg.aimFov=clamp(cfg.aimFov+5,2,360));row+=24;
        adjust(row,UiLabels.t("Aim range"),()->cfg.aimRange+UiLabels.t(" blocks"),
                ()->cfg.aimRange=Math.max(4,cfg.aimRange-4),
                ()->cfg.aimRange=Math.min(128,cfg.aimRange+4));
    }
    private dev.wukong.client.combat.AimResponse response() {
        return dev.wukong.client.combat.AimResponse.fromId(cfg.aimResponseProfile);
    }
    private void bowPage() {
        int row=y+90;
        toggle(row,UiLabels.t("Bow prediction"),()->cfg.aimPrediction,v->cfg.aimPrediction=v);row+=28;
        toggle(row,UiLabels.t("Separate Camera"),()->cfg.separateBowCamera,v->cfg.separateBowCamera=v);row+=28;
        toggle(row,UiLabels.t("Steady Bow View"),()->cfg.steadyBowView,v->cfg.steadyBowView=v);row+=28;
        toggle(row,UiLabels.t("Auto release bow"),()->cfg.aimTriggerBow,v->cfg.aimTriggerBow=v);row+=40;
        cycle(row,UiLabels.t("Aim mode"),()->switch(cfg.aimType){
            case "PRECISION" -> UiLabels.t("Precision");
            case "FOCUS" -> UiLabels.t("Focus");
            case "MELEE" -> UiLabels.t("Melee");
            default -> "Standard";
        },()->{
            String[] modes={"STANDARD","PRECISION","FOCUS","MELEE"};
            int idx=java.util.Arrays.asList(modes).indexOf(cfg.aimType);
            cfg.aimType=modes[(idx+1)%modes.length];
        });row+=27;
        cycle(row,UiLabels.t("Aim activation [")+WuKongKeys.AIM.getTranslatedKeyMessage().getString()+"]",
                ()->"TOGGLE".equals(cfg.aimActivationMode)?UiLabels.t("Toggle"):UiLabels.t("Hold"),
                ()->cfg.aimActivationMode="TOGGLE".equals(cfg.aimActivationMode)?"HOLD":"TOGGLE");row+=27;
        adjust(row,UiLabels.t("Bow charge"),()->cfg.aimBowMinChargeTicks+UiLabels.t(" ticks"),
                ()->cfg.aimBowMinChargeTicks=Math.max(3,cfg.aimBowMinChargeTicks-2),
                ()->cfg.aimBowMinChargeTicks=Math.min(40,cfg.aimBowMinChargeTicks+2));row+=30;
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Target priority / Aim Spot / Combat rules  >")), b -> {
            cfg.save();Minecraft.getInstance().gui.setScreen(new TargetRulesScreen());
        }).bounds(x+12,row,w-24,20).build());
    }
    private void diagnosticsPage() {
        toggle(y+90,UiLabels.t("Prediction debug HUD"),()->cfg.bowPredictionDebug,v->cfg.bowPredictionDebug=v);
        toggle(y+118,UiLabels.t("Optional latency estimate"),()->cfg.bowPredictionLatency,v->cfg.bowPredictionLatency=v);
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Run model checks")), b -> {
            checks=dev.wukong.client.combat.BowPredictionChecks.run();
        }).bounds(x+12,y+146,w-24,20).build());
    }
    private void drawChecks(GuiGraphicsExtractor g){
        g.text(font,UiLabels.t("MINECRAFT MODEL / 6.3.4"),x+20,y+176,CYAN,false);
        g.text(font,UiLabels.t("Synthetic checks; no shots or network actions."),x+20,y+189,0xFFD0DCE7,false);
        if(checks.isEmpty())return;
        long passed=checks.stream().filter(dev.wukong.client.combat.BowPredictionChecks.Check::passed).count();
        g.text(font,UiLabels.t("Passed: ")+passed+" / "+checks.size(),x+20,y+206,passed==checks.size()?0xFF77D9A5:0xFFFF8888,false);
        int yy=y+222;
        for(var check:checks){
            if(check.passed())continue;
            g.text(font,UiLabels.t("FAIL: ")+check.name(),x+20,yy,0xFFFF8888,false);yy+=12;if(yy>y+310)break;
        }
        if(passed==checks.size())g.text(font,UiLabels.t("All model checks passed."),x+20,yy,0xFF77D9A5,false);
    }
    private static double clamp(double d,double min,double max){return Math.max(min,Math.min(max,Math.round(d*100)/100.0));}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial) {
        g.fill(x,y,x+w,y+h,0xF0101924);
        g.fill(x,y,x+w,y+30,0xFF302B26);
        g.fill(x,y+28,x+w,y+30,AMBER);
        g.text(font,UiLabels.t("WUKONG  /  COMBAT SETTINGS"),x+12,y+10,0xFFFFFFFF,false);
        g.fill(x+8,y+69,x+w-8,y+328,0x77232B37);
        g.fill(x+9,y+76,x+12,y+326,AMBER);
        g.text(font,tab==0?UiLabels.t("AIM FILTERS  /  RESPONSE"):tab==1?UiLabels.t("BOW PREDICTION  /  CAMERA"):UiLabels.t("MOTION MODEL / DIAGNOSTICS"),
                x+20,y+74,tab==0?AMBER:CYAN,false);
        if(tab==0) g.text(font,UiLabels.t("TRACKING"),x+20,y+207,AMBER,false);
        else if(tab==1) g.text(font,UiLabels.t("CONTROL / ADVANCED"),x+20,y+206,AMBER,false);
        else drawChecks(g);
        super.extractRenderState(g,mx,my,partial);
        g.text(font,UiLabels.t("ESC / BACK"),x+12,y+h-7,0xFF9ABACB,false);
    }
    @Override public void onClose(){cfg.save();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
