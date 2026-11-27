package dev.wukong.client.research;

import java.util.*;

/** Local diagnostic evidence only. Neither ordinary deltas nor these findings prove server defects. */
public final class DetectionEngine {
    public enum Code { REPORTED_STACK_LIMIT, LOCAL_CAPTURE_MISMATCH }
    public enum State { SINGLE_OBSERVATION, REPEATED_OBSERVATION, CLEARED, INTERRUPTED }
    public record Finding(Code code, State state, long firstSample, long lastSample,
                          long observations, int affected, int peakAffected) {}
    private final int limit;
    private final EnumMap<Code,Finding> active = new EnumMap<>(Code.class);
    private final ArrayDeque<Finding> closed = new ArrayDeque<>();
    private long lastSample, discarded, samples, consistentSamples;
    public DetectionEngine(int limit) {
        if (limit < 1 || limit > 4096) throw new IllegalArgumentException("Finding limit");
        this.limit=limit;
    }
    /** The independently assembled view contains main inventory rows only. */
    public void observe(long sample, ResearchLedger.Snapshot aggregate, Map<String,Long> visibleCounts, int visibleSlots) {
        Objects.requireNonNull(aggregate); Objects.requireNonNull(visibleCounts);
        if (sample <= lastSample || visibleSlots < 0) throw new IllegalArgumentException("Invalid sample");
        if (lastSample != 0 && sample != lastSample+1) pause();
        lastSample=sample; samples++;
        boolean mismatch = aggregate.occupied()!=visibleSlots || !aggregate.counts().equals(visibleCounts);
        update(Code.LOCAL_CAPTURE_MISMATCH, mismatch ? 1 : 0, sample);
        // A conflicting local capture is not adequate evidence for another finding.
        if (mismatch) finish(Code.REPORTED_STACK_LIMIT,State.INTERRUPTED);
        else {
            update(Code.REPORTED_STACK_LIMIT,aggregate.invalid(),sample);
            if (aggregate.invalid()==0) consistentSamples++;
        }
    }
    private void update(Code code,int affected,long sample) {
        if (affected==0) { finish(code,State.CLEARED); return; }
        var old=active.get(code);
        active.put(code,old==null ? new Finding(code,State.SINGLE_OBSERVATION,sample,sample,1,affected,affected)
                : new Finding(code,State.REPEATED_OBSERVATION,old.firstSample(),sample,old.observations()+1,affected,Math.max(affected,old.peakAffected())));
    }
    private void finish(Code code,State state) {
        var old=active.remove(code);
        if(old==null)return;
        if(closed.size()==limit){closed.removeFirst();discarded++;}
        closed.addLast(new Finding(code,state,old.firstSample(),old.lastSample(),old.observations(),old.affected(),old.peakAffected()));
    }
    public void pause() { for(var code:Code.values())finish(code,State.INTERRUPTED); }
    public List<Finding> active(){return List.copyOf(active.values());}
    public List<Finding> history(){return List.copyOf(closed);}
    public long discarded(){return discarded;}
    public long samples(){return samples;}
    public long consistentSamples(){return consistentSamples;}
    public void clear(){active.clear();closed.clear();lastSample=discarded=samples=consistentSamples=0;}
    public static String explanation(Code code){return switch(code){
        case REPORTED_STACK_LIMIT -> "Stack count above the local item's reported limit; custom rules or transient state possible.";
        case LOCAL_CAPTURE_MISMATCH -> "Local aggregate and main-slot view disagree; capture quality issue, not a server finding.";
    };}
}
