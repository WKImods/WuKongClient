package dev.wukong.client;

/** Pure decision logic, independent of Minecraft's render and packet APIs. */
public final class SurvivalRules {
    public static int remaining(int maxDamage, int damage) { return Math.max(0, maxDamage - damage); }
    public static boolean wornOut(int maxDamage, int damage, int reserve) {
        return maxDamage > 0 && remaining(maxDamage, damage) <= Math.max(1,reserve);
    }
    public static boolean shouldEat(int food, int threshold) {
        return food >= 0 && food <= Math.max(1, Math.min(19,threshold));
    }
    public static double miningScore(float speed, boolean correctDrops) {
        return Math.max(0.0,speed) + (correctDrops ? 1.5 : 0.0);
    }
    private SurvivalRules() {}
}
