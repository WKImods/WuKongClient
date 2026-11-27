package dev.wukong.client.research;

import java.util.*;
import java.util.function.BooleanSupplier;

/** Deterministic synthetic checks, with no Minecraft or network access. */
public final class DetectionFixtures {
    public record Result(String name,boolean passed) {}
    private static ResearchLedger.Snapshot sample(long count,int invalid){
        return new ResearchLedger.Snapshot(Map.of("fixture:item",count),1,invalid,false);
    }
    private static void feed(DetectionEngine d,long n,long count,int invalid){
        var s=sample(count,invalid);d.observe(n,s,s.counts(),s.occupied());
    }
    private static Result test(String name,BooleanSupplier test){
        try{return new Result(name,test.getAsBoolean());}catch(RuntimeException e){return new Result(name,false);}
    }
    public static List<Result> run(){return List.of(
        test("Ordinary count changes",()->{var d=new DetectionEngine(4);feed(d,1,1,0);feed(d,2,64,0);feed(d,3,2,0);return d.active().isEmpty()&&d.history().isEmpty()&&d.consistentSamples()==3;}),
        test("Single local observation",()->{var d=new DetectionEngine(4);feed(d,1,65,1);return d.active().size()==1&&d.active().getFirst().state()==DetectionEngine.State.SINGLE_OBSERVATION;}),
        test("Repeated observations grouped",()->{var d=new DetectionEngine(4);feed(d,1,65,1);feed(d,2,66,1);return d.active().size()==1&&d.active().getFirst().observations()==2&&d.active().getFirst().state()==DetectionEngine.State.REPEATED_OBSERVATION;}),
        test("Cleared local observation",()->{var d=new DetectionEngine(4);feed(d,1,65,1);feed(d,2,64,0);return d.active().isEmpty()&&d.history().getFirst().state()==DetectionEngine.State.CLEARED;}),
        test("Pause interrupts evidence",()->{var d=new DetectionEngine(4);feed(d,1,65,1);d.pause();d.pause();feed(d,2,65,1);return d.history().size()==1&&d.history().getFirst().state()==DetectionEngine.State.INTERRUPTED&&d.active().getFirst().observations()==1;}),
        test("Missing sample interrupts evidence",()->{var d=new DetectionEngine(4);feed(d,1,65,1);feed(d,3,65,1);return d.history().getFirst().state()==DetectionEngine.State.INTERRUPTED&&d.active().getFirst().observations()==1;}),
        test("Local counts disagree",()->{var d=new DetectionEngine(4);d.observe(1,sample(65,1),Map.of("fixture:item",64L),1);return d.active().size()==1&&d.active().getFirst().code()==DetectionEngine.Code.LOCAL_CAPTURE_MISMATCH;}),
        test("Local slot counts disagree",()->{var d=new DetectionEngine(4);var s=sample(1,0);d.observe(1,s,s.counts(),0);return d.active().getFirst().code()==DetectionEngine.Code.LOCAL_CAPTURE_MISMATCH;}),
        test("Mismatch interrupts prior evidence",()->{var d=new DetectionEngine(4);feed(d,1,65,1);d.observe(2,sample(65,1),Map.of(),0);return d.active().size()==1&&d.history().getFirst().state()==DetectionEngine.State.INTERRUPTED;}),
        test("Bounded history",()->{var d=new DetectionEngine(1);feed(d,1,65,1);feed(d,2,64,0);feed(d,3,65,1);feed(d,4,64,0);return d.history().size()==1&&d.discarded()==1;}),
        test("Session reset",()->{var d=new DetectionEngine(1);feed(d,1,65,1);d.pause();d.clear();feed(d,1,1,0);return d.active().isEmpty()&&d.history().isEmpty()&&d.samples()==1&&d.discarded()==0;}),
        test("Duplicate sample rejected",()->{var d=new DetectionEngine(4);feed(d,1,1,0);try{feed(d,1,1,0);return false;}catch(IllegalArgumentException e){return d.samples()==1;}})
    );}
    private DetectionFixtures(){}
}
