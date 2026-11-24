package dev.wukong.client;

/** Small finite palette: saved IDs stay stable even when the UI language changes. */
public final class WaypointIcons {
    private static final String[] IDS = {"PIN", "STAR", "HOME", "FLAG", "CROSS", "SKULL"};
    private static final String[] GLYPHS = {"◆", "★", "⌂", "⚑", "+", "☠"};
    private static final int[] COLORS = {
        0xFF61DCE7, 0xFFFFD269, 0xFF8AE3AD, 0xFFB89AFF, 0xFF95C8FF, 0xFFFF8585
    };
    public static boolean isValid(String id) {
        for (String value : IDS) if (value.equals(id)) return true;
        return false;
    }
    public static String glyph(String id) {
        for (int i=0;i<IDS.length;i++) if (IDS[i].equals(id)) return GLYPHS[i];
        return GLYPHS[0];
    }
    public static int color(String id) {
        for (int i=0;i<IDS.length;i++) if (IDS[i].equals(id)) return COLORS[i];
        return COLORS[0];
    }
    public static String next(String id) {
        for (int i=0;i<IDS.length;i++) if (IDS[i].equals(id)) return IDS[(i+1)%IDS.length];
        return IDS[0];
    }
    private WaypointIcons() {}
}
