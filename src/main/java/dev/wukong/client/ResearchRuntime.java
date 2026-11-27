package dev.wukong.client;
import dev.wukong.client.research.ResearchLedger;
import dev.wukong.client.research.DetectionEngine;
import dev.wukong.client.research.SessionMetrics;
import dev.wukong.client.research.SlotHistory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.*;
/** Main-thread, read-only inventory and latency observations. */
public final class ResearchRuntime {
    private static ResearchLedger ledger=new ResearchLedger(256);
    private static final DetectionEngine detection = new DetectionEngine(128);
    public static DetectionEngine detection(){return detection;}
    private static final SessionMetrics metrics = new SessionMetrics(120);
    private static Map<String,ItemStack> icons = Map.of();
    private static List<String> equipment = List.of();
    public static SessionMetrics metrics(){return metrics;}
    public static ItemStack icon(String id){return icons.getOrDefault(id,ItemStack.EMPTY);}
    public static List<String> equipment(){return equipment;}
    private static final SlotHistory<ItemStack> slotHistory = new SlotHistory<>(64);
    public record ItemRow(String location, String id, ItemStack stack, long count, String nested) {}
    private static List<ItemRow> slots=List.of(), groups=List.of(), menuRows=List.of();
    private static String menuName="";
    private static boolean menuTruncated, menuOpen;
    public static boolean menuOpen(){return menuOpen;}
    public static List<ItemRow> slots(){return slots;}
    public static List<ItemRow> groups(){return groups;}
    public static List<ItemRow> menuRows(){return menuRows;}
    public static String menuName(){return menuName;}
    public static boolean menuTruncated(){return menuTruncated;}
    public static SlotHistory<ItemStack> slotHistory(){return slotHistory;}
    private static String id(ItemStack s){return BuiltInRegistries.ITEM.getKey(s.getItem()).toString();}
    private static String describe(ItemStack s){return s.getCount()+" x "+s.getHoverName().getString()+" ["+id(s)+"]";}
    private static boolean equal(ItemStack a,ItemStack b){return a.getCount()==b.getCount()&&ItemStack.isSameItemSameComponents(a,b);}
    private static String nested(ItemStack s){
        var contents=s.get(DataComponents.CONTAINER);
        if(contents!=null){var items=contents.itemCopies().limit(257).toList();
            long n=items.stream().limit(256).filter(v->!v.isEmpty()).count();
            return "Container component: "+n+" occupied entries"+(items.size()>256?" (partial)":"");}
        var bundle=s.get(DataComponents.BUNDLE_CONTENTS);
        if(bundle!=null){var items=bundle.itemCopies().limit(257).toList();return "Bundle component: "+Math.min(256,items.size())+" entries"+(items.size()>256?" (partial)":"");}
        return "";
    }
    private static ItemRow row(String location, ItemStack s){return new ItemRow(location,id(s),s.copy(),s.getCount(),nested(s));}
    private static void clearViews(){slots=List.of();groups=List.of();menuRows=List.of();menuName="";menuTruncated=false;menuOpen=false;}
    private static void captureDetails(Minecraft mc){
        var local=new ArrayList<ItemRow>();var grouped=new ArrayList<ItemRow>();var observed=new TreeMap<String,ItemStack>();
        for(int i=0;i<36;i++){
            var s=mc.player.getInventory().getItem(i);if(s.isEmpty())continue;
            String loc=String.format(Locale.ROOT,"%s %02d",i<9?"Hotbar":"Main",i);
            var r=row(loc,s);local.add(r);observed.put(loc,r.stack());
            // Group by exact item/components, never by a lossy hash. Count is displayed separately.
            int match=-1;for(int j=0;j<grouped.size();j++)if(ItemStack.isSameItemSameComponents(grouped.get(j).stack(),s)){match=j;break;}
            if(match<0)grouped.add(row(loc,s));else{
                var old=grouped.get(match);
                grouped.set(match,new ItemRow(old.location()+", "+i,old.id(),old.stack(),old.count()+s.getCount(),old.nested()));
            }
        }
        for(var e:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
            var s=mc.player.getItemBySlot(e);if(!s.isEmpty())local.add(row(e.name(),s));
        }
        var off=mc.player.getOffhandItem();if(!off.isEmpty())local.add(row("OFFHAND",off));
        var cursor=mc.player.containerMenu.getCarried();if(!cursor.isEmpty())local.add(row("CURSOR",cursor));
        slots=List.copyOf(local);groups=List.copyOf(grouped);
        slotHistory.observe(ledger.samples(),observed,ResearchRuntime::equal,ResearchRuntime::describe);
        var menu=mc.player.containerMenu;var visible=new ArrayList<ItemRow>();menuOpen=menu!=mc.player.inventoryMenu;
        if(menuOpen){
            menuName=menu.getClass().getSimpleName();menuTruncated=menu.slots.size()>256;
            for(int i=0;i<Math.min(256,menu.slots.size());i++){
                var slot=menu.slots.get(i);
                if(slot.container==mc.player.getInventory())continue;
                var stack=slot.getItem();if(!stack.isEmpty())visible.add(row("Menu slot "+i,stack));
            }
            menuRows=List.copyOf(visible);
        }
    }
    private static Object world, player;
    private static String profile="";
    private static int tick;
    private static boolean installed;
    public static ResearchLedger ledger(){return ledger;}
    public static void reset(){ledger=new ResearchLedger(256);detection.clear();tick=0;metrics.clear();icons=Map.of();equipment=List.of();slotHistory.clear();clearViews();}
    public static void install(){if(!installed){installed=true;ClientTickEvents.END_CLIENT_TICK.register(ResearchRuntime::tick);}}
    private static void tick(Minecraft mc){
        String next=ServerProfiles.activeFileLabel();
        if(world!=mc.level||player!=mc.player||!Objects.equals(profile,next)){
            reset();world=mc.level;player=mc.player;profile=next;
        }
        while(WuKongKeys.RESEARCH_OPEN.consumeClick()) if(!WuKongKeys.RESEARCH_OPEN.isUnbound() && mc.gui.screen()==null) mc.gui.setScreen(new ResearchScreen(new BenchScreen()));
        while(WuKongKeys.RESEARCH_MONITOR.consumeClick()) if(!WuKongKeys.RESEARCH_MONITOR.isUnbound() && mc.player!=null && mc.gui.screen()==null){
            BenchConfig.INSTANCE.researchMonitor=!BenchConfig.INSTANCE.researchMonitor;BenchConfig.INSTANCE.save();
        }
        if(!BenchConfig.INSTANCE.researchMonitor||mc.player==null||mc.level==null){ledger.pause();detection.pause();metrics.clear();icons=Map.of();equipment=List.of();slotHistory.pause();clearViews();tick=0;return;}
        if(++tick%10!=0)return;
        var connection=mc.getConnection();
        var info=connection==null?null:connection.getPlayerInfo(mc.player.getUUID());
        metrics.observe(mc.hasSingleplayerServer()||info==null?-1:info.getLatency());
        var counts=new HashMap<String,Long>();var nextIcons=new HashMap<String,ItemStack>();int occupied=0,invalid=0;
        for(int i=0;i<36;i++){
            var stack=mc.player.getInventory().getItem(i);
            if(stack.isEmpty())continue;
            occupied++;if(stack.getCount()>stack.getMaxStackSize())invalid++;
            String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            counts.merge(id,(long)stack.getCount(),Long::sum);
            if(!nextIcons.containsKey(id))nextIcons.put(id,stack.getItem().getDefaultInstance());
        }
        icons=Map.copyOf(nextIcons);
        var details=new ArrayList<String>();
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
            var stack=mc.player.getItemBySlot(slot);
            if(!stack.isEmpty())details.add(slot.name()+": "+stack.getHoverName().getString());
        }
        var off=mc.player.getOffhandItem();
        if(!off.isEmpty())details.add("OFFHAND: "+off.getHoverName().getString()+" x"+off.getCount());
        var cursor=mc.player.containerMenu.getCarried();
        if(!cursor.isEmpty())details.add("CURSOR: "+cursor.getHoverName().getString()+" x"+cursor.getCount());
        equipment=List.copyOf(details);
        ledger.observe(new ResearchLedger.Snapshot(counts,occupied,invalid,mc.player.containerMenu!=mc.player.inventoryMenu));
        captureDetails(mc);
        var viewCounts=new TreeMap<String,Long>();int viewSlots=0;
        for(var r:slots){
            if(!r.location().startsWith("Hotbar ")&&!r.location().startsWith("Main "))continue;
            viewCounts.merge(r.id(),r.count(),Long::sum);viewSlots++;
        }
        detection.observe(ledger.samples(),ledger.latest(),viewCounts,viewSlots);
    }
    private ResearchRuntime(){}
}
