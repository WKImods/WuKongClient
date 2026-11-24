package dev.wukong.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/** Draws independent optional ESP overlays, respecting the global master and visibility. */
public final class ExtraEspOverlay {
    private static final int ITEM_COLOR = 0xFFEFE087;
    private static final int SPAWNER_COLOR = 0xFFFF57CC;
    private static final Map<Long,Boolean> VISIBLE = new HashMap<>();
    private static Object oldWorld;
    private static int oldTick = Integer.MIN_VALUE;

    private static boolean visible(Minecraft mc,Vec3 eye,long key,AABB box,BlockPos pos) {
        if (oldWorld!=mc.level || oldTick!=mc.player.tickCount) {
            VISIBLE.clear();oldWorld=mc.level;oldTick=mc.player.tickCount;
        }
        return VISIBLE.computeIfAbsent(key,unused->VisibilityChecks.anyVisible(mc,eye,box,pos));
    }

    public static void draw(GuiGraphicsExtractor g, List<EntityScanner.Target> scanned) {
        var mc=Minecraft.getInstance();
        var cfg=BenchConfig.INSTANCE;
        if (!cfg.espEnabled || mc.player==null || mc.level==null) return;
        Camera camera=mc.gameRenderer.mainCamera();
        if (camera==null || !camera.isInitialized()) return;
        double maxSq=(double)cfg.range*cfg.range;
        Vec3 eyes=camera.position();
        if (cfg.itemEspEnabled || cfg.enhancedNametags) {
            int count=0;
            for (Entity e:mc.level.entitiesForRendering()) {
                if (count>=100 || e==mc.player || e.isRemoved() || e.distanceToSqr(mc.player)>maxSq) continue;
                if (e instanceof ItemEntity item && cfg.itemEspEnabled) {
                    if (!cfg.espThroughWalls && !visible(mc,eyes,((long)e.getId())<<1,e.getBoundingBox(),null)) continue;
                    var stack=item.getItem();
                    if (stack.isEmpty()) continue;
                    String label=stack.getHoverName().getString()+" x"+stack.getCount();
                    EspOverlay.drawMarker(g,mc,camera,e.getBoundingBox(),label,
                            Math.sqrt(e.distanceToSqr(mc.player)),ITEM_COLOR,cfg);
                    count++;
                } else if (cfg.enhancedNametags && e instanceof LivingEntity living
                        && (e instanceof Player || e instanceof Mob)) {
                    boolean tagged = (e instanceof Player && cfg.players)
                            || (e instanceof Mob && cfg.mobEspEnabled && cfg.mobEspNames
                                && (e instanceof Enemy ? cfg.hostile : cfg.passive));
                    if (!tagged) continue;
                    if (!cfg.espThroughWalls && !visible(mc,eyes,((long)e.getId())<<1,e.getBoundingBox(),null)) continue;
                    String name=e.getName().getString();
                    String stats=" " + (int)Math.ceil(living.getHealth()) + " HP";
                    if (e instanceof Player) stats += " | " + living.getArmorValue() + " Armor";
                    EspOverlay.drawFloatingLabel(g,mc,camera,e.getBoundingBox(),name+stats,
                            Math.sqrt(e.distanceToSqr(mc.player)),0xFFD7E9FF,cfg.distanceLabels);
                    count++;
                }
            }
        }
        if (cfg.spawnerEspEnabled) {
            int count=0;
            for (var t:scanned) {
                if (!"SPAWNER".equals(t.category()) || t.distance()>cfg.range || count++>=70) continue;
                double x=t.x(), y=t.y(), z=t.z();
                BlockPos pos=new BlockPos((int)Math.floor(x),(int)Math.floor(y),(int)Math.floor(z));
                AABB box=new AABB(x-.5,y-.5,z-.5,x+.5,y+.5,z+.5);
                if (!cfg.espThroughWalls && !visible(mc,eyes,(pos.asLong()<<1)|1,box,pos)) continue;
                EspOverlay.drawMarker(g,mc,camera,box,t.name(),t.distance(),SPAWNER_COLOR,cfg);
            }
        }
    }
    private ExtraEspOverlay() {}
}
