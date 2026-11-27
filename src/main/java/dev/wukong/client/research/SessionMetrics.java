package dev.wukong.client.research;
import java.util.*;
/** Rolling observations of the vanilla player-list latency estimate, not packet RTT probes. */
public final class SessionMetrics {
    public record Summary(int samples, int latest, int min, int max, double average) {}
    private final ArrayDeque<Integer> latency = new ArrayDeque<>();
    private final int limit;
    public SessionMetrics(int limit) {
        if (limit < 1 || limit > 4096) throw new IllegalArgumentException("History limit");
        this.limit = limit;
    }
    public void observe(int value) {
        if (value <= 0) { clear(); return; } // remote zero is ambiguous, not a measured RTT
        if (latency.size() == limit) latency.removeFirst();
        latency.addLast(value);
    }
    /** Rounded upper bound for a readable chart; always at least 10 ms. */
    public int chartCeiling() {
        int peak=latency.stream().mapToInt(Integer::intValue).max().orElse(0);
        long step=10; while(peak>step*5 && step<1_000_000_000L)step*=5;
        return (int)Math.min(Integer.MAX_VALUE, Math.max(step, ((peak+step-1)/step)*step));
    }
    public void clear() { latency.clear(); }
    public List<Integer> history() { return List.copyOf(latency); }
    public Optional<Summary> summary() {
        if (latency.isEmpty()) return Optional.empty();
        var stats = latency.stream().mapToInt(Integer::intValue).summaryStatistics();
        return Optional.of(new Summary(latency.size(), latency.getLast(), stats.getMin(), stats.getMax(), stats.getAverage()));
    }
}
