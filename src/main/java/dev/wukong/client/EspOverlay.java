package dev.wukong.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;

/**
 * Projects world-space AABBs into a lightweight GUI overlay. This is a 3D-box
 * projection, not a world render pass: its outlines intentionally ignore depth.
 * Only entities and block entities already loaded by the vanilla client are shown.
 */
public final class EspOverlay {
    // All 12 cube edges, each pair indexes into CORNERS in bit order X/Y/Z.
    private static final int[][] EDGES = {
        {0,1}, {2,3}, {4,5}, {6,7},
        {0,2}, {1,3}, {4,6}, {5,7},
        {0,4}, {1,5}, {2,6}, {3,7}
    };
    private static final int PLAYERS_COLOR = 0xFF56C7FF;
    private static final int HOSTILE_COLOR = 0xFFFF6A78;
    private static final int PASSIVE_COLOR = 0xFF7BE5A0;
    private static final int CHEST_COLOR = 0xFFFFC76A;
    private static final int MAX_MARKERS = 80;
    private static final Map<Long, Boolean> VISIBILITY_CACHE = new HashMap<>();
    private static int visibilityTick = Integer.MIN_VALUE;
    private static Object visibilityLevel;

    private static void updateVisibilityCache(Minecraft mc) {
        int tick = mc.player.tickCount;
        if (visibilityLevel != mc.level || visibilityTick != tick) {
            VISIBILITY_CACHE.clear();
            visibilityLevel = mc.level;
            visibilityTick = tick;
        }
    }

    private static boolean visible(Minecraft mc, Vec3 eyes, long key, AABB box, BlockPos pos) {
        return VISIBILITY_CACHE.computeIfAbsent(key,
                unused -> VisibilityChecks.anyVisible(mc, eyes, box, pos));
    }

    private record ScreenPoint(float x, float y) {}

    public static void draw(GuiGraphicsExtractor g, List<EntityScanner.Target> scanned) {
        var mc = Minecraft.getInstance();
        var cfg = BenchConfig.INSTANCE;
        if (!cfg.espEnabled || mc.level == null || mc.player == null
                || mc.gui.screen() != null) return;

        Camera camera = mc.gameRenderer.mainCamera();
        if (camera == null || !camera.isInitialized()) return;
        final double maxDistSq = (double) cfg.range * cfg.range;
        if (!cfg.espThroughWalls) updateVisibilityCache(mc);
        Vec3 eyes = camera.position();
        int count = 0;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (count >= MAX_MARKERS) break;
            if (e == mc.player || e.isRemoved() || e.distanceToSqr(mc.player) > maxDistSq) continue;
            int color;
            boolean isMob = false;
            if (e instanceof Player && cfg.players) color = PLAYERS_COLOR;
            else if (e instanceof Mob && cfg.mobEspEnabled && e instanceof Enemy && cfg.hostile) {
                color = HOSTILE_COLOR; isMob = true;
            } else if (e instanceof Mob && cfg.mobEspEnabled && !(e instanceof Enemy) && cfg.passive) {
                color = PASSIVE_COLOR; isMob = true;
            } else continue;
            boolean boxes = isMob ? cfg.mobEspBoxes : cfg.espBoxes;
            boolean names = isMob ? cfg.mobEspNames : cfg.espNames;
            if (!boxes && !names && !cfg.espTracers) continue;

            AABB box = e.getBoundingBox();
            if (!cfg.espThroughWalls && !visible(mc, eyes, ((long)e.getId()) << 1, box, null)) continue;
            drawMarker(g, mc, camera, box, cfg.enhancedNametags ? "" : e.getName().getString(),
                    Math.sqrt(e.distanceToSqr(mc.player)), color, cfg,
                    boxes, names, cfg.espTracers);
            count++;
        }
        if (cfg.containers) {
            for (EntityScanner.Target target : scanned) {
                if (count >= MAX_MARKERS) break;
                if (!"CONTAINER".equals(target.category()) || target.distance() > cfg.range) continue;
                double x = target.x(), y = target.y(), z = target.z();
                AABB box = new AABB(x-.5, y-.5, z-.5, x+.5, y+.5, z+.5);
                BlockPos pos = new BlockPos((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
                long key = (pos.asLong() << 1) | 1;
                if (!cfg.espThroughWalls && !visible(mc, eyes, key, box, pos)) continue;
                drawMarker(g, mc, camera, box,
                        cfg.storageEspPlus ? target.name() : "Container", target.distance(),
                        cfg.storageEspPlus ? storageColor(target.name()) : CHEST_COLOR, cfg);
                count++;
            }
        }
    }

    private static int storageColor(String type) {
        return switch (type) {
            case "Ender Chest" -> 0xFFC48BFF;
            case "Shulker Box" -> 0xFFB7A2F7;
            case "Barrel" -> 0xFFE8B075;
            case "Hopper" -> 0xFFBAC9D5;
            case "Furnace" -> 0xFFFF9B66;
            default -> CHEST_COLOR;
        };
    }

    static void drawMarker(GuiGraphicsExtractor g, Minecraft mc, Camera camera,
                                   AABB box, String label, double distance, int color, BenchConfig cfg) {
        drawMarker(g, mc, camera, box, label, distance, color, cfg,
                cfg.espBoxes, cfg.espNames, cfg.espTracers);
    }

    // Styles per module; a mob ESP toggle cannot affect ore outlines or labels.
    static void drawMarker(GuiGraphicsExtractor g, Minecraft mc, Camera camera,
                                   AABB box, String label, double distance, int color, BenchConfig cfg,
                                   boolean boxes, boolean names, boolean tracers) {
        if (!boxes && !names && !tracers) return;
        ScreenPoint[] projected = new ScreenPoint[8];
        boolean anyVisible = false;
        // Order of corners defined by the three bits of i: X=1, Y=2, Z=4.
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? box.minX : box.maxX;
            double y = (i & 2) == 0 ? box.minY : box.maxY;
            double z = (i & 4) == 0 ? box.minZ : box.maxZ;
            projected[i] = project(mc, camera, x, y, z, g.guiWidth(), g.guiHeight());
            if (projected[i] != null) anyVisible = true;
        }
        if (!anyVisible) return;
        if (boxes) {
            for (int[] edge : EDGES) {
                ScreenPoint a = projected[edge[0]], b = projected[edge[1]];
                if (a != null && b != null) line(g, a.x, a.y, b.x, b.y, color);
            }
        }
        ScreenPoint bottom = project(mc, camera, (box.minX + box.maxX)*.5,
                box.minY, (box.minZ + box.maxZ)*.5, g.guiWidth(), g.guiHeight());
        if (tracers && bottom != null)
            line(g, g.guiWidth() * .5f, g.guiHeight() - 8f, bottom.x, bottom.y, color);
        if (names && label != null && !label.isBlank()) {
            ScreenPoint top = project(mc, camera, (box.minX + box.maxX)*.5,
                    box.maxY + .22, (box.minZ + box.maxZ)*.5, g.guiWidth(), g.guiHeight());
            if (top != null && top.x >= 0 && top.x < g.guiWidth() && top.y >= 8 && top.y < g.guiHeight()) {
                String text = label + (cfg.distanceLabels ? " [" + Math.round(distance) + "m]" : "");
                // Don't let an exceptionally long custom name cover the entire HUD.
                if (text.length() > 46) text = text.substring(0,43) + "...";
                int lx = Math.max(0, Math.min(g.guiWidth() - mc.font.width(text),
                        Math.round(top.x) - mc.font.width(text)/2));
                g.text(mc.font, text, lx, Math.round(top.y) - 8, color, true);
            }
        }
    }

