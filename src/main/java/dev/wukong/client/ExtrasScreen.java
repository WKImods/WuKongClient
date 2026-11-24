package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

/** In-game manual, hotkey reference and independent German/English UI language. */
public final class ExtrasScreen extends Screen {
    private static final int CYAN = 0xFF5BDCE7;
    private static final int WHITE = 0xFFE1EBF3;
    private static final int MUTED = 0xFFA3B9C7;
    private int x, y, w, h, page;
    private Button languageButton;

    public ExtrasScreen() { super(Component.literal("WuKong - Extras")); }

    @Override protected void init() {
        w = Math.min(548, width - 20);
        h = Math.min(368, height - 16);
        x = (width - w) / 2;
        y = (height - h) / 2;
        int nav = y + h - 56;
        int middleWidth = Math.max(60, (w - 124) / 2);
        addRenderableWidget(Button.builder(Component.literal("<"), b -> page = (page + 7) % 8)
                .bounds(x + 15, nav, 34, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> page = (page + 1) % 8)
                .bounds(x + w - 49, nav, 34, 20).build());
        languageButton = addRenderableWidget(Button.builder(Component.literal(languageLabel()), b -> {
            WuKongLanguage.choose(WuKongLanguage.german() ? "en" : "de");
            b.setMessage(Component.literal(languageLabel()));
        }).bounds(x + 54, nav, middleWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal(WuKongLanguage.text("Tastenbelegung", "Key bindings")), b ->
                Minecraft.getInstance().gui.setScreen(new KeyBindsScreen(this, Minecraft.getInstance().options)))
                .bounds(x + 62 + middleWidth, nav, middleWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal(WuKongLanguage.text("Zurueck", "Back")), b -> onClose())
                .bounds(x + (w - 128) / 2, y + h - 29, 128, 20).build());
    }

