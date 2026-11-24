package dev.wukong.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;

/** Uses the same 3D-to-HUD projection as entity ESP; one box/label per connected ore vein. */
public final class OreOverlay {
    private record Candidate(OreClusters.Cluster group, double distanceSq) {}
    private static final int MAX_GROUPS_DRAWN = 75;
    private static final int MAX_DOTS_DRAWN = 240;
    private static final int MAX_VISIBILITY_CHECKS = 180;
    private static final int MAX_TESTS_PER_VEIN = 10;
    private static final Map<OreClusters.Cluster, Boolean> VISIBILITY = new IdentityHashMap<>();
    private static Object lastLevel;
    private static int lastTick = Integer.MIN_VALUE;
    private static int raysThisTick;

    public static void draw(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        BenchConfig cfg = BenchConfig.INSTANCE;
        if (!cfg.espEnabled || !cfg.oreFinderEnabled || mc.player == null || mc.level == null ||
                mc.gui.screen() != null) return;
        Camera camera = mc.gameRenderer.mainCamera();
        if (camera == null || !camera.isInitialized()) return;
        Vec3 origin = camera.position();
        // A block-ray per draw frame is expensive at high FPS. Cache visibility for one game tick.
        if (mc.level != lastLevel || mc.player.tickCount != lastTick) {
            VISIBILITY.clear(); raysThisTick = 0; lastTick = mc.player.tickCount; lastLevel = mc.level;
        }
        int range = Math.max(16,Math.min(64,cfg.oreRange));
        double limit = (double) range*range;
        List<Candidate> candidates = new ArrayList<>();
        for (OreClusters.Cluster group : OreScanner.clusters()) {
            double cx=(group.minX()+group.maxX()+1)*.5;
            double cy=(group.minY()+group.maxY()+1)*.5;
            double cz=(group.minZ()+group.maxZ()+1)*.5;
            double d2=mc.player.position().distanceToSqr(cx,cy,cz);
            if (d2 <= limit + 64) candidates.add(new Candidate(group,d2));
        }
        candidates.sort(Comparator.comparingDouble(Candidate::distanceSq));
        int drawn=0, dotsDrawn=0;
        for (Candidate candidate : candidates) {
            if (drawn >= MAX_GROUPS_DRAWN) break;
            OreClusters.Cluster vein = candidate.group();
            if (!cfg.espThroughWalls) {
                Boolean known = VISIBILITY.get(vein);
                if (known == null) {
                    boolean seen = false;
                    int checks = Math.min(MAX_TESTS_PER_VEIN, vein.blocks().size());
                    for (int i=0; i<checks && raysThisTick<MAX_VISIBILITY_CHECKS; i++) {
                        // Spread samples through the vein rather than repeatedly checking one edge.
                        OreClusters.Point p = vein.blocks().get((i*vein.blocks().size())/checks);
                        raysThisTick++;
                        BlockPos pos = new BlockPos(p.x(),p.y(),p.z());
                        Vec3 center = new Vec3(p.x()+.5,p.y()+.5,p.z()+.5);
                        if (VisibilityChecks.clear(mc,origin,center,pos)) { seen=true; break; }
                    }
                    if (raysThisTick < MAX_VISIBILITY_CHECKS) VISIBILITY.put(vein,seen);
                    known = seen;
                }
                if (!known) continue;
            }
            OreKind kind = OreKind.valueOf(vein.kind());
            if (!cfg.oreVisible(kind)) continue;
            AABB bounds = new AABB(vein.minX(),vein.minY(),vein.minZ(),
                    vein.maxX()+1,vein.maxY()+1,vein.maxZ()+1);
            String name=kind.displayName + " x" + vein.count();
            EspOverlay.drawMarker(g,mc,camera,bounds,name,Math.sqrt(candidate.distanceSq()),kind.color,cfg,
                    cfg.oreEspBoxes, cfg.oreEspNames, cfg.espTracers);
            if (cfg.oreDotsEnabled && cfg.espThroughWalls) {
                // Small squares convey the individual blocks without adding a label to each one.
                int shown=0;
                for (OreClusters.Point p : vein.blocks()) {
                    if (shown++ >= 40 || dotsDrawn >= MAX_DOTS_DRAWN) break;
                    dotsDrawn++;
                    EspOverlay.drawOreDot(g,mc,camera,p.x()+.5,p.y()+.5,p.z()+.5,kind.color);
                }
            }
            drawn++;
        }
    }
    private OreOverlay() {}
}
