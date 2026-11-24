package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Vanilla block-ray visibility from one viewpoint to sampled entity/container bounds. */
public final class VisibilityChecks {
    public static boolean clear(Minecraft mc, Vec3 from, Vec3 to, BlockPos targetBlock) {
        if (mc.player == null || mc.level == null) return false;
        if (from.distanceToSqr(to) < 1.0e-6) return true;
        var hit = mc.level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS ||
                (targetBlock != null && hit.getBlockPos().equals(targetBlock));
    }

    /** 17 samples, starting near center then checking the outline for partially exposed entities. */
    public static boolean anyVisible(Minecraft mc, Vec3 from, AABB box, BlockPos targetBlock) {
        double cx = (box.minX + box.maxX) * 0.5;
        double cy = (box.minY + box.maxY) * 0.5;
        double cz = (box.minZ + box.maxZ) * 0.5;
        if (clear(mc, from, new Vec3(cx,cy,cz), targetBlock)) return true;
        for (double yFrac : new double[]{0.18, 0.55, 0.88}) {
            double y = box.minY + (box.maxY-box.minY)*yFrac;
            // Four side points, including arms/legs at the outer hitbox.
            for (double[] xz : new double[][]{{.05,.5},{.95,.5},{.5,.05},{.5,.95}}) {
                double x=box.minX + (box.maxX-box.minX)*xz[0];
                double z=box.minZ + (box.maxZ-box.minZ)*xz[1];
                if (clear(mc,from,new Vec3(x,y,z),targetBlock)) return true;
            }
        }
        // Corners at mid height catch diagonal peeks.
        for (double x : new double[]{.05,.95}) for (double z : new double[]{.05,.95}) {
            Vec3 sample = new Vec3(box.minX + box.getXsize()*x, cy,
                    box.minZ + box.getZsize()*z);
            if (clear(mc,from,sample,targetBlock)) return true;
        }
        return false;
    }

    private VisibilityChecks() {}
}
