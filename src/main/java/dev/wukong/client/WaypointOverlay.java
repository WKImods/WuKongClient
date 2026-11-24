package dev.wukong.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/** World-anchored waypoint glyphs drawn on the GUI layer; no world edits or packets. */
public final class WaypointOverlay {
    public static void draw(GuiGraphicsExtractor g) {
        Minecraft mc=Minecraft.getInstance();
        BenchConfig cfg=BenchConfig.INSTANCE;
        if (!cfg.espEnabled || mc.player==null || mc.level==null || cfg.waypoints==null) return;
        Camera camera=mc.gameRenderer.mainCamera();
        if (camera==null || !camera.isInitialized()) return;
        String dimension=WaypointManager.currentDimension(mc);
        int shown=0;
        for (BenchConfig.Waypoint wp : cfg.waypoints) {
            if (wp==null || !wp.showInWorld || !dimension.equals(wp.dimension)) continue;
            if (wp.death ? !cfg.deathMarkerEnabled : !cfg.waypointsEnabled) continue;
            if (shown++>=96) break;
            double x=wp.x+.5, y=wp.y+1.3, z=wp.z+.5;
            Vec3 pos=camera.position();
            Vector3fc forward=camera.forwardVector();
            double dx=x-pos.x, dy=y-pos.y, dz=z-pos.z;
            if (dx*forward.x()+dy*forward.y()+dz*forward.z() <= .10) continue;
            Vec3 point=mc.gameRenderer.projectPointToScreen(new Vec3(x,y,z));
            if (!Double.isFinite(point.x) || !Double.isFinite(point.y)) continue;
            int px=(int)Math.round((point.x+1)*.5*g.guiWidth());
            int py=(int)Math.round((1-point.y)*.5*g.guiHeight());
            if (px<1 || px>=g.guiWidth()-1 || py<12 || py>=g.guiHeight()-13) continue;
            double distance=Math.sqrt(dx*dx+dy*dy+dz*dz);
            String name=(wp.name==null || wp.name.isBlank()) ? "Waypoint" : wp.name;
            if (name.length()>26) name=name.substring(0,23)+"...";
            String line=WaypointIcons.glyph(wp.icon) + " " + name + " [" + Math.round(distance) + "m]";
            int tw=mc.font.width(line);
            int left=Math.max(2,Math.min(g.guiWidth()-tw-6,px-tw/2-3));
            g.fill(left,py-12,left+tw+6,py+2,0xC314202B);
            g.fill(left,py-12,left+2,py+2,WaypointIcons.color(wp.icon));
            g.text(mc.font,line,left+4,py-10,WaypointIcons.color(wp.icon),true);
        }
    }
    private WaypointOverlay() {}
}
