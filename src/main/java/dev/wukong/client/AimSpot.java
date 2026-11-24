package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Aim spots use hitbox fractions, not anatomical bones (vanilla mobs have one collision box). */
public final class AimSpot {
    public static final String[] MODES = {"TORSO", "HEAD", "NECK", "ALL_VISIBLE"};
    // normalized (x, y, z) inside the target AABB, ordered by preference.
    private static final double[][] ALL_SAMPLES = {
        {.50,.58,.50}, {.50,.78,.50}, {.50,.92,.50},
        {.04,.58,.50}, {.96,.58,.50}, {.50,.58,.04}, {.50,.58,.96},
        {.04,.78,.50}, {.96,.78,.50}, {.50,.78,.04}, {.50,.78,.96},
        {.04,.92,.50}, {.96,.92,.50}, {.50,.92,.04}, {.50,.92,.96},
        {.04,.58,.04}, {.04,.58,.96}, {.96,.58,.04}, {.96,.58,.96},
        {.04,.78,.04}, {.04,.78,.96}, {.96,.78,.04}, {.96,.78,.96}
    };

    public static String normalize(String mode) {
        if (mode != null) for (String allowed : MODES) if (allowed.equals(mode)) return mode;
        return "TORSO";
    }

    public static String next(String mode) {
        String normalized = normalize(mode);
        for (int i=0;i<MODES.length;i++) if (MODES[i].equals(normalized))
            return MODES[(i+1)%MODES.length];
        return "TORSO";
    }

    public static String label(String mode) {
        return switch (normalize(mode)) {
            case "HEAD" -> "Head";
            case "NECK" -> "Neck";
            case "ALL_VISIBLE" -> "All Visible";
            default -> "Torso";
        };
    }

    public static double[][] samples(String mode) {
        return switch (normalize(mode)) {
            case "HEAD" -> new double[][]{{.50,.92,.50}};
            case "NECK" -> new double[][]{{.50,.77,.50}};
            case "ALL_VISIBLE" -> ALL_SAMPLES;
            default -> new double[][]{{.50,.58,.50}};
        };
    }

    public static Vec3 select(Minecraft mc, LivingEntity target, Vec3 eye,
                              String mode, boolean visibilityRequired) {
        AABB box = target.getBoundingBox();
        // All Visible deliberately always requires a genuinely clear block ray.
        boolean mustSee = visibilityRequired || "ALL_VISIBLE".equals(normalize(mode));
        for (double[] sample : samples(mode)) {
            Vec3 point = new Vec3(box.minX + box.getXsize()*sample[0],
                    box.minY + box.getYsize()*sample[1],
                    box.minZ + box.getZsize()*sample[2]);
            if (!mustSee || VisibilityChecks.clear(mc, eye, point, null)) return point;
        }
        return null;
    }
    private AimSpot() {}
}
