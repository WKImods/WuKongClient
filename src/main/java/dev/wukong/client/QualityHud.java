package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;

/** Compact HUD without custom inventory hooks or client-side network messages. */
public final class QualityHud {
    private static Object level;
    private static boolean dead;

    public static void tick(Minecraft mc) {
        if (mc.level != level) { level=mc.level; dead=false; }
        if (mc.player==null || mc.level==null) return;
        boolean nowDead=mc.player.isDeadOrDying();
        if (nowDead && !dead && BenchConfig.INSTANCE.espEnabled && BenchConfig.INSTANCE.deathMarkerEnabled) WaypointManager.recordDeath(mc);
        dead=nowDead;
    }

    /** Kept for callers of older versions; the new hotkey opens WaypointEditScreen. */
    public static void addCurrentPosition(Minecraft mc) {
        if (mc.player!=null && mc.level!=null) {
            WaypointManager.add(WaypointManager.newAtPlayer(mc));
            BenchConfig.INSTANCE.waypointsEnabled=true;
            BenchConfig.INSTANCE.save();
        }
    }

    public static void draw(GuiGraphicsExtractor g) {
        Minecraft mc=Minecraft.getInstance();
        BenchConfig cfg=BenchConfig.INSTANCE;
        if (mc.player==null || mc.level==null) return;
        BowDiagnostics.draw(g,mc);
        if (!cfg.hudMasterEnabled) return;
        HudStudio.draw(g,mc,false);
        if (cfg.inventoryHudEnabled) {
            int bx=8,by=g.guiHeight()-54;
            ItemStack hand=mc.player.getMainHandItem();
            if (!hand.isEmpty()) {
                String text=hand.getHoverName().getString()+" x"+hand.getCount();
                if (hand.isDamageableItem()) text+=" ("+(hand.getMaxDamage()-hand.getDamageValue())+"/"+hand.getMaxDamage()+")";
                g.text(mc.font,text,bx,by,0xFFE9E9E9,true); by+=12;
            }
            for (EquipmentSlot slot : new EquipmentSlot[] {
                    EquipmentSlot.HEAD,
                    EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS,
                    EquipmentSlot.FEET
            }) {
                ItemStack armor = mc.player.getItemBySlot(slot);
                if (armor.isEmpty() || !armor.isDamageableItem()) continue;
                String text=armor.getHoverName().getString()+": "+(armor.getMaxDamage()-armor.getDamageValue());
                g.text(mc.font,text,bx,by,0xFFC7E5FF,true); by+=12;
            }
        }
    }
    private QualityHud(){}
}
