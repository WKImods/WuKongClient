package dev.wukong.client;

import java.util.Comparator;
import java.util.ArrayList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import dev.wukong.client.mixin.FishingHookAccessor;

public final class SmartInventory {
    private static int recast=-1;
    private static int cooldown;
    private static Object world;
    private static int rodSlot=-1;
    private static ItemStack rod=ItemStack.EMPTY;
    private static Object owner;
    static void install(){ ClientTickEvents.END_CLIENT_TICK.register(SmartInventory::tick); }
    private static boolean local(Minecraft mc){return mc.player!=null && mc.level!=null && mc.gameMode!=null && mc.hasSingleplayerServer() && mc.player.isAlive();}
    private static String tr(String de,String en){return WuKongLanguage.text(de,en);}
    private static void reset(){recast=-1;rodSlot=-1;rod=ItemStack.EMPTY;}
    private static void tick(Minecraft mc){
        var cfg=BenchConfig.INSTANCE;
        while(WuKongKeys.SORT_INVENTORY.consumeClick()) if(!WuKongKeys.SORT_INVENTORY.isUnbound() && (mc.gui.screen()==null || mc.gui.screen() instanceof InventoryScreen)) sort(mc);
        while(WuKongKeys.AUTO_FISH.consumeClick()) if(!WuKongKeys.AUTO_FISH.isUnbound() && local(mc) && mc.gui.screen()==null){cfg.autoFishEnabled=!cfg.autoFishEnabled;cfg.save();reset();}
        if(world!=mc.level || owner!=mc.player){world=mc.level;owner=mc.player;reset();cooldown=0;}
        if(cooldown>0)cooldown--;
        if(!local(mc) || !cfg.inventoryMasterEnabled || !cfg.autoFishEnabled || mc.gui.screen()!=null || mc.isPaused() || mc.options.keyAttack.isDown() || mc.options.keyUse.isDown() || mc.player.isUsingItem()){reset();return;}
        var inv=mc.player.getInventory(); var held=mc.player.getMainHandItem();
        if(held.getItem()!=Items.FISHING_ROD || held.getMaxDamage()-held.getDamageValue()<=Math.max(1,cfg.toolSaverMinDurability) || inv.getFreeSlot()<0){reset();return;}
        if(recast>=0){
            if(inv.getSelectedSlot()!=rodSlot || held.getItem()!=rod.getItem()){reset();return;}
            if(mc.player.fishing!=null){if(--recast<=0)reset();return;}
            if(--recast<=0){mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);reset();cooldown=20;}
            return;
        }
        var hook=mc.player.fishing;
        // Start with a manually cast rod. No automatic initial cast.
        if(cooldown==0 && hook!=null && hook.getHookedIn()==null && ((FishingHookAccessor)hook).wukong$isBiting()){
            rodSlot=inv.getSelectedSlot();rod=held.copy();mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);recast=12;cooldown=20;
        }
    }
    private static final Comparator<ItemStack> ORDER=Comparator.comparing(ItemStack::isEmpty)
        .thenComparing(s->s.isEmpty()?"":BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
        .thenComparing(s->s.isEmpty()?"":s.getHoverName().getString())
        .thenComparingInt(s->-s.getCount());
    public static void sort(Minecraft mc){
        if(!local(mc) || !BenchConfig.INSTANCE.inventoryMasterEnabled || !BenchConfig.INSTANCE.inventorySorterEnabled)return;
        var p=mc.player;
        var server=mc.getSingleplayerServer();
        if(server==null || p.hasInfiniteMaterials() || p.containerMenu!=p.inventoryMenu || !p.inventoryMenu.getCarried().isEmpty())return;
        final var playerId=p.getUUID();
        final var success=Component.literal(tr("Inventar sortiert.","Inventory sorted."));
        final var rejected=Component.literal(tr("Sortierung gestoppt: Inventar nicht bereit.","Sorting stopped: inventory not ready."));
        // The integrated server owns the inventory. Never mutate the client copy.
        server.execute(()->{
            var player=server.getPlayerList().getPlayer(playerId);
            if(player==null || !player.isAlive())return;
            var menu=player.inventoryMenu;
            if(player.hasInfiniteMaterials() || player.containerMenu!=menu || !menu.getCarried().isEmpty()){
                player.sendSystemMessage(rejected);return;
            }
            var inventory=player.getInventory();
            var ordered=new ArrayList<ItemStack>(27);
            for(int i=9;i<36;i++)ordered.add(inventory.getItem(i));
            ordered.sort(ORDER);
            // Preserve each stack and its components, including empty slots.
            // No buffer, merging, dropping or changes outside the main inventory.
            for(int i=9;i<36;i++)inventory.setItem(i,ordered.get(i-9));
            inventory.setChanged();
            menu.broadcastFullState();
            player.sendSystemMessage(success);
        });
    }
    private SmartInventory(){}
}
