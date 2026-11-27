package dev.wukong.client.research;

import java.util.*;

/** Synthetic, isolated fixtures only. No Minecraft connection or inventory mutation. */
public final class ConservationLab {
    public enum Verdict { CONSERVED, MISMATCH, INCOMPLETE }
    public record Result(String name, Verdict expected, Verdict actual) {
        public boolean passed() { return expected == actual; }
    }
    public static Verdict compare(Map<String, Long> before, Map<String, Long> after,
                                  Map<String, Long> declaredDelta, boolean complete) {
        if (!complete) return Verdict.INCOMPLETE;
        Set<String> keys = new HashSet<>(before.keySet()); keys.addAll(after.keySet()); keys.addAll(declaredDelta.keySet());
        boolean mismatch = false;
        for (String k : keys) {
            long a = before.getOrDefault(k, 0L), b = after.getOrDefault(k, 0L);
            if (a < 0 || b < 0) return Verdict.INCOMPLETE;
            try { if (Math.addExact(a, declaredDelta.getOrDefault(k, 0L)) != b) mismatch = true; }
            catch (ArithmeticException ex) { return Verdict.INCOMPLETE; }
        }
        return mismatch ? Verdict.MISMATCH : Verdict.CONSERVED;
    }
    public static List<Result> run() {
        var base = Map.of("fixture:a", 64L, "fixture:b", 4L);
        return List.of(
            test("Unchanged / transfer", Verdict.CONSERVED, base, base, Map.of(), true),
            test("Declared addition", Verdict.CONSERVED, base, Map.of("fixture:a", 65L,"fixture:b",4L), Map.of("fixture:a",1L),true),
            test("Declared consumption", Verdict.CONSERVED, base, Map.of("fixture:a",63L,"fixture:b",4L),Map.of("fixture:a",-1L),true),
            test("Unexpected surplus", Verdict.MISMATCH, base, Map.of("fixture:a",65L,"fixture:b",4L),Map.of(),true),
            test("Unexpected loss", Verdict.MISMATCH, base, Map.of("fixture:a",63L,"fixture:b",4L),Map.of(),true),
            test("Partial visibility", Verdict.INCOMPLETE, base, Map.of(),Map.of(),false),
            test("Negative count", Verdict.INCOMPLETE, base, Map.of("fixture:a",-1L),Map.of(),true),
            test("Overflow", Verdict.INCOMPLETE, Map.of("fixture:a",Long.MAX_VALUE), Map.of(),Map.of("fixture:a",1L),true)
        );
    }
    private static Result test(String name,Verdict expected, Map<String,Long> a,Map<String,Long> b,Map<String,Long> delta,boolean complete) {
        return new Result(name,expected,compare(a,b,delta,complete));
    }
    private ConservationLab() {}
}
