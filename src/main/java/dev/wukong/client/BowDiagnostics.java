package dev.wukong.client;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import dev.wukong.client.combat.BowMotion.Sample;
import dev.wukong.client.combat.BowMotion.V;
import dev.wukong.client.combat.VanillaBowModel.Result;

/** Short-lived read-only telemetry from the selected Minecraft bow model. */
public final class BowDiagnostics {
    private record Frame(Object world,Object player,int tick,Sample own,Sample target,V inherited,Result result,int ping,double age,String status){}
    private static Frame frame;
    public static void clear(){frame=null;}
    public static void publish(Minecraft mc,Sample own,Sample target,V inherited,Result result,int ping,double age,String status){
        if(BenchConfig.INSTANCE.bowPredictionDebug)frame=new Frame(mc.level,mc.player,mc.player.tickCount,own,target,inherited,result,ping,age,status);
    }
    private static String f(double x){return String.format(Locale.ROOT,"%.2f",x);}
    private static String vec(V v){return f(v.x())+" / "+f(v.y())+" / "+f(v.z());}
    public static void draw(GuiGraphicsExtractor g,Minecraft mc){
        BenchConfig c=BenchConfig.INSTANCE;Frame d=frame;
        if(!c.bowPredictionDebug||!c.aimAssist||!c.aimPrediction||d==null||mc.player==null||mc.level!=d.world||mc.player!=d.player||mc.gui.screen()!=null||mc.player.tickCount-d.tick<0||mc.player.tickCount-d.tick>1)return;
        String[] lines={UiLabels.t("BOW MODEL 6.3.4 | ")+UiLabels.t(d.status),
                UiLabels.t("Self: ")+UiLabels.t(d.own.mode())+" | "+f(d.inherited.length()*20)+UiLabels.t(" b/s inherited"),
                UiLabels.t("Self accel: ")+vec(d.own.acceleration()),
                UiLabels.t("Target: ")+(d.target==null?"--":UiLabels.t(d.target.mode())+" | "+f(d.target.velocity().length()*20)+UiLabels.t(" b/s")),
                UiLabels.t("Target accel: ")+(d.target==null?"--":vec(d.target.acceleration())),
                UiLabels.t("Flight: ")+(d.result==null?"--":f(d.result.ticks())+UiLabels.t(" ticks | residual ")+f(d.result.residual())),
                UiLabels.t("Future pos: ")+(d.result==null?"--":vec(d.result.futurePoint())),
                "Ping: "+(d.ping<0?"--":d.ping+" ms")+UiLabels.t(" | age: ")+f(d.age)+" t"};
        int w=0;for(String line:lines)w=Math.max(w,mc.font.width(line));
        int x=Math.max(4,(g.guiWidth()-w-14)/2),y=5;
        g.fill(x-4,y-3,x+w+4,y+lines.length*11+1,0xC0101924);
        for(int i=0;i<lines.length;i++)g.text(mc.font,lines[i],x,y+i*11,i==0?0xFF65DBE7:0xFFD8E4EC,true);
    }
    private BowDiagnostics(){}
}
