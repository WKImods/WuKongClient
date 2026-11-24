package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;

/** Shared persistent storage for manual waypoints and death markers. */
public final class WaypointManager {
    public static String currentDimension(Minecraft mc) {
        return mc.level.dimension().identifier().toString();
    }
    public static BenchConfig.Waypoint newAtPlayer(Minecraft mc) {
        BlockPos p=mc.player.blockPosition();
        int count=BenchConfig.INSTANCE.waypoints == null ? 0 : BenchConfig.INSTANCE.waypoints.size();
        return new BenchConfig.Waypoint("Waypoint " + (count + 1), currentDimension(mc), p.getX(), p.getY(), p.getZ());
    }
    public static void add(BenchConfig.Waypoint wp) {
        BenchConfig cfg=BenchConfig.INSTANCE;
        if (cfg.waypoints == null) cfg.waypoints = new ArrayList<>();
        if (wp.id == null || wp.id.isBlank()) wp.id=java.util.UUID.randomUUID().toString();
        cfg.waypoints.add(wp);
        while (cfg.waypoints.size()>128) cfg.waypoints.remove(0);
        cfg.save();
    }
    public static void recordDeath(Minecraft mc) {
        if (mc.player==null || mc.level==null) return;
        BlockPos p=mc.player.blockPosition();
        BenchConfig.Waypoint wp = new BenchConfig.Waypoint("Death", currentDimension(mc), p.getX(),p.getY(),p.getZ());
        wp.death=true; wp.icon="SKULL"; wp.showInWorld=true;
        add(wp);
        // A new death should be visible without visiting the settings first.
        BenchConfig.INSTANCE.deathMarkerEnabled=true;
        BenchConfig.INSTANCE.save();
    }
    public static void remove(BenchConfig.Waypoint wp) {
        if (BenchConfig.INSTANCE.waypoints != null && BenchConfig.INSTANCE.waypoints.remove(wp))
            BenchConfig.INSTANCE.save();
    }
    private WaypointManager() {}
}
