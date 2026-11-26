package dev.wukong.client;
/** Pure projection math used by the local camera zoom. */
final class ZoomMath {
    static double clamp(double n,double lo,double hi){return Double.isFinite(n)?Math.max(lo,Math.min(hi,n)):lo;}
    static double scroll(double zoom,double step,double max){return clamp(zoom*Math.pow(1.25,clamp(step,-4,4)),2,clamp(max,2,50));}
    static float fov(float original,double factor){
        if(!Float.isFinite(original)||original<=0||original>=179)return original;
        return (float)Math.toDegrees(2*Math.atan(Math.tan(Math.toRadians(original)*.5)/Math.max(1,clamp(factor,1,50))));
    }
    private ZoomMath(){}
}
