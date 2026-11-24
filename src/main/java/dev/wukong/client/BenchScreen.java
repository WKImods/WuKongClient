package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

/** Compact WuKong home dashboard. Detailed aim controls belong to CombatScreen. */
public final class BenchScreen extends Screen {
    private final BenchConfig cfg = BenchConfig.INSTANCE;
    private int x, y, w, h, leftW, rightX, rightW;
    private static final int CYAN = 0xFF5BDCE7;
    private static final int AMBER = 0xFFFFBE64;
    private static final int GREEN = 0xFF77DE9D;
    private static final int VIOLET = 0xFFC1A2FF;

    public BenchScreen() { super(Component.literal("WuKong")); }

    @Override protected void init() {
        w = Math.min(624, width - 20);
        h = 394;
        x = (width - w) / 2;
        y = Math.max(6, (height - h) / 2);
        int gap = 18;
        leftW = (w - 38 - gap) / 2;
        rightW = w - 38 - gap - leftW;
        rightX = x + 19 + leftW + gap;
        int l = x + 16;
        int top = y + 100;

        page(l, top, leftW, "ESP Dashboard", () -> new EspScreen());
        page(l, top + 29, leftW, "Entity ESP", () -> new EntityEspScreen());
        page(l, top + 58, leftW, "Ore / Block Finder", () -> new OreScreen());
        page(l, top + 87, leftW, "World ESP / Storage", () -> new WorldEspScreen());
        page(l, top + 116, leftW, "HUD / Utilities", () -> new ModulesScreen(1));
        page(l, top + 145, leftW, "Survival Automation", () -> new SurvivalScreen());
        page(l, top + 174, leftW, "Combat / Aim", CombatScreen::new);
        page(l, top + 203, leftW, "Server Profiles", ServerProfileScreen::new);

        quick(rightX, top, "ESP Master", () -> cfg.espEnabled, v -> cfg.espEnabled = v);
        quick(rightX, top + 29, "Ore Finder", () -> cfg.oreFinderEnabled, v -> cfg.oreFinderEnabled = v);
        quick(rightX, top + 58, "Fullbright", () -> cfg.fullbrightEnabled, v -> cfg.fullbrightEnabled = v);
        quick(rightX, top + 87, "Aim Assist", () -> cfg.aimAssist, v -> cfg.aimAssist = v);
        quick(rightX, top + 116, "AutoTool", () -> cfg.autoToolEnabled, v -> cfg.autoToolEnabled = v);
        quick(rightX, top + 145, "Auto Eat", () -> cfg.autoEatEnabled, v -> cfg.autoEatEnabled = v);
        page(rightX, top + 174, rightW, "Keybinds", () ->
                new KeyBindsScreen(this, Minecraft.getInstance().options));

        // Dedicated help entry centered in the footer, never over the module controls.
        addRenderableWidget(Button.builder(Component.literal(WuKongLanguage.text("?  Extras / Hilfe  >", "?  Extras / Help  >")), b -> {
            cfg.save(); Minecraft.getInstance().gui.setScreen(new ExtrasScreen());
        }).bounds(x + (w - 152) / 2, y + h - 32, 152, 22).build());
    }

    private static String tr(String label) {
        if (!WuKongLanguage.german()) return label;
        return switch (label) {
            case "ESP Dashboard" -> "ESP-Uebersicht";
            case "Entity ESP" -> "Entitaeten ESP";
            case "Ore / Block Finder" -> "Erz- / Blockfinder";
            case "World ESP / Storage" -> "Welt ESP / Lager";
            case "HUD / Utilities" -> "HUD / Werkzeuge";
            case "Survival Automation" -> "Survival-Automatik";
            case "Combat / Aim" -> "Kampf / Zielen";
            case "Server Profiles" -> "Serverprofile";
            case "Keybinds" -> "Tastenbelegung";
            case "ESP Master" -> "Gesamt-ESP";
            case "Ore Finder" -> "Erzfinder";
            case "Aim Assist" -> "Zielhilfe";
            case "Auto Eat" -> "Auto-Essen";
            default -> label;
        };
    }

    private interface Flag { boolean get(); }
    private interface Toggle { void set(boolean on); }

    private void page(int xx, int yy, int ww, String label, java.util.function.Supplier<Screen> screen) {
        addRenderableWidget(Button.builder(Component.literal(tr(label) + "  >"), b -> {
            cfg.save(); Minecraft.getInstance().gui.setScreen(screen.get());
        }).bounds(xx, yy, ww, 21).build());
    }

    private void quick(int xx, int yy, String label, Flag get, Toggle set) {
        addRenderableWidget(Button.builder(Component.literal(tr(label) + " : " + (get.get() ? "ON" : "OFF")), b -> {
            set.set(!get.get());
            cfg.save();
            b.setMessage(Component.literal(tr(label) + " : " + (get.get() ? "ON" : "OFF")));
        }).bounds(xx, yy, rightW, 21).build());
    }

    private void status(GuiGraphicsExtractor g, int xx, int chipWidth, String label, boolean on, int accent) {
        g.fill(xx, y + 39, xx + chipWidth, y + 64, on ? 0xFF203E39 : 0xFF262D35);
        g.fill(xx, y + 39, xx + 3, y + 64, accent);
        g.text(font, label + " " + (on ? "ON" : "OFF"), xx + 9, y + 47,
                on ? 0xFFEDF7F8 : 0xFFABB3BF, false);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        g.fill(x, y, x + w, y + h, 0xEF101924);
        g.fill(x, y, x + w, y + 32, 0xFF1F3342);
        g.fill(x, y + 30, x + w, y + 32, CYAN);
        g.text(font, WuKongLanguage.text("WUKONG / KONTROLLZENTRUM", "WUKONG / CONTROL CENTER"), x + 16, y + 12, 0xFFFFFFFF, false);
        g.fill(x + 10, y + 72, x + w - 10, y + 74, 0xFF314253);
        int chipW = (w - 38 - 3 * 6) / 4;
        status(g, x + 16, chipW, "ESP", cfg.espEnabled, CYAN);
        status(g, x + 22 + chipW, chipW, "ORE", cfg.oreFinderEnabled, CYAN);
        status(g, x + 28 + 2 * chipW, chipW, "AIM", cfg.aimAssist, AMBER);
        status(g, x + 34 + 3 * chipW, chipW, "LIGHT", cfg.fullbrightEnabled, GREEN);
        g.fill(x + 10, y + 82, x + w - 10, y + h - 43, 0x99141F2C);
        g.fill(x + 13, y + 91, x + 16, y + h - 55, CYAN);
        g.fill(rightX - 5, y + 91, rightX - 2, y + h - 55, GREEN);
        g.text(font, "MODULES", x + 24, y + 85, CYAN, false);
        g.text(font, WuKongLanguage.text("SCHNELLSCHALTER", "QUICK SWITCHES"), rightX + 7, y + 85, GREEN, false);
        super.extractRenderState(g, mx, my, partial);
        g.fill(x + 12, y + h - 37, x + w - 12, y + h - 36, 0xFF314253);
        g.text(font, "WUKONG", x + 16, y + h - 24, VIOLET, false);
        g.text(font, "ESC", x + w - 40, y + h - 24, 0xFFA4BDC9, false);
    }

    @Override public void onClose() { cfg.save(); super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }
}
