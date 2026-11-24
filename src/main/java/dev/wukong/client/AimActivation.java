package dev.wukong.client;

/** Tracks R key presses at client-tick frequency. Independent of Minecraft input APIs. */
public final class AimActivation {
    private boolean wasRDown;
    private boolean toggled;
    private String previousMode = "HOLD";

    /**
     * @param rDown current R key state
     * @param enabled master Aim Assist switch
     * @param gameplayAllowed false while a menu is active
     * @param mode HOLD or TOGGLE (invalid/missing values fall back to HOLD)
     * @return whether targeting is enabled for this tick
     */
    public boolean update(boolean rDown, boolean enabled, boolean gameplayAllowed, String mode) {
        String resolvedMode = "TOGGLE".equals(mode) ? "TOGGLE" : "HOLD";
        boolean justPressed = rDown && !wasRDown;
        wasRDown = rDown; // Always update, including while screens are open.

        if (!resolvedMode.equals(previousMode) || !enabled) toggled = false;
        previousMode = resolvedMode;

        if (!enabled || !gameplayAllowed) return false;
        if ("TOGGLE".equals(resolvedMode)) {
            if (justPressed) toggled = !toggled;
            return toggled;
        }
        return rDown;
    }

    /** Prevents a previous world's toggle state carrying into another world. */
    public void reset() {
        wasRDown = false;
        toggled = false;
        previousMode = "HOLD";
    }
}
