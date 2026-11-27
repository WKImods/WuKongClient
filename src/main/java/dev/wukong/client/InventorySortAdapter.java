package dev.wukong.client;

import java.util.*;
import java.util.function.IntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/** Multiplayer: ordinary menu clicks, no raw packets, no retries, no direct inventory writes. */
final class InventorySortAdapter {
    record Model(List<ItemStack> representatives, InventorySortPlan.Plan plan) {
        ItemStack stack(InventorySortPlan.Stack token){
            if(token.empty())return ItemStack.EMPTY;
            ItemStack copy=representatives.get(token.variant()).copy();copy.setCount(token.count());return copy;
        }
        boolean matches(ItemStack actual,InventorySortPlan.Stack expected){
            return expected.empty()?actual.isEmpty():!actual.isEmpty()&&actual.getCount()==expected.count()&&ItemStack.isSameItemSameComponents(actual,representatives.get(expected.variant()));
        }
    }
    static Model build(IntFunction<ItemStack> source,int locked,String order,boolean merge){
        var reps=new ArrayList<ItemStack>();var variants=new ArrayList<InventorySortPlan.Variant>();var slots=new ArrayList<InventorySortPlan.Stack>();
        for(int i=9;i<36;i++){
            ItemStack s=source.apply(i);if(s.isEmpty()){slots.add(InventorySortPlan.Stack.EMPTY);continue;}
            int n=0;while(n<reps.size()&&!ItemStack.isSameItemSameComponents(s,reps.get(n)))n++;
            if(n==reps.size()){reps.add(s.copy());variants.add(new InventorySortPlan.Variant(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(),s.getHoverName().getString(),s.getMaxStackSize()));}
            slots.add(new InventorySortPlan.Stack(n,s.getCount()));
        }
        return new Model(List.copyOf(reps),InventorySortPlan.plan(slots,variants,locked,InventorySortPlan.order(order),merge));
    }
    private static Session session;
    private static final class Session {
        final Object player,world,menu,screen;final Model model;final String profile,order; final int locked; final boolean merge;
        InventorySortPlan.State expected;int next,wait;
        Session(Minecraft mc,Model model){this.model=model;expected=model.plan().initial();player=mc.player;world=mc.level;menu=mc.player.inventoryMenu;screen=mc.gui.screen();profile=ServerProfiles.activeFileLabel();var cfg=BenchConfig.INSTANCE;locked=cfg.inventorySortLocked;merge=cfg.inventorySortMerge;order=cfg.inventorySortOrder;}
    }
    private static String tr(String de,String en){return WuKongLanguage.text(de,en);}
    private static void notice(String s){EvolutionUi.note(s);}
    static boolean active(){return session!=null;}
    static void cancel(){session=null;notice(tr("Sortierung abgebrochen. Cursor prüfen.","Sorting cancelled. Check cursor."));}
    static void start(Minecraft mc){
        if(active()){cancel();return;}
        var p=mc.player;
        if(p==null||mc.level==null||mc.gameMode==null||!p.isAlive()||p.hasInfiniteMaterials()||p.containerMenu!=p.inventoryMenu||!p.inventoryMenu.getCarried().isEmpty()) {notice(tr("Inventar nicht bereit.","Inventory not ready."));return;}
        if(!(mc.gui.screen() instanceof InventoryScreen))mc.gui.setScreen(new InventoryScreen(p));
        if(!(mc.gui.screen() instanceof InventoryScreen))return;
        var cfg=BenchConfig.INSTANCE;
        try{
            var model=build(i->p.getInventory().getItem(i),cfg.inventorySortLocked,cfg.inventorySortOrder,cfg.inventorySortMerge);
            session=new Session(mc,model);
            if(!matches(mc,session,session.expected)){cancel();return;}
            notice(tr("Inventarsortierung gestartet.","Inventory sorting started."));
        }catch(RuntimeException e){session=null;notice(tr("Sortierung nicht möglich: ","Cannot sort: ")+e.getMessage());}
    }
    private static boolean matches(Minecraft mc,Session s,InventorySortPlan.State state){
        var p=mc.player;var menu=p.inventoryMenu;
        if(menu.slots.size()<36)return false;
        for(int i=0;i<27;i++){
            var slot=menu.slots.get(i+9);
            if(slot.container!=p.getInventory()||!s.model.matches(slot.getItem(),state.slots().get(i))||!s.model.matches(p.getInventory().getItem(i+9),state.slots().get(i)))return false;
        }
        return s.model.matches(menu.getCarried(),state.cursor());
    }
    static void tick(Minecraft mc){
        var s=session;if(s==null)return;var p=mc.player;var cfg=BenchConfig.INSTANCE;
        if(p==null||p!=s.player||mc.level!=s.world||mc.gameMode==null||!p.isAlive()||p.hasInfiniteMaterials()||p.containerMenu!=s.menu||mc.gui.screen()!=s.screen||!cfg.inventoryMasterEnabled||!cfg.inventorySorterEnabled||!Objects.equals(s.profile,ServerProfiles.activeFileLabel())||s.locked!=cfg.inventorySortLocked||s.merge!=cfg.inventorySortMerge||!Objects.equals(s.order,cfg.inventorySortOrder)){cancel();return;}
        try{
            if(!matches(mc,s,s.expected)){cancel();return;}
            if(s.wait>0){s.wait--;return;}
            if(s.next>=s.model.plan().batches().size()){session=null;notice(tr("Sortierung lokal abgeschlossen.","Sorting locally completed."));return;}
            var batch=s.model.plan().batches().get(s.next);
            // Complete a normal pickup/placement exchange within this client tick.
            // Do not continue if the local prediction disagrees. No recovery clicks.
            for(var click:batch.clicks()){
                mc.gameMode.handleContainerInput(p.inventoryMenu.containerId,click.slot()+9,0,ContainerInput.PICKUP,p);
                if(!matches(mc,s,click.expected())){cancel();return;}
                s.expected=click.expected();
            }
            s.next++;s.wait=10; // fixed conservative pause; not a server acknowledgement
        }catch(RuntimeException e){cancel();}
    }
    private InventorySortAdapter(){}
}
