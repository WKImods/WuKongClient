package dev.wukong.client;

/** Pure target-ranking rules. Smaller scores are better. */
public final class TargetRules {
    public static final String[] MODES = {
            "CROSSHAIR", "CLOSEST", "FURTHEST", "LOWEST_HP", "HIGHEST_HP",
            "LOW_HP_CLOSE", "HIGH_HP_CLOSE"
    };

    public static String normalize(String value) {
        if (value != null) for (String mode : MODES) if (mode.equals(value)) return mode;
        return "CROSSHAIR";
    }

    public static String next(String current) {
        String mode = normalize(current);
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i].equals(mode)) return MODES[(i + 1) % MODES.length];
        }
        return MODES[0];
    }

    public static String label(String mode) {
        return switch (normalize(mode)) {
            case "CLOSEST" -> "Closest";
            case "FURTHEST" -> "Furthest";
            case "LOWEST_HP" -> "Lowest HP";
            case "HIGHEST_HP" -> "Highest HP";
            case "LOW_HP_CLOSE" -> "Low HP + Close";
            case "HIGH_HP_CLOSE" -> "High HP + Close";
            default -> "Crosshair (original)";
        };
    }

    public static double score(String mode, double distance, double health, double maxHealth,
                               double angle, double range) {
        double d = Math.max(0, distance);
        double h = Math.max(0, health);
        double ratio = Math.min(1.0, h / Math.max(1.0, maxHealth));
        double normalizedDistance = Math.min(1.0, d / Math.max(1.0, range));
        double tie = d * 0.00001 + Math.max(0, angle) * 0.0000001;
        return switch (normalize(mode)) {
            case "CLOSEST" -> d + Math.max(0, angle) * 0.00001;
            case "FURTHEST" -> -d + Math.max(0, angle) * 0.00001;
            case "LOWEST_HP" -> h + tie;
            case "HIGHEST_HP" -> -h + tie;
            case "LOW_HP_CLOSE" -> 0.65 * ratio + 0.35 * normalizedDistance + tie * 0.00001;
            case "HIGH_HP_CLOSE" -> 0.65 * (1.0 - ratio) + 0.35 * normalizedDistance + tie * 0.00001;
            default -> Math.max(0, angle) + d * 0.05;
        };
    }

    private TargetRules() {}
}
