package dev.wukong.client;

import net.minecraft.client.Minecraft;

/** Local camera magnification. Never writes the vanilla FOV or sensitivity settings. */
public final class DynamicZoom {
    private static boolean held;
    private static double target=4, current=1;
    private static long previousNanos;
    private static String profile;
    private DynamicZoom() {}
    private static double clamp(double n,double lo,double hi){return ZoomMath.clamp(n,lo,hi);}
    private static boolean permitted(){
        Minecraft mc=Minecraft.getInstance(); var c=BenchConfig.INSTANCE;
        return c.visualMasterEnabled && c.zoomEnabled && mc.player!=null && mc.level!=null
                && mc.gui.screen()==null && mc.isWindowActive() && mc.mouseHandler.isMouseGrabbed()
                && !mc.player.isScoping() && !WuKongKeys.ZOOM.isUnbound() && WuKongKeys.ZOOM.isDown();
    }
    private static boolean begin(){
        if(!permitted()){reset();return false;}
        String key=ServerProfiles.activeFileLabel();
        if(!held || !java.util.Objects.equals(profile,key)){
            var c=BenchConfig.INSTANCE;
            profile=key;held=true;
            target=clamp(c.zoomRemember?c.zoomRemembered:c.zoomStart,2,clamp(c.zoomMax,2,50));
            current=c.zoomSmooth?1:target;previousNanos=System.nanoTime();
        }
        return true;
    }
    private static void reset(){held=false;current=1;previousNanos=0;profile=null;}
    static void tick(){if(!permitted())reset();while(WuKongKeys.ZOOM.consumeClick()){} }
    public static boolean scroll(double vertical){
        if(!begin())return false;
        var c=BenchConfig.INSTANCE;
        if(Double.isFinite(vertical)&&vertical!=0){
            target=ZoomMath.scroll(target,vertical,c.zoomMax);
            if(!c.zoomSmooth)current=target;
            if(c.zoomRemember){c.zoomRemembered=target;c.save();}
        }
        return true;
    }
    public static float fov(float vanilla){
        if(!begin())return vanilla;
        var c=BenchConfig.INSTANCE;target=clamp(target,2,clamp(c.zoomMax,2,50));
        long now=System.nanoTime();double dt=clamp((now-previousNanos)/1_000_000_000.0,0,.1);previousNanos=now;
        current=c.zoomSmooth?current+(target-current)*(1-Math.exp(-18*dt)):target;
        return ZoomMath.fov(vanilla,current);
    }
    public static double mouseDelta(double original){
        return begin() && BenchConfig.INSTANCE.zoomAdaptive?original/Math.max(1,current):original;
    }
}
