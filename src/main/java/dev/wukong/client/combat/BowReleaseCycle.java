package dev.wukong.client.combat;

/** A release belongs to one observed vanilla bow-use cycle, not to the held input key. */
public final class BowReleaseCycle {
    private int tick = Integer.MIN_VALUE, charge;
    private boolean drawn, released;
    public void observe(int now, boolean usingBow, int chargeTicks) {
        if (now == tick) return; // Do not rearm on repeated observations in the same tick.
        if (now < tick) reset();
        int next = Math.max(0, chargeTicks);
        if (!usingBow || !drawn || next < charge) released = false;
        tick = now;
        charge = usingBow ? next : 0;
        drawn = usingBow;
    }
    public boolean ready() { return drawn && !released; }
    public void markReleased() { if (drawn) released = true; }
    public void reset() { tick = Integer.MIN_VALUE; charge = 0; drawn = false; released = false; }
}
