package dev.wukong.client;

import java.util.*;

/** Pure inventory planner. Only ordinary left-click operations; each batch ends with an empty cursor. */
final class InventorySortPlan {
    enum Order { TYPE, NAME, AMOUNT, MOD }
    record Variant(String id, String name, int max) {
        Variant { if(max<1 || id==null || name==null) throw new IllegalArgumentException("Invalid variant"); }
    }
    record Stack(int variant, int count) {
        static final Stack EMPTY=new Stack(-1,0);
        Stack { if(count<0 || (count==0)!=(variant==-1) || variant< -1) throw new IllegalArgumentException("Invalid stack"); }
        boolean empty(){return count==0;}
    }
    record State(List<Stack> slots, Stack cursor) { State { slots=List.copyOf(slots); } }
    record Click(int slot, State expected) {}
    record Batch(List<Click> clicks) { Batch { clicks=List.copyOf(clicks); } }
    record Plan(List<Batch> batches, State initial, State result) { Plan { batches=List.copyOf(batches); } }
    static Order order(String value){try{return Order.valueOf(value);}catch(RuntimeException e){return Order.TYPE;}}
    private static String namespace(String id){int n=id.indexOf(':');return n<0?"":id.substring(0,n);}
    static Plan plan(List<Stack> input,List<Variant> variants,int locked,Order order,boolean merge){
        if(input.size()!=27)throw new IllegalArgumentException("Expected 27 main slots");
        for(var s:input)if(!s.empty()&&(s.variant()>=variants.size()||s.count()>variants.get(s.variant()).max()))throw new IllegalArgumentException("Unsupported stack size");
        var sim=new Simulation(input,variants,locked);
        State initial=sim.state();
        var available=new ArrayList<Integer>();
        for(int i=0;i<27;i++)if((locked&(1<<i))==0)available.add(i);
        if(merge)for(int i=0;i<available.size();i++){
            int a=available.get(i);var dest=sim.slots.get(a);
            if(dest.empty())continue;
            for(int j=i+1;j<available.size();j++){
                dest=sim.slots.get(a);if(dest.count()>=variants.get(dest.variant()).max())break;
                int b=available.get(j);var source=sim.slots.get(b);
                if(source.empty()||source.variant()!=dest.variant())continue;
                sim.transfer(b,a);
            }
        }
        long[] totals=new long[variants.size()];
        for(int i:available){var s=sim.slots.get(i);if(!s.empty())totals[s.variant()]+=s.count();}
        Comparator<Integer> id=Comparator.comparing(i->variants.get(i).id());
        Comparator<Integer> name=Comparator.comparing(i->variants.get(i).name(),String.CASE_INSENSITIVE_ORDER);
        Comparator<Integer> cmp=switch(order){
            case TYPE -> id.thenComparing(name);
            case NAME -> name.thenComparing(id);
            case MOD -> Comparator.<Integer,String>comparing(i->namespace(variants.get(i).id())).thenComparing(name).thenComparing(id);
            case AMOUNT -> Comparator.<Integer>comparingLong(i->totals[i]).reversed().thenComparing(id).thenComparing(name);
        };
        cmp=cmp.thenComparingInt(Integer::intValue);
        var desired=new ArrayList<Integer>();
        for(int i:available)desired.add(sim.slots.get(i).variant());
        Comparator<Integer> finalCmp=cmp;
        desired.sort((a,b)->a.equals(b)?0:a==-1?1:b==-1?-1:finalCmp.compare(a,b));
        for(int i=0;i<available.size();i++){
            int a=available.get(i),want=desired.get(i);
            if(sim.slots.get(a).variant()==want)continue;
            if(want==-1)throw new IllegalStateException("Unexpected empty target");
            int b=-1;
            for(int j=i+1;j<available.size();j++)if(sim.slots.get(available.get(j)).variant()==want){b=available.get(j);break;}
            if(b<0)throw new IllegalStateException("Missing target variant");
            sim.transfer(b,a); // different variant or empty destination, never a merge here
        }
        return new Plan(sim.batches,initial,sim.state());
    }
    private static final class Simulation {
        final ArrayList<Stack> slots; final List<Variant> variants; final int locked;
        Stack cursor=Stack.EMPTY; final ArrayList<Batch> batches=new ArrayList<>();
        Simulation(List<Stack> slots,List<Variant> variants,int locked){this.slots=new ArrayList<>(slots);this.variants=variants;this.locked=locked;}
        State state(){return new State(slots,cursor);}
        void transfer(int source,int dest){
            if(!cursor.empty()||slots.get(source).empty()||source==dest)throw new IllegalStateException("Bad batch");
            var clicks=new ArrayList<Click>(); click(source,clicks);click(dest,clicks);if(!cursor.empty())click(source,clicks);
            if(!cursor.empty())throw new IllegalStateException("Cursor not empty");
            batches.add(new Batch(clicks));
        }
        void click(int slot,List<Click> clicks){
            if((locked&(1<<slot))!=0)throw new IllegalStateException("Protected slot");
            Stack old=slots.get(slot);
            if(cursor.empty()||old.empty()||cursor.variant()!=old.variant()) {slots.set(slot,cursor);cursor=old;}
            else {
                int move=Math.min(cursor.count(),variants.get(old.variant()).max()-old.count());
                slots.set(slot,new Stack(old.variant(),old.count()+move));
                int remaining=cursor.count()-move;cursor=remaining==0?Stack.EMPTY:new Stack(cursor.variant(),remaining);
            }
            clicks.add(new Click(slot,state()));
        }
    }
    private InventorySortPlan(){}
}
