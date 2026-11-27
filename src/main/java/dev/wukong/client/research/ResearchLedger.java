package dev.wukong.client.research;

import java.util.*;
import java.time.Instant;
import java.time.Clock;

/** Read-only observations. Client observations never prove a server-side duplication. */
public final class ResearchLedger {
    public enum Kind { BASELINE, CHANGE, CONTAINER, PAUSE, RESUME }
    public record Snapshot(Map<String, Long> counts, int occupied, int invalid, boolean containerOpen) {
        public Snapshot {
            counts = Collections.unmodifiableMap(new TreeMap<>(counts));
            if (occupied < 0 || invalid < 0 || counts.values().stream().anyMatch(n -> n <= 0))
                throw new IllegalArgumentException("Invalid snapshot");
        }
        public long total() { return counts.values().stream().mapToLong(Long::longValue).sum(); }
    }
    public record Event(long sequence, Kind kind, int changedTypes, long itemDelta, String detail,
                        Instant recordedAt, Map<String,Long> differences) {
        public Event { differences = Collections.unmodifiableMap(new TreeMap<>(differences)); }
    }
    public record Finding(String code, String evidence) {}
    private final int limit;
    private final Clock clock;
    private final ArrayDeque<Event> events = new ArrayDeque<>();
    private Snapshot last;
    private long samples, sequence, dropped, changed;
    private int invalidSamples;
    private boolean paused;
    public ResearchLedger(int limit) { this(limit, Clock.systemUTC()); }
    public ResearchLedger(int limit, Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        if (limit < 1 || limit > 4096) throw new IllegalArgumentException("History limit");
        this.limit = limit;
    }
    public void observe(Snapshot current) {
        Objects.requireNonNull(current);
        samples++;
        if (current.invalid() > 0) invalidSamples++;
        if (last == null) add(Kind.BASELINE, 0, 0, "New observation baseline");
        else {
            if (last.containerOpen() != current.containerOpen())
                add(Kind.CONTAINER, 0, 0, current.containerOpen() ? "Container opened" : "Container closed");
            if (!last.counts().equals(current.counts())) {
                Set<String> keys = new HashSet<>(last.counts().keySet());
                keys.addAll(current.counts().keySet());
                Map<String,Long> differences = new TreeMap<>();
                for (String key : keys) {
                    long delta = current.counts().getOrDefault(key, 0L) - last.counts().getOrDefault(key, 0L);
                    if (delta != 0) differences.put(key, delta);
                }
                changed++;
                add(Kind.CHANGE, differences.size(), current.total() - last.total(), "Observed count change; cause unknown", differences);
            }
        }
        paused = false;
        last = current;
    }
    public void pause() {
        if (last != null && !paused) { add(Kind.PAUSE, 0, 0, "Observation paused"); paused = true; }
        last = null; // never compare across gaps
    }
    private void add(Kind kind, int types, long delta, String detail) {
        add(kind, types, delta, detail, Map.of());
    }
    private void add(Kind kind, int types, long delta, String detail, Map<String,Long> differences) {
        if (events.size() == limit) { events.removeFirst(); dropped++; }
        events.addLast(new Event(++sequence, kind, types, delta, detail, clock.instant(), differences));
    }
    public List<Event> events() { return List.copyOf(events); }
    public List<Finding> findings() {
        if (invalidSamples == 0) return List.of();
        return List.of(new Finding("CLIENT_STACK_LIMIT", invalidSamples + " sampled observations above the reported stack limit; not server proof"));
    }
    public Snapshot latest() { return last; }
    public long samples() { return samples; }
    public long changed() { return changed; }
    public long dropped() { return dropped; }
    public boolean paused() { return paused; }
}
