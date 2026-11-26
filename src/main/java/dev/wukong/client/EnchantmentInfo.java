package dev.wukong.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;

/** Read-only explanations for enchantments the vanilla tooltip is allowed to show. */
public final class EnchantmentInfo {
    private static final Map<String,String[]> EFFECTS=Map.ofEntries(
        Map.entry("aqua_affinity", new String[]{"Schnellerer Blockabbau unter Wasser.","Faster mining underwater."}),
        Map.entry("bane_of_arthropods", new String[]{"Mehr Schaden und Verlangsamung gegen Gliederf\u00FC\u00DFer.","Extra damage and slowing against arthropods."}),
        Map.entry("binding_curse", new String[]{"Angelegte Ausr\u00FCstung l\u00E4sst sich normalerweise nicht ablegen.","Equipped gear cannot normally be removed."}),
        Map.entry("blast_protection", new String[]{"Verringert Explosionsschaden und R\u00FCcksto\u00DF.","Reduces explosion damage and knockback."}),
        Map.entry("breach", new String[]{"Verringert die Wirksamkeit gegnerischer R\u00FCstung.","Reduces the effectiveness of target armor."}),
        Map.entry("channeling", new String[]{"Ruft bei geeigneten Gewittern Blitze hervor.","Summons lightning in suitable thunderstorms."}),
        Map.entry("density", new String[]{"Erh\u00F6ht den Fallschaden-Bonus des Streitkolbens.","Increases mace damage from falling."}),
        Map.entry("depth_strider", new String[]{"Erh\u00F6ht die Bewegungsgeschwindigkeit unter Wasser.","Increases underwater movement speed."}),
        Map.entry("efficiency", new String[]{"Erh\u00F6ht das Abbautempo geeigneter Werkzeuge.","Increases mining speed with suitable tools."}),
        Map.entry("feather_falling", new String[]{"Verringert Fallschaden.","Reduces fall damage."}),
        Map.entry("fire_aspect", new String[]{"Setzt getroffene Ziele in Brand.","Sets targets on fire on hit."}),
        Map.entry("fire_protection", new String[]{"Verringert Feuerschaden und Brenndauer.","Reduces fire damage and burn duration."}),
        Map.entry("flame", new String[]{"Abgeschossene Pfeile k\u00F6nnen Ziele entz\u00FCnden.","Fired arrows can ignite targets."}),
        Map.entry("fortune", new String[]{"Erh\u00F6ht bestimmte Block-Drops.","Increases certain block drops."}),
        Map.entry("frost_walker", new String[]{"Friert nahes Oberfl\u00E4chenwasser vor\u00FCbergehend ein.","Temporarily freezes nearby surface water."}),
        Map.entry("impaling", new String[]{"Erh\u00F6ht in Java den Schaden gegen Wassermobs.","Increases damage against aquatic mobs in Java."}),
        Map.entry("infinity", new String[]{"Normale Pfeile werden beim Schie\u00DFen nicht verbraucht.","Normal arrows are not consumed when shooting."}),
        Map.entry("knockback", new String[]{"Erh\u00F6ht den R\u00FCcksto\u00DF im Nahkampf.","Increases melee knockback."}),
        Map.entry("looting", new String[]{"Erh\u00F6ht bestimmte Mob-Drops und seltene Drop-Chancen.","Increases certain mob drops and rare drop chances."}),
        Map.entry("loyalty", new String[]{"Geworfene Dreizacke kehren zur\u00FCck.","Thrown tridents return to their owner."}),
        Map.entry("luck_of_the_sea", new String[]{"Verbessert die Chance auf wertvolle Angelbeute.","Improves chances of valuable fishing loot."}),
        Map.entry("lunge", new String[]{"Bewegt den Spieler bei geeigneten Speerangriffen vorw\u00E4rts.","Propels the player forward during suitable spear attacks."}),
        Map.entry("lure", new String[]{"Verk\u00FCrzt die Wartezeit auf einen Fischanbiss.","Reduces the wait for a fishing bite."}),
        Map.entry("mending", new String[]{"Repariert Ausr\u00FCstung mit gesammelter Erfahrung.","Repairs equipment using collected experience."}),
        Map.entry("multishot", new String[]{"Feuert drei Projektile mit der Armbrust ab.","Fires three projectiles from a crossbow."}),
        Map.entry("piercing", new String[]{"Armbrustpfeile k\u00F6nnen Ziele durchdringen.","Crossbow arrows can pass through targets."}),
        Map.entry("power", new String[]{"Erh\u00F6ht den Pfeilschaden des Bogens.","Increases bow arrow damage."}),
        Map.entry("projectile_protection", new String[]{"Verringert Projektilschaden.","Reduces projectile damage."}),
        Map.entry("protection", new String[]{"Verringert viele Arten erlittenen Schadens.","Reduces many types of incoming damage."}),
        Map.entry("punch", new String[]{"Erh\u00F6ht den R\u00FCcksto\u00DF von Pfeilen.","Increases arrow knockback."}),
        Map.entry("quick_charge", new String[]{"Verk\u00FCrzt die Ladezeit der Armbrust.","Reduces crossbow reload time."}),
        Map.entry("respiration", new String[]{"Verl\u00E4ngert die Atemzeit unter Wasser.","Extends underwater breathing time."}),
        Map.entry("riptide", new String[]{"Schleudert den Tr\u00E4ger mit dem Dreizack in Wasser oder Regen vorw\u00E4rts.","Propels the user with a trident in water or rain."}),
        Map.entry("sharpness", new String[]{"Erh\u00F6ht den Nahkampfschaden.","Increases melee attack damage."}),
        Map.entry("silk_touch", new String[]{"Sammelt bestimmte Bl\u00F6cke in ihrer urspr\u00FCnglichen Form.","Collects certain blocks in their original form."}),
        Map.entry("smite", new String[]{"Erh\u00F6ht den Schaden gegen Untote.","Increases damage against undead mobs."}),
        Map.entry("soul_speed", new String[]{"Erh\u00F6ht das Tempo auf Seelensand und Seelenerde.","Increases movement speed on soul sand and soil."}),
        Map.entry("sweeping_edge", new String[]{"Erh\u00F6ht den Rundumschlagschaden von Schwertern.","Increases sword sweeping damage."}),
        Map.entry("swift_sneak", new String[]{"Erh\u00F6ht das Bewegungstempo beim Schleichen.","Increases movement speed while sneaking."}),
        Map.entry("thorns", new String[]{"Kann Schaden an Angreifer zur\u00FCckgeben.","Can reflect damage to attackers."}),
        Map.entry("unbreaking", new String[]{"Verringert den durchschnittlichen Haltbarkeitsverbrauch.","Reduces average durability consumption."}),
        Map.entry("vanishing_curse", new String[]{"Gegenstand verschwindet beim Tod unter normalen Todesregeln.","Item disappears on death under normal death rules."}),
        Map.entry("wind_burst", new String[]{"Schleudert den Tr\u00E4ger nach einem erfolgreichen Streitkolben-Sturzangriff nach oben.","Launches the user upward after a successful mace smash."})
    );
    private static String tr(String de,String en){return WuKongLanguage.text(de,en);}
    public static List<Component> append(ItemStack stack,List<Component> original) {
        var cfg=BenchConfig.INSTANCE;
        if(!cfg.tooltipsMasterEnabled || !cfg.enchantmentInfoEnabled || original==null || original.isEmpty()) return original;
        var display=stack.get(DataComponents.TOOLTIP_DISPLAY);
        if(display==null) display=TooltipDisplay.DEFAULT;
        if(display.hideTooltip()) return original;
        var entries=new java.util.LinkedHashMap<net.minecraft.core.Holder<Enchantment>,Integer>();
        for(var type:List.of(DataComponents.ENCHANTMENTS,DataComponents.STORED_ENCHANTMENTS)) {
            if(!display.shows(type)) continue;
            var data=stack.get(type);
            if(data!=null) for(var key:data.keySet()) entries.merge(key,data.getLevel(key),Math::max);
        }
        if(entries.isEmpty()) return original;
        var result=new ArrayList<>(original);
        if(cfg.enchantmentInfoShiftOnly && !Minecraft.getInstance().hasShiftDown()) {
            result.add(Component.literal(tr("SHIFT: Verzauberungsdetails","SHIFT: enchantment details")).withStyle(ChatFormatting.DARK_GRAY));
            return result;
        }
        result.add(Component.literal(tr("WuKong \u2022 Verzauberungen","WuKong \u2022 Enchantments")).withStyle(ChatFormatting.AQUA));
        for(var entry:entries.entrySet()) {
            var holder=entry.getKey(); int level=entry.getValue();
            if(!holder.isBound()) continue;
            int max=holder.value().getMaxLevel();
            result.add(Enchantment.getFullname(holder,level).copy().append(Component.literal(" ["+level+"/"+max+"]")).withStyle(ChatFormatting.GRAY));
            var id=holder.unwrapKey().map(k->k.identifier()).orElse(null);
            var effect=id!=null && id.getNamespace().equals("minecraft")?EFFECTS.get(id.getPath()):null;
            if(effect!=null) result.add(Component.literal(tr(effect[0],effect[1])).withStyle(ChatFormatting.GRAY));
            else result.add(Component.literal(tr("Keine hinterlegte Wirkung.","No built-in effect description.")).withStyle(ChatFormatting.DARK_GRAY));
        }
        return result;
    }
    private EnchantmentInfo(){}
}