    static void drawFloatingLabel(GuiGraphicsExtractor g, Minecraft mc, Camera camera,
            AABB bounds, String label, double distance, int color, boolean distanceLabels) {
        ScreenPoint pt=project(mc,camera,(bounds.minX+bounds.maxX)*.5,bounds.maxY+.35,
                (bounds.minZ+bounds.maxZ)*.5,g.guiWidth(),g.guiHeight());
        if (pt==null || pt.x<0 || pt.x>g.guiWidth() || pt.y<9 || pt.y>g.guiHeight()) return;
        String text=label+(distanceLabels ? " ["+Math.round(distance)+"m]" : "");
        if (text.length()>60) text=text.substring(0,57)+"...";
        int xx=Math.max(0,Math.min(g.guiWidth()-mc.font.width(text),Math.round(pt.x)-mc.font.width(text)/2));
        g.text(mc.font,text,xx,Math.round(pt.y)-8,color,true);
    }

    /** One centered, 3px glyph per ore block. */
    static void drawOreDot(GuiGraphicsExtractor g, Minecraft mc, Camera camera,
                           double x, double y, double z, int color) {
        ScreenPoint p = project(mc,camera,x,y,z,g.guiWidth(),g.guiHeight());
        if (p == null || p.x < 2 || p.x >= g.guiWidth()-2 || p.y < 2 || p.y >= g.guiHeight()-2) return;
        int xx=Math.round(p.x), yy=Math.round(p.y);
        g.fill(xx-1,yy-1,xx+2,yy+2,color);
    }

    private static ScreenPoint project(Minecraft mc, Camera camera, double wx, double wy, double wz,
                                       int screenWidth, int screenHeight) {
        Vec3 pos = camera.position();
        double dx = wx - pos.x, dy = wy - pos.y, dz = wz - pos.z;
        Vector3fc forward = camera.forwardVector();
        // Cull points behind/too near camera to avoid giant inverted boxes.
        double forwardDistance = dx*forward.x() + dy*forward.y() + dz*forward.z();
        if (forwardDistance < .07) return null;
        Vec3 ndc = mc.gameRenderer.projectPointToScreen(new Vec3(wx, wy, wz));
        if (!Double.isFinite(ndc.x) || !Double.isFinite(ndc.y)) return null;
        double sx = (ndc.x + 1d) * .5d * screenWidth;
        double sy = (1d - ndc.y) * .5d * screenHeight;
        if (Math.abs(sx) > 50000 || Math.abs(sy) > 50000) return null;
        return new ScreenPoint((float)sx, (float)sy);
    }

    /** Draw each segment with a single rotated GUI quad instead of many pixels. */
    private static void line(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, int color) {
        var clipped = EspGeometry.clip(x0,y0,x1,y1,g.guiWidth()-1,g.guiHeight()-1);
        if (clipped == null) return;
        float dx = clipped.x1() - clipped.x0();
        float dy = clipped.y1() - clipped.y0();
        float length = (float)Math.hypot(dx,dy);
        if (length < .2f) return;
        g.pose().pushMatrix();
        try {
            g.pose().translate(clipped.x0(), clipped.y0());
            g.pose().rotate((float)Math.atan2(dy,dx));
            g.fill(0,0,Math.max(1,(int)Math.ceil(length)),1,color);
        } finally {
            g.pose().popMatrix();
        }
    }

    private EspOverlay() {}
}
