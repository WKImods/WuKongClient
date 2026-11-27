package dev.wukong.client;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.gui.screens.Screen;

/** Stable module IDs are used for favorites; switches always address the live profile. */
final class ModuleCatalog {
    private static final BenchConfig c=BenchConfig.INSTANCE;
    record Group(String id,String de,String en,BooleanSupplier on,Consumer<Boolean> set,Supplier<Screen> advanced){String name(){return tr(de,en);}}
    record Module(String id,String group,String de,String en,String descriptionDe,String descriptionEn,BooleanSupplier on,Consumer<Boolean> set){
        String name(){return tr(de,en);} String description(){return tr(descriptionDe,descriptionEn);}
    }
    static String tr(String de,String en){return WuKongLanguage.text(de,en);}
    static final List<Group> GROUPS=List.of(
        new Group("esp","ESP & Wegpunkte","ESP & Waypoints",()->c.espEnabled,v->c.espEnabled=v,EspScreen::new),
        new Group("combat","Kampf","Combat",()->c.aimAssist,v->c.aimAssist=v,CombatScreen::new),
        new Group("visual","Darstellung","Visuals",()->c.visualMasterEnabled,v->c.visualMasterEnabled=v,null),
        new Group("survival","Survival","Survival",()->c.survivalMasterEnabled,v->c.survivalMasterEnabled=v,null),
        new Group("world_mining","Bauen","Building",()->c.worldMiningMaster,v->c.worldMiningMaster=v,null),
        new Group("inventory","Inventar","Smart Inventory",()->c.inventoryMasterEnabled,v->c.inventoryMasterEnabled=v,null),
        new Group("tooltips","Tooltips","Intelligent Tooltips",()->c.tooltipsMasterEnabled,v->c.tooltipsMasterEnabled=v,null),
        new Group("hud","HUD Studio","HUD Studio",()->c.hudMasterEnabled,v->c.hudMasterEnabled=v,HudStudioScreen::new));
    static final List<Module> MODULES=List.of(
        m("players","esp","Spieler","Players","Spieleranzeige.","Player overlay.",()->c.players,v->c.players=v),
        m("mobs","esp","Mobs","Mobs","Mob-Anzeige.","Mob overlay.",()->c.mobEspEnabled,v->c.mobEspEnabled=v),
        m("ore","esp","Erze","Ores","Erzfinder.","Ore finder.",()->c.oreFinderEnabled,v->c.oreFinderEnabled=v),
        m("storage","esp","Lager","Storage","Lageranzeige.","Storage overlay.",()->c.containers,v->c.containers=v),
        m("items","esp","Items","Dropped Items","Item-Anzeige.","Dropped item overlay.",()->c.itemEspEnabled,v->c.itemEspEnabled=v),
        m("spawner","esp","Spawner","Spawners","Spawner-Anzeige.","Spawner overlay.",()->c.spawnerEspEnabled,v->c.spawnerEspEnabled=v),
        m("nametags","esp","Namensschilder","Nametags","Erweiterte Namensschilder.","Enhanced nametags.",()->c.enhancedNametags,v->c.enhancedNametags=v),
        m("waypoints","esp","Wegpunkte","Waypoints","Gespeicherte Positionen verwalten.","Manage saved locations.",()->c.waypointsEnabled,v->c.waypointsEnabled=v),
        m("death","esp","Todesmarker","Death Markers","Todesposition speichern.","Record death locations.",()->c.deathMarkerEnabled,v->c.deathMarkerEnabled=v),
        m("bow_prediction","combat","Bogen / Vorhersage","Bow / Prediction","Vorhandene Bogensicht-Einstellungen.","Existing bow settings.",()->c.aimPrediction,v->c.aimPrediction=v),
        m("bow_view","combat","Bogen / Kamera","Bow / Camera","Vorhandene separate Kamera.","Existing separate bow camera.",()->c.separateBowCamera,v->c.separateBowCamera=v),
        m("fullbright","visual","Fullbright","Fullbright","Lokale Helligkeitsdarstellung.","Local brightness rendering.",()->c.fullbrightEnabled,v->c.fullbrightEnabled=v),
        m("zoom","visual","Dynamischer Zoom","Dynamic Zoom","Taste halten, Mausrad zum Zoomen.","Hold the key; scroll to zoom.",()->c.zoomEnabled,v->c.zoomEnabled=v),
        m("autotool","survival","AutoTool","AutoTool","Werkzeugauswahl beim Abbauen.","Tool selection while mining.",()->c.autoToolEnabled,v->c.autoToolEnabled=v),
        m("toolsaver","survival","Tool Saver","Tool Saver","Werkzeugbruch vermeiden.","Protect tool durability.",()->c.toolSaverEnabled,v->c.toolSaverEnabled=v),
        m("refill","survival","Auto Refill","Auto Refill","Verbrauchte Items nachf\u00FCllen.","Refill consumed items.",()->c.autoRefillEnabled,v->c.autoRefillEnabled=v),
        m("eat","survival","Auto Eat","Auto Eat","Bei niedrigem Hunger essen.","Eat at low hunger.",()->c.autoEatEnabled,v->c.autoEatEnabled=v),
        m("area_nuker","world_mining","Area Nuker","Area Nuker","Automatischer Abbau im Umkreis.","Automatic excavation with selectable engine.",()->c.areaNuker,v->c.areaNuker=v),
        m("precision_nuker","world_mining","Precision Nuker","Precision Nuker","Mehrblockabbau mit Werkzeug.","Multi-block mining with selectable engine.",()->c.precisionNuker,v->c.precisionNuker=v),
        m("shulker","inventory","Shulker-Vorschau","Shulker Preview","Inhalt im Tooltip.","Contents in item tooltip.",()->c.shulkerPreviewEnabled,v->c.shulkerPreviewEnabled=v),
        m("bundle","inventory","B\u00FCndel-Vorschau","Bundle Preview","Inhalt im Tooltip.","Contents in item tooltip.",()->c.bundlePreviewEnabled,v->c.bundlePreviewEnabled=v),
        m("sort","inventory","Inventarsortierung","Inventory Sorter","Single- und Multiplayer; Hotbar geschützt.","Singleplayer and multiplayer; preserves hotbar.",()->c.inventorySorterEnabled,v->c.inventorySorterEnabled=v),
        m("fish","inventory","Auto-Angeln","Auto Fish","Nur Einzelspieler; erster Wurf manuell.","Singleplayer only; cast first manually.",()->c.autoFishEnabled,v->c.autoFishEnabled=v),
        m("enchant","tooltips","Verzauberungsinfo","Enchantment Info","Wirkung und Stufe erkl\u00E4ren.","Explain effects and levels.",()->c.enchantmentInfoEnabled,v->c.enchantmentInfoEnabled=v),
        m("trade","tooltips","Handelsrechner","Trade Calculator","SHIFT \u00FCber einem Handelsangebot.","SHIFT over a trade offer.",()->c.tradeCalculatorEnabled,v->c.tradeCalculatorEnabled=v),
        m("armor","hud","R\u00FCstung","Armor HUD","Haltbarkeit anzeigen.","Show equipment durability.",()->c.armorHudEnabled,v->c.armorHudEnabled=v),
        m("potion","hud","Statuseffekte","Potion HUD","Effekte und Restlaufzeit.","Effects and remaining duration.",()->c.potionHudEnabled,v->c.potionHudEnabled=v),
        m("performance","hud","FPS & Ping","FPS & Ping","Bildrate und Latenz.","Frame rate and latency.",()->c.performanceHudEnabled,v->c.performanceHudEnabled=v),
        m("resource","hud","Ressourcen","Resource Tracker","Sammelziel verwalten.","Track a collection goal.",()->c.resourceHudEnabled,v->c.resourceHudEnabled=v),
        m("legacy_hud","hud","Inventar-HUD","Inventory HUD","Bisherige kompakte Anzeige.","Legacy compact display.",()->c.inventoryHudEnabled,v->c.inventoryHudEnabled=v));
    private static Module m(String id,String group,String de,String en,String dd,String ee,BooleanSupplier on,Consumer<Boolean> set){return new Module(id,group,de,en,dd,ee,on,set);}
    static Group group(String id){return GROUPS.stream().filter(g->g.id().equals(id)).findFirst().orElseThrow();}
    static String state(Module m){return !m.on().getAsBoolean()?UiLabels.t("OFF"):group(m.group()).on().getAsBoolean()?UiLabels.t("ON"):tr("PAUSE","PAUSED");}
    static void toggle(Module m){m.set().accept(!m.on().getAsBoolean());c.save();EvolutionUi.note(m.name()+": "+state(m));}
    static void toggle(Group g){g.set().accept(!g.on().getAsBoolean());c.save();EvolutionUi.note(g.name()+UiLabels.t(" master: ")+(g.on().getAsBoolean()?UiLabels.t("ON"):UiLabels.t("OFF")));}
    private ModuleCatalog(){}
}
