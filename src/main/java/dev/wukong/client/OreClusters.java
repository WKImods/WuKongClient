package dev.wukong.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure geometry: face-connected blocks of the SAME kind form one ore vein. */
public final class OreClusters {
    public record Point(int x, int y, int z, String kind) {}
    private record Pos(int x, int y, int z) {}
    public record Cluster(String kind, List<Point> blocks, int minX, int minY, int minZ,
                          int maxX, int maxY, int maxZ) {
        public int count() { return blocks.size(); }
    }
    private static final int[][] NEIGHBORS = {
        {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}
    };

    public static List<Cluster> group(Collection<Point> candidates) {
        Map<Pos, Point> remaining = new HashMap<>();
        for (Point p : candidates) remaining.put(new Pos(p.x(),p.y(),p.z()),p);
        List<Cluster> result = new ArrayList<>();
        while (!remaining.isEmpty()) {
            Point start = remaining.values().iterator().next();
            remaining.remove(new Pos(start.x(),start.y(),start.z()));
            ArrayDeque<Point> queue = new ArrayDeque<>();
            List<Point> vein = new ArrayList<>();
            queue.add(start);
            int minX=start.x(), maxX=start.x();
            int minY=start.y(), maxY=start.y();
            int minZ=start.z(), maxZ=start.z();
            while (!queue.isEmpty()) {
                Point current = queue.removeFirst();
                vein.add(current);
                minX = Math.min(minX,current.x()); maxX = Math.max(maxX,current.x());
                minY = Math.min(minY,current.y()); maxY = Math.max(maxY,current.y());
                minZ = Math.min(minZ,current.z()); maxZ = Math.max(maxZ,current.z());
                for (int[] offset : NEIGHBORS) {
                    Pos otherPos = new Pos(current.x()+offset[0], current.y()+offset[1], current.z()+offset[2]);
                    Point neighbor = remaining.get(otherPos);
                    if (neighbor != null && neighbor.kind().equals(start.kind())) {
                        remaining.remove(otherPos);
                        queue.addLast(neighbor);
                    }
                }
            }
            result.add(new Cluster(start.kind(),List.copyOf(vein), minX,minY,minZ,maxX,maxY,maxZ));
        }
        result.sort(Comparator.comparingInt(Cluster::count).reversed());
        return result;
    }
    private OreClusters() {}
}
