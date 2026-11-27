package dev.wukong.client.research;
import java.util.*;
/** Bounded local slot history. A missing baseline never creates a change. */
public final class SlotHistory<T> {
    public record Change(String slot, String before, String after) {}
    public record Batch(long sample, List<Change> changes) { public Batch { changes=List.copyOf(changes); } }
    private final int limit;
    private final ArrayDeque<Batch> history=new ArrayDeque<>();
    private Map<String,T> previous;
    private long discarded;
    public SlotHistory(int limit){if(limit<1)throw new IllegalArgumentException();this.limit=limit;}
    public void observe(long sample, Map<String,T> current, java.util.function.BiPredicate<T,T> equal, java.util.function.Function<T,String> describe){
        var now=Map.copyOf(current);
        if(previous!=null){
            var keys=new TreeSet<>(previous.keySet());keys.addAll(now.keySet());
            var changes=new ArrayList<Change>();
            for(String key:keys){T a=previous.get(key),b=now.get(key);
                if(a==null&&b==null || a!=null&&b!=null&&equal.test(a,b))continue;
                String before=a==null?"Empty":describe.apply(a), after=b==null?"Empty":describe.apply(b);
                if(a!=null&&b!=null&&before.equals(after))after+=" (details changed)";
                changes.add(new Change(key,before,after));
            }
            if(!changes.isEmpty()){
                if(history.size()==limit){history.removeFirst();discarded++;}
                history.addLast(new Batch(sample,changes));
            }
        }
        previous=now;
    }
    public void pause(){previous=null;}
    public void clear(){pause();history.clear();discarded=0;}
    public List<Batch> history(){return List.copyOf(history);}
    public long discarded(){return discarded;}
}
