package dev.wukong.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Fabric 26.3 hooks. Hotkeys are vanilla KeyMappings: fully rebindable. */
public final class ClientHooks {
    private static int ticks;
    private static final AimActivation ACTIVATION = new AimActivation();
    private static List<EntityScanner.Target> targets = List.of();

    public static void install() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientHooks::onTick);
        SurvivalAutomation.install();
        WorldMining.install();
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(WuKong.ID, "targets"),
                ClientHooks::drawHud);
    }

    private static void onTick(Minecraft mc) {
        boolean hasWorld = mc.player != null && mc.level != null;
        boolean gameplayAllowed = hasWorld && mc.gui.screen() == null;

        // Consume toggle clicks even when a screen is active, so a queued click
        // cannot unexpectedly activate a module after closing the menu.
        while (WuKongKeys.ESP.consumeClick()) {
            if (gameplayAllowed && !WuKongKeys.ESP.isUnbound()) {
                BenchConfig.INSTANCE.espEnabled = !BenchConfig.INSTANCE.espEnabled;
                BenchConfig.INSTANCE.save();
            }
        }
        while (WuKongKeys.ORE.consumeClick()) {
            if (gameplayAllowed && !WuKongKeys.ORE.isUnbound()) {
                BenchConfig.INSTANCE.oreFinderEnabled = !BenchConfig.INSTANCE.oreFinderEnabled;
                BenchConfig.INSTANCE.save();
            }
        }
        // A hotkey toggles only its own module, never the entire ESP.
        toggle(WuKongKeys.THROUGH_WALLS, gameplayAllowed, () -> cfg().espThroughWalls = !cfg().espThroughWalls);
        toggle(WuKongKeys.BOXES, gameplayAllowed, () -> cfg().espBoxes = !cfg().espBoxes);
        toggle(WuKongKeys.TRACERS, gameplayAllowed, () -> cfg().espTracers = !cfg().espTracers);
        toggle(WuKongKeys.LABELS, gameplayAllowed, () -> cfg().espNames = !cfg().espNames);
        toggle(WuKongKeys.ORE_DOTS, gameplayAllowed, () -> cfg().oreDotsEnabled = !cfg().oreDotsEnabled);
        toggle(WuKongKeys.PLAYERS, gameplayAllowed, () -> cfg().players = !cfg().players);
        toggle(WuKongKeys.MOBS, gameplayAllowed, () -> cfg().mobEspEnabled = !cfg().mobEspEnabled);
        toggle(WuKongKeys.MOB_BOXES, gameplayAllowed, () -> cfg().mobEspBoxes = !cfg().mobEspBoxes);
        toggle(WuKongKeys.MOB_NAMES, gameplayAllowed, () -> cfg().mobEspNames = !cfg().mobEspNames);
        toggle(WuKongKeys.ORE_BOXES, gameplayAllowed, () -> cfg().oreEspBoxes = !cfg().oreEspBoxes);
        toggle(WuKongKeys.ORE_NAMES, gameplayAllowed, () -> cfg().oreEspNames = !cfg().oreEspNames);
        toggle(WuKongKeys.HOSTILE, gameplayAllowed, () -> cfg().hostile = !cfg().hostile);
        toggle(WuKongKeys.PASSIVE, gameplayAllowed, () -> cfg().passive = !cfg().passive);
        toggle(WuKongKeys.STORAGE, gameplayAllowed, () -> cfg().containers = !cfg().containers);
        toggle(WuKongKeys.ITEMS, gameplayAllowed, () -> cfg().itemEspEnabled = !cfg().itemEspEnabled);
        toggle(WuKongKeys.SPAWNERS, gameplayAllowed, () -> cfg().spawnerEspEnabled = !cfg().spawnerEspEnabled);
        toggle(WuKongKeys.NAMETAGS, gameplayAllowed, () -> cfg().enhancedNametags = !cfg().enhancedNametags);
        toggle(WuKongKeys.FULLBRIGHT, gameplayAllowed, () -> cfg().fullbrightEnabled = !cfg().fullbrightEnabled);
        toggle(WuKongKeys.WAYPOINTS, gameplayAllowed, () -> cfg().waypointsEnabled = !cfg().waypointsEnabled);
        toggle(WuKongKeys.DEATH_MARKER, gameplayAllowed, () -> cfg().deathMarkerEnabled = !cfg().deathMarkerEnabled);
        toggle(WuKongKeys.INVENTORY, gameplayAllowed, () -> cfg().inventoryHudEnabled = !cfg().inventoryHudEnabled);
        toggle(WuKongKeys.AUTO_TOOL, gameplayAllowed, () -> cfg().autoToolEnabled = !cfg().autoToolEnabled);
        toggle(WuKongKeys.TOOL_SAVER, gameplayAllowed, () -> cfg().toolSaverEnabled = !cfg().toolSaverEnabled);
        toggle(WuKongKeys.AUTO_REFILL, gameplayAllowed, () -> cfg().autoRefillEnabled = !cfg().autoRefillEnabled);
        toggle(WuKongKeys.AUTO_EAT, gameplayAllowed, () -> cfg().autoEatEnabled = !cfg().autoEatEnabled);
        // Additional module and category switches: preserve master/child independence.
        toggle(WuKongKeys.AREA_NUKER, gameplayAllowed, () -> {
            cfg().areaNuker = !cfg().areaNuker;
            EvolutionUi.note(ModuleCatalog.tr("Area Nuker","Area Nuker") + ": " + (ModuleCatalog.state(ModuleCatalog.MODULES.stream().filter(m -> m.id().equals("area_nuker")).findFirst().orElseThrow())));
        });
        toggle(WuKongKeys.PRECISION_NUKER, gameplayAllowed, () -> {
            cfg().precisionNuker = !cfg().precisionNuker;
            EvolutionUi.note(ModuleCatalog.tr("Precision Nuker","Precision Nuker") + ": " + (ModuleCatalog.state(ModuleCatalog.MODULES.stream().filter(m -> m.id().equals("precision_nuker")).findFirst().orElseThrow())));
        });
        toggle(WuKongKeys.WORLD_MINING_MASTER, gameplayAllowed, () -> {
            cfg().worldMiningMaster = !cfg().worldMiningMaster;
            EvolutionUi.note(ModuleCatalog.tr("Bauen (Master)","Building (master)") + ": " + (cfg().worldMiningMaster ? "ON" : "OFF"));
        });
        toggle(WuKongKeys.AREA_FLOOR, gameplayAllowed, () -> {
            cfg().areaProtectFloor = !cfg().areaProtectFloor;
            EvolutionUi.note(ModuleCatalog.tr("Area Nuker: Bodenschutz","Area Nuker: floor protection") + ": " + (cfg().areaProtectFloor ? "ON" : "OFF"));
        });
        toggle(WuKongKeys.PRECISION_PREVIEW, gameplayAllowed, () -> {
            cfg().precisionPreview = !cfg().precisionPreview;
            EvolutionUi.note(ModuleCatalog.tr("Precision Nuker: Vorschau","Precision Nuker: preview") + ": " + (cfg().precisionPreview ? "ON" : "OFF"));
        });
        toggle(WuKongKeys.VISUAL_MASTER, gameplayAllowed, () -> {
            cfg().visualMasterEnabled = !cfg().visualMasterEnabled;
            EvolutionUi.note(ModuleCatalog.tr("Darstellung (Master)","Visuals (master)") + ": " + (cfg().visualMasterEnabled ? "ON" : "OFF"));
        });
        toggle(WuKongKeys.SURVIVAL_MASTER, gameplayAllowed, () -> {
            cfg().survivalMasterEnabled = !cfg().survivalMasterEnabled;
            EvolutionUi.note(ModuleCatalog.tr("Survival (Master)","Survival (master)") + ": " + (cfg().survivalMasterEnabled ? "ON" : "OFF"));
        });
        toggle(WuKongKeys.INVENTORY_MASTER, gameplayAllowed, () -> {
            cfg().inventoryMasterEnabled = !cfg().inventoryMasterEnabled;
            EvolutionUi.note(ModuleCatalog.tr("Inventar (Master)","Inventory (master)") + ": " + (cfg().inventoryMasterEnabled ? "ON" : "OFF"));
        });
        toggle(WuKongKeys.TOOLTIPS_MASTER, gameplayAllowed, () -> {
            cfg().tooltipsMasterEnabled = !cfg().tooltipsMasterEnabled;
            EvolutionUi.note(ModuleCatalog.tr("Tooltips (Master)","Tooltips (master)") + ": " + (cfg().tooltipsMasterEnabled ? "ON" : "OFF"));
        });
        toggle(WuKongKeys.HUD_MASTER, gameplayAllowed, () -> {
            cfg().hudMasterEnabled = !cfg().hudMasterEnabled;
            EvolutionUi.note(ModuleCatalog.tr("HUD Studio (Master)","HUD Studio (master)") + ": " + (cfg().hudMasterEnabled ? "ON" : "OFF"));
        });
        toggle(WuKongKeys.COMBAT_MASTER, gameplayAllowed, () -> {
            cfg().aimAssist = !cfg().aimAssist;
            EvolutionUi.note(ModuleCatalog.tr("Kampf / Zielhilfe (Master)","Combat / Aim Assist (master)") + ": " + (cfg().aimAssist ? "ON" : "OFF"));
        });
        toggle(WuKongKeys.BOW_PREDICTION, gameplayAllowed, () -> {
            cfg().aimPrediction = !cfg().aimPrediction;
            EvolutionUi.note(ModuleCatalog.tr("Bogen: Vorhersage","Bow prediction") + ": " + (ModuleCatalog.state(ModuleCatalog.MODULES.stream().filter(m -> m.id().equals("bow_prediction")).findFirst().orElseThrow())));
        });
        toggle(WuKongKeys.BOW_VIEW, gameplayAllowed, () -> {
            cfg().separateBowCamera = !cfg().separateBowCamera;
            EvolutionUi.note(ModuleCatalog.tr("Bogen: separate Kamera","Bow: separate camera") + ": " + (ModuleCatalog.state(ModuleCatalog.MODULES.stream().filter(m -> m.id().equals("bow_view")).findFirst().orElseThrow())));
        });
        toggle(WuKongKeys.STEADY_BOW, gameplayAllowed, () -> {
            cfg().steadyBowView = !cfg().steadyBowView;
            EvolutionUi.note(ModuleCatalog.tr("Bogen: ruhige Ansicht","Bow: steady view") + ": " + (cfg().steadyBowView ? "ON" : "OFF"));
        });
        toggle(WuKongKeys.BOW_RELEASE, gameplayAllowed, () -> {
            cfg().aimTriggerBow = !cfg().aimTriggerBow;
            EvolutionUi.note(ModuleCatalog.tr("Bogen: automatische Freigabe","Bow: automatic release") + ": " + (cfg().aimTriggerBow ? "ON" : "OFF"));
        });
        toggle(WuKongKeys.ZOOM_TOGGLE, gameplayAllowed, () -> {
            cfg().zoomEnabled = !cfg().zoomEnabled;
            EvolutionUi.note(ModuleCatalog.tr("Dynamischer Zoom (Modul ein/aus)","Dynamic Zoom (module toggle)") + ": " + (ModuleCatalog.state(ModuleCatalog.MODULES.stream().filter(m -> m.id().equals("zoom")).findFirst().orElseThrow())));
        });
        toggle(WuKongKeys.SHULKER_PREVIEW, gameplayAllowed, () -> {
            cfg().shulkerPreviewEnabled = !cfg().shulkerPreviewEnabled;
            EvolutionUi.note(ModuleCatalog.tr("Shulker-Vorschau","Shulker Preview") + ": " + (ModuleCatalog.state(ModuleCatalog.MODULES.stream().filter(m -> m.id().equals("shulker")).findFirst().orElseThrow())));
        });
        toggle(WuKongKeys.BUNDLE_PREVIEW, gameplayAllowed, () -> {
            cfg().bundlePreviewEnabled = !cfg().bundlePreviewEnabled;
            EvolutionUi.note(ModuleCatalog.tr("B\u00fcndel-Vorschau","Bundle Preview") + ": " + (ModuleCatalog.state(ModuleCatalog.MODULES.stream().filter(m -> m.id().equals("bundle")).findFirst().orElseThrow())));
        });
        toggle(WuKongKeys.SORT_TOGGLE, gameplayAllowed, () -> {
            cfg().inventorySorterEnabled = !cfg().inventorySorterEnabled;
            EvolutionUi.note(ModuleCatalog.tr("Inventarsortierung (Modul ein/aus)","Inventory Sorter (module toggle)") + ": " + (ModuleCatalog.state(ModuleCatalog.MODULES.stream().filter(m -> m.id().equals("sort")).findFirst().orElseThrow())));
        });
        toggle(WuKongKeys.ENCHANTMENT_INFO, gameplayAllowed, () -> {
            cfg().enchantmentInfoEnabled = !cfg().enchantmentInfoEnabled;
            EvolutionUi.note(ModuleCatalog.tr("Verzauberungsinfo","Enchantment Info") + ": " + (ModuleCatalog.state(ModuleCatalog.MODULES.stream().filter(m -> m.id().equals("enchant")).findFirst().orElseThrow())));
        });
        toggle(WuKongKeys.TRADE_CALCULATOR, gameplayAllowed, () -> {
            cfg().tradeCalculatorEnabled = !cfg().tradeCalculatorEnabled;
            EvolutionUi.note(ModuleCatalog.tr("Handelsrechner","Trade Calculator") + ": " + (ModuleCatalog.state(ModuleCatalog.MODULES.stream().filter(m -> m.id().equals("trade")).findFirst().orElseThrow())));
        });
        toggle(WuKongKeys.ARMOR_HUD, gameplayAllowed, () -> {
            cfg().armorHudEnabled = !cfg().armorHudEnabled;
            EvolutionUi.note(ModuleCatalog.tr("R\u00fcstungs-HUD","Armor HUD") + ": " + (ModuleCatalog.state(ModuleCatalog.MODULES.stream().filter(m -> m.id().equals("armor")).findFirst().orElseThrow())));
        });
        toggle(WuKongKeys.POTION_HUD, gameplayAllowed, () -> {
            cfg().potionHudEnabled = !cfg().potionHudEnabled;
            EvolutionUi.note(ModuleCatalog.tr("Statuseffekt-HUD","Potion HUD") + ": " + (ModuleCatalog.state(ModuleCatalog.MODULES.stream().filter(m -> m.id().equals("potion")).findFirst().orElseThrow())));
        });
        toggle(WuKongKeys.PERFORMANCE_HUD, gameplayAllowed, () -> {
            cfg().performanceHudEnabled = !cfg().performanceHudEnabled;
            EvolutionUi.note(ModuleCatalog.tr("FPS & Ping","FPS & Ping") + ": " + (ModuleCatalog.state(ModuleCatalog.MODULES.stream().filter(m -> m.id().equals("performance")).findFirst().orElseThrow())));
        });
        toggle(WuKongKeys.RESOURCE_HUD, gameplayAllowed, () -> {
            cfg().resourceHudEnabled = !cfg().resourceHudEnabled;
            EvolutionUi.note(ModuleCatalog.tr("Ressourcen-HUD","Resource Tracker") + ": " + (ModuleCatalog.state(ModuleCatalog.MODULES.stream().filter(m -> m.id().equals("resource")).findFirst().orElseThrow())));
        });
        toggle(WuKongKeys.DISTANCE_LABELS, gameplayAllowed, () -> {
            cfg().distanceLabels = !cfg().distanceLabels;
            EvolutionUi.note(ModuleCatalog.tr("Entfernungsanzeige","Distance labels") + ": " + (cfg().distanceLabels ? "ON" : "OFF"));
        });
        toggle(WuKongKeys.STORAGE_PLUS, gameplayAllowed, () -> {
            cfg().storageEspPlus = !cfg().storageEspPlus;
            EvolutionUi.note(ModuleCatalog.tr("Lager: ESP+ Farben","Storage ESP+ colors") + ": " + (cfg().storageEspPlus ? "ON" : "OFF"));
        });
        toggle(WuKongKeys.EAT_BACKGROUND, gameplayAllowed, () -> {
            cfg().autoEatBackground = !cfg().autoEatBackground;
            EvolutionUi.note(ModuleCatalog.tr("Auto Eat: Hintergrundmodus","Auto Eat: background mode") + ": " + (cfg().autoEatBackground ? "ON" : "OFF"));
        });
        boolean openWaypointEditor = false;
        while (WuKongKeys.ADD_WAYPOINT.consumeClick()) {
            if (gameplayAllowed && !WuKongKeys.ADD_WAYPOINT.isUnbound()) openWaypointEditor = true;
        }
        if (openWaypointEditor) {
            mc.gui.setScreen(new WaypointEditScreen(null, true));
            gameplayAllowed = false;
        }
        for (var entry: WuKongKeys.ORE_TYPES.entrySet()) {
            OreKind kind=entry.getKey();
            toggle(entry.getValue(),gameplayAllowed,()->cfg().setOreVisible(kind,!cfg().oreVisible(kind)));
        }
        boolean openMenu = false;
        while (WuKongKeys.MENU.consumeClick()) {
            if (gameplayAllowed && !WuKongKeys.MENU.isUnbound()) openMenu = true;
        }
        if (openMenu) {
            mc.gui.setScreen(new BenchScreen());
            gameplayAllowed = false;
        }

        // Utility shortcuts: consume even with a screen open, never queue a later action.
        while (WuKongKeys.HUD_EDITOR.consumeClick()) {
            if (gameplayAllowed && mc.gui.screen() == null && !WuKongKeys.HUD_EDITOR.isUnbound()) {
                mc.gui.setScreen(new HudStudioScreen());
            }
        }
        while (WuKongKeys.PROFILE_MANAGER.consumeClick()) {
            if (gameplayAllowed && mc.gui.screen() == null && !WuKongKeys.PROFILE_MANAGER.isUnbound()) {
                mc.gui.setScreen(new ServerProfileScreen());
            }
        }
        while (WuKongKeys.NOTIFICATIONS.consumeClick()) {
            if (gameplayAllowed && mc.gui.screen() == null && !WuKongKeys.NOTIFICATIONS.isUnbound()) {
                mc.gui.setScreen(new NotificationScreen(null));
            }
        }
        while (WuKongKeys.THEME_NEXT.consumeClick()) {
            if (gameplayAllowed && mc.gui.screen() == null && !WuKongKeys.THEME_NEXT.isUnbound()) {
                EvolutionUi.nextTheme();
            }
        }
        while (WuKongKeys.UPDATE_CHECK.consumeClick()) {
            if (gameplayAllowed && mc.gui.screen() == null && !WuKongKeys.UPDATE_CHECK.isUnbound()) {
                EvolutionUi.checkNow();
            }
        }
        gameplayAllowed = gameplayAllowed && mc.gui.screen() == null;

        if (!hasWorld) {
            SurvivalAutomation.tick(mc, false);
            WorldMining.publish(mc,false);
            ACTIVATION.reset();
            BowViewSplit.clear();
            AimAssist.tick(mc, false); // Clears prediction history for the previous world.
            targets = List.of();
            OreScanner.tick(mc);
            return;
        }

        boolean aimActive = ACTIVATION.update(
                !WuKongKeys.AIM.isUnbound() && WuKongKeys.AIM.isDown(),
                BenchConfig.INSTANCE.aimAssist,
                gameplayAllowed,
                BenchConfig.INSTANCE.aimActivationMode);
        if (gameplayAllowed) AimAssist.tick(mc, aimActive);
        else BowViewSplit.clear();
        OreScanner.tick(mc);
        SurvivalAutomation.tick(mc, gameplayAllowed);
        WorldMining.publish(mc,gameplayAllowed);
        QualityHud.tick(mc);
        if (++ticks % 10 == 0) targets = EntityScanner.scan();
    }

    private static void drawHud(GuiGraphicsExtractor g, DeltaTracker delta) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gui.screen() != null) return;
        // ESP master is independent of Ore Finder's own toggle; both must be ON.
        if (BenchConfig.INSTANCE.espEnabled) {
            EspOverlay.draw(g, targets);
            OreOverlay.draw(g);
            ExtraEspOverlay.draw(g,targets);
            WaypointOverlay.draw(g);
        }
        QualityHud.draw(g);
        WorldMining.draw(g);
    }

    private static BenchConfig cfg(){return BenchConfig.INSTANCE;}
    private static void toggle(net.minecraft.client.KeyMapping key,boolean gameplayAllowed,Runnable action) {
        // 26.3 can send SDLK_UNKNOWN for unbound keys on some layouts: never act on it.
        while (key.consumeClick()) {
            if (gameplayAllowed && !key.isUnbound()
                    && WuKongKeys.permits(key, WuKongKeys.controlPressed())) {
                action.run();
                cfg().save();
            }
        }
    }
    private ClientHooks() {}
}
