package dev.wukong.client;

public class LogicSmokeTest {
    private static void verify(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        AimActivation a = new AimActivation();
        verify(!a.update(false, true, true, "HOLD"), "Hold idle");
        verify(a.update(true, true, true, "HOLD"), "Hold down");
        verify(!a.update(false, true, true, "HOLD"), "Hold release");
        a.reset();
        verify(a.update(true, true, true, "TOGGLE"), "Toggle on");
        verify(a.update(true, true, true, "TOGGLE"), "Toggle retained");
        verify(a.update(false, true, true, "TOGGLE"), "Toggle idle");
        verify(!a.update(true, true, true, "TOGGLE"), "Toggle off");
        verify("CROSSHAIR".equals(TargetRules.normalize(null)), "Null priority");
        verify("CLOSEST".equals(TargetRules.next("CROSSHAIR")), "Priority next");
        verify(TargetRules.score("CLOSEST", 2, 10, 20, 5, 32)
                < TargetRules.score("CLOSEST", 4, 10, 20, 5, 32), "Nearest wins");
        verify(TargetRules.score("LOWEST_HP", 2, 2, 20, 5, 32)
                < TargetRules.score("LOWEST_HP", 2, 10, 20, 5, 32), "Low HP wins");
        System.out.println("PASS: 11 pure-Java assertions (AimActivation / TargetRules)");
    }
}
