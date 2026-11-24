package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;

/**
 * Render-only view interpolation for the already computed AimAssist rotation.
 * This class NEVER calculates a second (direct-to-target) aiming direction.
 * It neither changes the player's rotation nor the projectile's trajectory.
 */
public final class BowViewSplit {
    private static boolean tracked;
    private static Object level;
    private static int lastTick;
    private static float previousYaw;
    private static float previousPitch;
    private static float currentYaw;
    private static float currentPitch;

    /** Must be called AFTER AimAssist has applied its rotation for this tick. */
    public static void trackIfEnabled(Minecraft mc, LivingEntity target) {
        BenchConfig cfg = BenchConfig.INSTANCE;
        boolean bowEquipped = mc.player != null &&
                (mc.player.getMainHandItem().getItem() instanceof BowItem ||
                 mc.player.getOffhandItem().getItem() instanceof BowItem);
        if (mc.player == null || mc.level == null || target == null || !target.isAlive() ||
                !cfg.separateBowCamera || !cfg.aimPrediction || !bowEquipped) {
            clear();
            return;
        }

        int tick = mc.player.tickCount;
        float playerYaw = mc.player.getYRot();
        float playerPitch = mc.player.getXRot();

        if (!tracked || mc.level != level || tick < lastTick || tick - lastTick > 1) {
            previousYaw = currentYaw = playerYaw;
            previousPitch = currentPitch = playerPitch;
        } else {
            if (tick != lastTick) {
                previousYaw = currentYaw;
                previousPitch = currentPitch;
            }
            // Unwrap yaw so interpolating across -180/180 degrees takes the short arc.
            currentYaw = previousYaw + Mth.wrapDegrees(playerYaw - previousYaw);
            currentPitch = playerPitch;
        }
        lastTick = tick;
        level = mc.level;
        tracked = true;
    }

    public static boolean shouldApply() {
        Minecraft mc = Minecraft.getInstance();
        return tracked && BenchConfig.INSTANCE.separateBowCamera &&
                BenchConfig.INSTANCE.aimPrediction &&
                mc.player != null && mc.level != null && mc.level == level &&
                mc.player.tickCount - lastTick >= 0 &&
                mc.player.tickCount - lastTick <= 1 &&
                mc.gui.screen() == null && mc.getCameraEntity() == mc.player;
    }

    public static float yaw(float partial) {
        return Mth.lerp(Mth.clamp(partial, 0.0F, 1.0F), previousYaw, currentYaw);
    }

    public static float pitch(float partial) {
        return Mth.lerp(Mth.clamp(partial, 0.0F, 1.0F), previousPitch, currentPitch);
    }

    public static void clear() {
        tracked = false;
        level = null;
    }

    private BowViewSplit() {}
}
