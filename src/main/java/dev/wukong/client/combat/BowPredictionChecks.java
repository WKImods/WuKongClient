package dev.wukong.client.combat;

import java.util.ArrayList;
import java.util.List;
import dev.wukong.client.combat.BowMotion.*;

/** Deterministic Minecraft-only model checks; never access a world or send game actions. */
public final class BowPredictionChecks {
    public record Check(String name, boolean passed){}
    private static void check(List<Check> out,String name,boolean ok){out.add(new Check(name,ok));}
    private static boolean close(double a,double b){return Math.abs(a-b)<1e-6;}
    private static Sample sample(Mode mode,V velocity,V acceleration){return new Sample(mode,velocity,acceleration,5,true);}
    private static boolean valid(V target,Sample motion,V inherited,double power,double age){
        var r=VanillaBowModel.solve(V.ZERO,target,motion,inherited,power,age);
        if(r==null || !close(r.direction().length(),1)||r.ticks()<=0||r.ticks()>100||r.residual()>1e-4)return false;
        // Independent per-tick game update, not the solver's precomputed lookup table.
        V p=V.ZERO, v=r.direction().mul(power).add(inherited);
        int n=(int)r.ticks();for(int i=0;i<n;i++){p=p.add(v);v=v.mul((double).99f).add(new V(0,-.05,0));}
        p=p.add(v.mul(r.ticks()-n));
        return p.sub(target.add(motion.displacement(r.ticks()+age))).length()<1e-4;
    }
    public static List<Check> run(){
        List<Check> out=new ArrayList<>();
        var t=new Tracker();
        check(out,"Initial sample is unknown",!t.update(V.ZERO,0,Mode.GROUND).valid());
        check(out,"Ground speed",close(t.update(new V(.2,0,0),1,Mode.GROUND).velocity().x(),.2));
        check(out,"Duplicate tick ignored",t.update(new V(99,0,0),1,Mode.GROUND).observations()==1);
        check(out,"Ground vertical motion ignored",close(t.update(new V(.4,.2,0),2,Mode.GROUND).velocity().y(),0));
        check(out,"Gap resets",!t.update(new V(.5,0,0),8,Mode.GROUND).valid());
        check(out,"Reset can recover",t.update(new V(.7,0,0),9,Mode.GROUND).valid());
        check(out,"Teleport rejected",!t.update(new V(99,0,0),10,Mode.GROUND).valid());
        check(out,"Backward tick resets",!t.update(V.ZERO,1,Mode.GROUND).valid());
        check(out,"Invalid coordinates rejected",!t.update(new V(Double.NaN,0,0),2,Mode.GROUND).valid());
        t=new Tracker();t.update(V.ZERO,0,Mode.ELYTRA);
        var fly=t.update(new V(1.675,.2,0),1,Mode.ELYTRA);
        check(out,"Elytra above old 24 b/s limit",fly.valid()&&fly.velocity().length()>1.2);
        check(out,"Elytra vertical movement",close(fly.velocity().y(),.2));
        var accel=t.update(new V(3.65,.5,.2),2,Mode.ELYTRA);
        check(out,"Elytra acceleration sampled",accel.acceleration().length()>0&&accel.acceleration().length()<=.180001);
        var land=t.update(new V(3.85,.5,.2),3,Mode.GROUND);
        check(out,"Landing clears acceleration",close(land.acceleration().length(),0)&&land.mode()==Mode.GROUND);
        check(out,"Ground inherited Y excluded",VanillaBowModel.inherited(new V(.3,.4,.5),true).equals(new V(.3,0,.5)));
        check(out,"Flight inherits XYZ",VanillaBowModel.inherited(new V(.3,.4,.5),false).equals(new V(.3,.4,.5)));
        var still=sample(Mode.GROUND,V.ZERO,V.ZERO);
        var moving=sample(Mode.GROUND,new V(.2,0,0),V.ZERO);
        var flying=sample(Mode.ELYTRA,new V(1.675,.1,0),V.ZERO);
        check(out,"Stationary 10 blocks",valid(new V(0,0,10),still,V.ZERO,3,0));
        check(out,"Stationary 64 blocks",valid(new V(0,0,64),still,V.ZERO,3,0));
        check(out,"Ground moving target",valid(new V(0,0,30),moving,V.ZERO,3,0));
        check(out,"Own walking motion",valid(new V(0,0,30),still,new V(.24,0,0),3,0));
        check(out,"Both players moving",valid(new V(0,0,30),moving,new V(-.3,0,0),3,0));
        check(out,"Own airborne XYZ motion",valid(new V(0,0,30),still,new V(.7,-.2,.1),3,0));
        check(out,"Elytra target",valid(new V(0,0,24),flying,V.ZERO,3,0));
        check(out,"Both Elytra",valid(new V(0,0,24),flying,new V(1.5,.2,0),3,0));
        check(out,"Flight acceleration",valid(new V(0,0,20),sample(Mode.ELYTRA,new V(.6,.1,0),new V(.02,-.01,.03)),new V(.5,0,0),3,0));
        check(out,"Reduced charge",valid(new V(0,0,8),still,V.ZERO,1.2,0));
        check(out,"Optional age estimate",valid(new V(0,0,20),moving,V.ZERO,3,1));
        check(out,"No model when unreachable",VanillaBowModel.solve(V.ZERO,new V(10000,0,0),still,V.ZERO,3,0)==null);
        check(out,"No model at zero charge",VanillaBowModel.solve(V.ZERO,new V(0,0,20),still,V.ZERO,0,0)==null);
        check(out,"No model for unknown movement",VanillaBowModel.solve(V.ZERO,new V(0,0,20),Sample.empty(Mode.AIR),V.ZERO,3,0)==null);
        check(out,"Nonfinite input rejected",VanillaBowModel.solve(V.ZERO,new V(Double.NaN,0,0),still,V.ZERO,3,0)==null);
        check(out,"Invalid ping-age rejected",VanillaBowModel.solve(V.ZERO,new V(0,0,20),still,V.ZERO,3,4)==null);
        return List.copyOf(out);
    }
    private BowPredictionChecks(){}
}
