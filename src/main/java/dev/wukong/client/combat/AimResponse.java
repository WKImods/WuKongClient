package dev.wukong.client.combat;

/** Minecraft camera response presets. Stable IDs are stored in server profiles. */
public enum AimResponse {
    CUSTOM("Custom response", 0),
    SOFT("Smooth - Soft", .10),
    LOW("Smooth - Low", .25),
    MID("Smooth - Mid", .55),
    SNAPPY("Snappy", 1.0);

    private final String label;
    private final double fraction;
    AimResponse(String label, double fraction) { this.label=label; this.fraction=fraction; }
    public String label() { return label; }
    public AimResponse next() { return values()[(ordinal()+1)%values().length]; }
    public static AimResponse fromId(String id) {
        if (id != null) {
            try { return valueOf(id); } catch (IllegalArgumentException ignored) { }
        }
        return CUSTOM;
    }
    public double strength(double custom, boolean precision) {
        if (this != CUSTOM) return fraction;
        if (!Double.isFinite(custom)) custom = .75;
        double clamped = Math.max(.10, Math.min(1.0, custom));
        return precision ? clamped*.55 : clamped;
    }
}
