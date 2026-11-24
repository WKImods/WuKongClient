import dev.wukong.client.OreClusters;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OreClustersTest {
    public static void main(String[] args) {
        var blocks = List.of(
            new OreClusters.Point(15,10,0,"DIAMOND"),
            new OreClusters.Point(16,10,0,"DIAMOND"), // connected across chunk border
            new OreClusters.Point(16,11,0,"DIAMOND"),
            new OreClusters.Point(17,12,0,"DIAMOND"), // diagonal, separate vein
            new OreClusters.Point(16,10,1,"EMERALD"), // adjacent but a different kind
            new OreClusters.Point(-7,2,-1,"COPPER"),
            new OreClusters.Point(-8,2,-1,"COPPER")
        );
        var groups = OreClusters.group(blocks);
        if (groups.size() != 4) throw new AssertionError("Expected 4 groups, got "+groups.size());
        var sizes = groups.stream().map(OreClusters.Cluster::count).sorted().toList();
        if (!sizes.equals(List.of(1,1,2,3))) throw new AssertionError("Wrong sizes "+sizes);
        var largest=groups.stream().filter(g->g.count()==3).findFirst().orElseThrow();
        if (!largest.kind().equals("DIAMOND") || largest.minX()!=15 || largest.maxX()!=16 || largest.minY()!=10 || largest.maxY()!=11)
            throw new AssertionError("Bounds / kind incorrect: "+largest);
        var ids = groups.stream().map(OreClusters.Cluster::kind).collect(Collectors.toSet());
        if (!ids.equals(Set.of("DIAMOND","EMERALD","COPPER"))) throw new AssertionError("Types incorrect");
        if (!OreClusters.group(List.of()).isEmpty()) throw new AssertionError("Empty input not empty output");
        if (OreClusters.group(List.of(blocks.get(0),blocks.get(0))).getFirst().count()!=1)
            throw new AssertionError("Duplicate cells counted twice");
        System.out.println("OreClusters: PASS (connected faces, chunk borders, same kind, negatives, diagonals, empty, duplicate)");
    }
}
