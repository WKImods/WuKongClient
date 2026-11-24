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
            if (gameplayAllowed) {
                BenchConfig.INSTANCE.espEnabled = !BenchConfig.INSTANCE.espEnabled;
                BenchConfig.INSTANCE.save();
            }
        }
        while (WuKongKeys.ORE.consumeClick()) {
            if (gameplayAllowed) {
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
            if (gameplayAllowed) openMenu = true;
        }
        if (openMenu) {
            mc.gui.setScreen(new BenchScreen());
            gameplayAllowed = false;
        }

        if (!hasWorld) {
            SurvivalAutomation.tick(mc, false);
            ACTIVATION.reset();
            BowViewSplit.clear();
            AimAssist.tick(mc, false); // Clears prediction history for the previous world.
            targets = List.of();
            OreScanner.tick(mc);
            return;
        }

        boolean aimActive = ACTIVATION.update(
                WuKongKeys.AIM.isDown(),
                BenchConfig.INSTANCE.aimAssist,
                gameplayAllowed,
                BenchConfig.INSTANCE.aimActivationMode);
        if (gameplayAllowed) AimAssist.tick(mc, aimActive);
        else BowViewSplit.clear();
        OreScanner.tick(mc);
        SurvivalAutomation.tick(mc, gameplayAllowed);
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
