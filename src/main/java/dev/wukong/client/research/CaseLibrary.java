package dev.wukong.client.research;
import java.util.*;
/** Historical evidence is deliberately separate from executable synthetic fixtures. */
public final class CaseLibrary {
    public static final int SCHEMA = 1;
    public static final String TARGET = "26.3";
    public enum Status { HISTORICAL_FIXED, UNKNOWN, SYNTHETIC }
    public record HistoricalCase(String id, String category, String documentedFix, String fixedRelease, String source) {
        public Status status(String exactVersion) {
            return compareRelease(exactVersion, fixedRelease) >= 0 && parse(exactVersion) != null
                ? Status.HISTORICAL_FIXED : Status.UNKNOWN;
        }
    }
    public static List<HistoricalCase> historical() {
        return List.of(
            new HistoricalCase("MC-161754", "Entity inventory", "Documented duplication defect", "1.16", "https://www.minecraft.net/en-us/article/nether-update-java"),
            new HistoricalCase("MC-275228", "Creative hotbar", "Documented duplication defect", "1.21.2", "https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-2"),
            new HistoricalCase("MC-275239", "Creative inventory screen", "Documented duplication defect", "1.21.2", "https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-2")
        );
    }
    private static int[] parse(String s) {
        if (s == null || !s.matches("[0-9]{1,4}\\.[0-9]{1,4}(\\.[0-9]{1,4})?")) return null;
        String[] parts=s.split("\\."); int[] out=new int[3];
        for(int i=0;i<parts.length;i++) out[i]=Integer.parseInt(parts[i]);
        return out;
    }
    private static int compareRelease(String a,String b) {
        int[] x=parse(a), y=parse(b); if(x==null||y==null)return -1;
        for(int i=0;i<3;i++)if(x[i]!=y[i])return Integer.compare(x[i],y[i]); return 0;
    }
    private CaseLibrary() {}
}
