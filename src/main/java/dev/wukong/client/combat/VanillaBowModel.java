package dev.wukong.client.combat;

import dev.wukong.client.combat.BowMotion.V;
import dev.wukong.client.combat.BowMotion.Sample;

/** Approximation of Minecraft Java vanilla BowItem/Arrow in air; all units are game ticks/blocks. */
public final class VanillaBowModel {
    private static final double DRAG=(double)0.99f, GRAVITY=.05;
    private static final int MAX_TICKS=100;
    // Discrete position, drag, acceleration sequence; fractional ticks interpolate movement.
    private static final double[] DISTANCE=new double[MAX_TICKS+1], FALL=new double[MAX_TICKS+1];
    static {double factor=1,down=0;for(int i=1;i<=MAX_TICKS;i++){
        DISTANCE[i]=DISTANCE[i-1]+factor;FALL[i]=FALL[i-1]+down;
        factor*=DRAG;down=down*DRAG+GRAVITY;
    }}
    public record Result(V direction,V futurePoint,double ticks,double residual){}
    public static V inherited(V motion,boolean grounded){return grounded?new V(motion.x(),0,motion.z()):motion;}
    private static double at(double[] values,double t){int i=(int)Math.floor(t);return i>=MAX_TICKS?values[MAX_TICKS]:values[i]+(values[i+1]-values[i])*(t-i);}
    private static V needed(V origin,V target,Sample sample,V inherited,double t,double age){
        double d=at(DISTANCE,t);
        return target.add(sample.displacement(t+age)).sub(origin).add(new V(0,at(FALL,t),0)).mul(1/d).sub(inherited);
    }
    /** Earliest low-arc model solution, or null when the bounded game model cannot find one. */
    public static Result solve(V origin,V target,Sample motion,V inherited,double power,double observationAge){
        if(origin==null||target==null||motion==null||inherited==null||!origin.finite()||!target.finite()||!inherited.finite()
                ||!Double.isFinite(power)||power<=.15||power>3.01||!Double.isFinite(observationAge)||observationAge<0||observationAge>3
                ||!motion.valid()||!motion.velocity().finite()||!motion.acceleration().finite())return null;
        double lo=.05;
        double previous=needed(origin,target,motion,inherited,lo,observationAge).length()-power;
        if(!Double.isFinite(previous)||previous<0)return null;
        for(double hi=.25;hi<=MAX_TICKS;hi+=.25){
            double f=needed(origin,target,motion,inherited,hi,observationAge).length()-power;
            if(!Double.isFinite(f))return null;
            if(f<=0){
                for(int j=0;j<20;j++){double mid=(lo+hi)*.5;
                    if(needed(origin,target,motion,inherited,mid,observationAge).length()>power)lo=mid;else hi=mid;}
                double t=(lo+hi)*.5;V required=needed(origin,target,motion,inherited,t,observationAge);
                double speed=required.length();if(speed<1e-8||!Double.isFinite(speed))return null;
                V direction=required.mul(1/speed), future=target.add(motion.displacement(t+observationAge));
                V endpoint=origin.add(direction.mul(power).add(inherited).mul(at(DISTANCE,t))).sub(new V(0,at(FALL,t),0));
                return new Result(direction,future,t,endpoint.sub(future).length());
            }
            lo=hi;
        }
        return null;
    }
    private VanillaBowModel(){}
}
