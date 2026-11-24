package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Bounded, client-thread-only ore scan. Never requests or loads chunks from the server. */
public final class OreScanner {
    private static final int SECTIONS_PER_TICK = 6;
    private static final int RESCAN_AFTER_TICKS = 160;
    private static final int MAX_CACHED_BLOCKS = 12000;

    private record Key(int chunkX, int sectionY, int chunkZ) {}
    private record Snapshot(int tick, List<OreClusters.Point> blocks) {}
    private static final Map<Key, Snapshot> CACHE = new HashMap<>();
    private static final ArrayDeque<Key> TODO = new ArrayDeque<>();
    private static final Set<Key> QUEUED = new HashSet<>();
    private static List<OreClusters.Cluster> clusters = List.of();
    private static Object lastLevel;
    private static int ticks;
    private static int lastChunkX = Integer.MIN_VALUE, lastChunkZ = Integer.MIN_VALUE;
    private static int lastSectionY = Integer.MIN_VALUE, lastRange = -1;
    private static boolean dirty;

    public static List<OreClusters.Cluster> clusters() { return clusters; }

    private static void clear() {
        CACHE.clear(); TODO.clear(); QUEUED.clear(); clusters = List.of();
        lastChunkX=Integer.MIN_VALUE; lastChunkZ=Integer.MIN_VALUE;
        lastSectionY=Integer.MIN_VALUE; lastRange=-1;
        ticks = 0; dirty = false;
    }

    public static void tick(Minecraft mc) {
        if (mc.level != lastLevel) { clear(); lastLevel = mc.level; }
        if (mc.player == null || mc.level == null) return;
        BenchConfig cfg = BenchConfig.INSTANCE;
        if (!cfg.espEnabled || !cfg.oreFinderEnabled) return;
        ticks++;
        int range = Math.max(16, Math.min(64, cfg.oreRange));
        int px = mc.player.blockPosition().getX();
        int py = mc.player.blockPosition().getY();
        int pz = mc.player.blockPosition().getZ();
        int cx = px >> 4, cz = pz >> 4, sy = py >> 4;
        boolean moved = cx != lastChunkX || cz != lastChunkZ || sy != lastSectionY || range != lastRange;
        if (moved) {
            TODO.clear(); QUEUED.clear();
            lastChunkX=cx; lastChunkZ=cz; lastSectionY=sy; lastRange=range;
        }
        if (moved || ticks % 20 == 1) schedule(mc, px, py, pz, range);
        for (int i = 0; i < SECTIONS_PER_TICK && !TODO.isEmpty(); i++) {
            Key key = TODO.removeFirst();
            QUEUED.remove(key);
            LevelChunk chunk = mc.level.getChunkSource().getChunk(key.chunkX(), key.chunkZ(), false);
            if (chunk == null) {
                if (CACHE.remove(key) != null) dirty = true;
                continue;
            }
            int index = chunk.getSectionIndexFromSectionY(key.sectionY());
            if (index < 0 || index >= chunk.getSections().length) continue;
            LevelChunkSection section = chunk.getSection(index);
            List<OreClusters.Point> found = new ArrayList<>();
            if (!section.hasOnlyAir() && section.maybeHas(s -> OreKind.from(s) != null)) {
                int bx = key.chunkX() << 4, by = key.sectionY() << 4, bz = key.chunkZ() << 4;
                long rr = (long)range * range;
                for (int lx = 0; lx < 16; lx++) for (int ly = 0; ly < 16; ly++) for (int lz = 0; lz < 16; lz++) {
                    int wx=bx+lx, wy=by+ly, wz=bz+lz;
                    long dx=wx-px,dy=wy-py,dz=wz-pz;
                    if (dx*dx+dy*dy+dz*dz > rr) continue;
                    OreKind ore = OreKind.from(section.getBlockState(lx,ly,lz));
                    if (ore != null) found.add(new OreClusters.Point(wx,wy,wz,ore.name()));
                }
            }
            CACHE.put(key,new Snapshot(ticks,found));
            dirty = true;
        }
        if (dirty && ticks % 5 == 0) rebuild();
    }

    private static void schedule(Minecraft mc, int px, int py, int pz, int range) {
        int rChunks = (range + 15) / 16;
        int cx = px >> 4, cz = pz >> 4;
        int minY = Math.max(mc.level.getMinSectionY(), (py - range) >> 4);
        int maxY = Math.min(mc.level.getMaxSectionY(), (py + range) >> 4);
        final long rr = (long)range * range;
        // Discard positions outside range after movement or chunk unloads; never display stale cross-world data.
        if (CACHE.entrySet().removeIf(entry -> {
            Key key=entry.getKey();
            return cubeDistanceSq(key,px,py,pz) > rr ||
                   mc.level.getChunkSource().getChunk(key.chunkX(),key.chunkZ(),false) == null;
        })) dirty=true;
        List<Key> nearest = new ArrayList<>();
        for (int x = cx-rChunks; x <= cx+rChunks; x++) for (int z=cz-rChunks; z <= cz+rChunks; z++) {
            if (mc.level.getChunkSource().getChunk(x,z,false)==null) continue;
            for (int y=minY; y<=maxY; y++) {
                Key key = new Key(x,y,z);
                if (cubeDistanceSq(key,px,py,pz) > rr) continue;
                Snapshot old = CACHE.get(key);
                if ((old == null || ticks-old.tick() >= RESCAN_AFTER_TICKS) && !QUEUED.contains(key)) nearest.add(key);
            }
        }
        nearest.sort(Comparator.comparingLong(k -> cubeDistanceSq(k,px,py,pz)));
        for (Key key : nearest) if (QUEUED.add(key)) TODO.addLast(key);
    }

    /** Squared distance from player to closest point in this 16x16x16 section. */
    private static long cubeDistanceSq(Key k, int px, int py, int pz) {
        long dx = axisDistance(px,k.chunkX()<<4);
        long dy = axisDistance(py,k.sectionY()<<4);
        long dz = axisDistance(pz,k.chunkZ()<<4);
        return dx*dx+dy*dy+dz*dz;
    }
    private static long axisDistance(int pos, int min) {
        if (pos < min) return (long)min-pos;
        if (pos > min+15) return (long)pos-(min+15);
        return 0;
    }

    private static void rebuild() {
        List<OreClusters.Point> blocks = new ArrayList<>();
        for (Snapshot snap : CACHE.values()) {
            if (blocks.size() >= MAX_CACHED_BLOCKS) break;
            int to = Math.min(snap.blocks().size(), MAX_CACHED_BLOCKS-blocks.size());
            blocks.addAll(snap.blocks().subList(0,to));
        }
        clusters = OreClusters.group(blocks);
        dirty=false;
    }
    private OreScanner() {}
}