    private String languageLabel() {
        return WuKongLanguage.german() ? "Sprache: Deutsch" : "Language: English";
    }
    private String titleForPage() {
        String[] de = {"Einstieg", "ESP und Ore Finder", "Survival Automation", "Waypoints und Tod", "Serverprofile", "Standardtasten: F-Tasten", "Standardtasten: Strg + Numpad", "Weitere Tasten und Hinweise"};
        String[] en = {"Getting started", "ESP and Ore Finder", "Survival automation", "Waypoints and death", "Server profiles", "Default keys: function keys", "Default keys: Ctrl + Numpad", "Other keys and notes"};
        return (WuKongLanguage.german() ? de : en)[page];
    }
    private String[] lines() {
        boolean de = WuKongLanguage.german();
        return switch (page) {
            case 0 -> de ? new String[] {
                    "WuKong ist ein modularer Fabric-Client.",
                    "Ueber Insert kommst du zum Dashboard.",
                    "Jedes Modul hat eigene Einstellungen.",
                    "Die Schnelltasten findest du auf Seite 6 und 7.",
                    "Alle Tasten sind in Minecraft neu belegbar.",
                    "F1 bis F5 und F11 bleiben fuer Minecraft frei."} : new String[] {
                    "WuKong is a modular Fabric client.",
                    "Press Insert to open the dashboard.",
                    "Each module has separate settings.",
                    "Quick keys are listed on pages 6 and 7.",
                    "You can rebind all keys in Minecraft Controls.",
                    "F1-F5 and F11 stay reserved for Minecraft."};
            case 1 -> de ? new String[] {
                    "ESP Master: schaltet alle ESP-Ebenen aus.",
                    "Mob ESP und Ore ESP sind getrennte Module.",
                    "Boxen, Namen und Erzfilter sind einzeln schaltbar.",
                    "Through Walls: Anzeige auch hinter Waenden.",
                    "F9 schaltet nur den Ore Finder um.",
                    "F7 schaltet das gesamte ESP um."} : new String[] {
                    "ESP Master toggles all ESP overlays.",
                    "Mob ESP and Ore ESP work independently.",
                    "Boxes, labels and ore filters are separate.",
                    "Through Walls shows targets behind blocks.",
                    "F9 toggles only the Ore Finder.",
                    "F7 toggles the entire ESP."};
            case 2 -> de ? new String[] {
                    "AutoTool: passendes Werkzeug beim Abbau.",
                    "Tool Saver: warnt bzw. stoppt vor Werkzeugbruch.",
                    "Auto Refill: fuellt leere Hotbar-Slots nach.",
                    "Auto Eat: isst bei niedrigem Hunger automatisch.",
                    "Jedes Modul kann separat umgeschaltet werden.",
                    "Auf Servern gelten weiterhin deren Regeln."} : new String[] {
                    "AutoTool chooses mining tools automatically.",
                    "Tool Saver helps prevent breaking tools.",
                    "Auto Refill restores empty hotbar slots.",
                    "Auto Eat uses food at low hunger.",
                    "Each module can be toggled independently.",
                    "Always follow each server's rules."};
            case 3 -> de ? new String[] {
                    "F10 oeffnet den Waypoint-Editor.",
                    "Namen, Position, Icon und Sichtbarkeit waehlen.",
                    "Beim Tod entsteht ein loeschbarer Waypoint.",
                    "Waypoints gehoeren zum jeweiligen Weltprofil.",
                    "Versteckte Marker bleiben gespeichert."} : new String[] {
                    "F10 opens the waypoint editor.",
                    "Choose name, coordinates, icon and visibility.",
                    "Death creates an editable/removable waypoint.",
                    "Waypoints belong to the active world profile.",
                    "Hidden markers are still saved."};
            case 4 -> de ? new String[] {
                    "WuKong wechselt das Profil pro Server / Welt.",
                    "ESP, Survival, Filter, Waypoints: getrennt.",
                    "Beim Wechsel werden Einstellungen gespeichert.",
                    "Server Profiles zeigt das aktive Profil.",
                    "Tasten und UI-Sprache bleiben global.",
                    "Profile sind keine Anti-Cheat-Umgehung."} : new String[] {
                    "WuKong switches profiles by server / world.",
                    "ESP, survival, filters, waypoints are separate.",
                    "Changes are saved when switching worlds.",
                    "Server Profiles shows the active profile.",
                    "Keys and UI language remain global.",
                    "Profiles do not bypass server checks."};
            case 5 -> de ? new String[] {
                    "Insert  -  WuKong Hauptmenue",
                    "R       -  Aim Assist (Halten/Umschalten)",
                    "F6      -  Fullbright",
                    "F7      -  ESP Master",
                    "F8      -  Mob ESP",
                    "F9      -  Ore Finder",
                    "F10     -  Waypoint-Editor"} : new String[] {
                    "Insert  -  WuKong dashboard",
                    "R       -  Aim Assist (hold/toggle)",
                    "F6      -  Fullbright",
                    "F7      -  ESP Master",
                    "F8      -  Mob ESP",
                    "F9      -  Ore Finder",
                    "F10     -  Waypoint editor"};
            case 6 -> de ? new String[] {
                    "Strg + Num1  -  Spieler ESP",
                    "Strg + Num2  -  Storage ESP",
                    "Strg + Num3  -  Item ESP",
                    "Strg + Num4  -  Spawner Finder",
                    "Strg + Num5  -  Through Walls",
                    "Strg + Num6  -  Nametags",
                    "Strg + Num7  -  AutoTool",
                    "Strg + Num8  -  Tool Saver",
                    "Strg + Num9  -  Auto Refill",
                    "Strg + Num0  -  Auto Eat"} : new String[] {
                    "Ctrl + Num1  -  Player ESP",
                    "Ctrl + Num2  -  Storage ESP",
                    "Ctrl + Num3  -  Item ESP",
                    "Ctrl + Num4  -  Spawner Finder",
                    "Ctrl + Num5  -  Through Walls",
                    "Ctrl + Num6  -  Nametags",
                    "Ctrl + Num7  -  AutoTool",
                    "Ctrl + Num8  -  Tool Saver",
                    "Ctrl + Num9  -  Auto Refill",
                    "Ctrl + Num0  -  Auto Eat"};
            default -> de ? new String[] {
                    "Weitere Module sind ab Werk unbelegt.",
                    "Strg bleibt fuer Numpad-Module erforderlich,",
                    "auch wenn du deren Taste neu belegst.",
                    "Minecraft speichert Tasten in options.txt.",
                    "Alte Belegungen koennen neue Defaults ueberdecken.",
                    "Bei Bedarf in Steuerung neu belegen.",
                    "Achtung: NumLock / Tastatur-Layout pruefen.",
                    "UI-Sprache bleibt ueber alle Server gleich."} : new String[] {
                    "Other modules start unbound.",
                    "Ctrl is required for the Numpad actions,",
                    "even after remapping their base key.",
                    "Minecraft saves bindings in options.txt.",
                    "Previous bindings can override new defaults.",
                    "Rebind in Controls if needed.",
                    "Check NumLock and your keyboard layout.",
                    "UI language is shared across all servers."};
        };
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        g.fill(x, y, x + w, y + h, 0xF0141C29);
        g.fill(x, y, x + w, y + 33, 0xFF1F3342);
        g.fill(x, y + 31, x + w, y + 33, CYAN);
        g.text(font, "WUKONG  /  " + WuKongLanguage.text("EXTRAS & HILFE", "EXTRAS & HELP"), x + 16, y + 12, 0xFFFFFFFF, false);
        g.text(font, titleForPage(), x + 18, y + 49, CYAN, false);
        g.fill(x + 18, y + 65, x + w - 18, y + 66, 0xFF355067);
        String[] content = lines();
        int lineStart = y + 79;
        int max = Math.max(1, (y + h - 66 - lineStart) / 16);
        for (int i=0; i<content.length && i<max; i++) {
            if ((i & 1) == 0) g.fill(x + 18, lineStart + i*16 - 4, x + w - 18, lineStart + i*16 + 11, 0x35263A4C);
            g.text(font, content[i], x + 25, lineStart + i*16, WHITE, false);
        }
        g.text(font, (page + 1) + " / 8", x + w - 58, y + 49, MUTED, false);
        super.extractRenderState(g, mx, my, partial);
    }
    @Override public void onClose() { Minecraft.getInstance().gui.setScreen(new BenchScreen()); }
    @Override public boolean isPauseScreen() { return false; }
}
