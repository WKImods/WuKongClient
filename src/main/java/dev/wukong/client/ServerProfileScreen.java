package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Readable status page: profile switching itself is automatic. */
public final class ServerProfileScreen extends Screen {
    private int x, y, w;

    public ServerProfileScreen() { super(Component.literal("WuKong - Server Profiles")); }

    @Override protected void init() {
        w = Math.min(430, width - 24);
        x = (width - w) / 2;
        y = Math.max(8, (height - 252) / 2);
        addRenderableWidget(Button.builder(Component.literal(WuKongLanguage.text("Aktuelles Profil speichern", "Save current profile")), b -> {
            BenchConfig.INSTANCE.save();
            b.setMessage(Component.literal(WuKongLanguage.text("Profil gespeichert", "Profile saved")));
        }).bounds(x + 18, y + 168, w - 36, 22).build());
        addRenderableWidget(Button.builder(Component.literal(WuKongLanguage.text("Zurueck", "Back")), b -> onClose())
                .bounds(x + 18, y + 199, w - 36, 22).build());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        g.fill(x, y, x + w, y + 247, 0xF0111925);
        g.fill(x, y, x + w, y + 30, 0xFF203E4C);
        g.fill(x, y + 28, x + w, y + 30, 0xFF5BDCE7);
        g.text(font, "WUKONG  /  SERVER PROFILES", x + 16, y + 11, 0xFFFFFFFF, false);
        g.text(font, WuKongLanguage.text("Aktives Profil:", "Active profile:"), x + 18, y + 45, 0xFF9CBECC, false);
        String label = ServerProfiles.activeLabel();
        if (label.length() > 52) label = label.substring(0, 49) + "...";
        g.text(font, label, x + 18, y + 60, 0xFF89F0CF, false);
        g.fill(x + 18, y + 81, x + w - 18, y + 82, 0xFF354857);
        g.text(font, WuKongLanguage.text("Einstellungen werden beim Weltwechsel", "Settings are saved and loaded when"), x + 18, y + 94, 0xFFE0E9EF, false);
        g.text(font, WuKongLanguage.text("automatisch gespeichert und geladen.", "switching between worlds/servers."), x + 18, y + 108, 0xFFE0E9EF, false);
        g.text(font, WuKongLanguage.text("Waypoints und Todespunkte bleiben pro Welt.", "Waypoints and death markers stay per world."), x + 18, y + 126, 0xFF9CBECC, false);
        g.text(font, WuKongLanguage.text("Datei: ", "File: ") + shortName(ServerProfiles.activeFileLabel()), x + 18, y + 145,
                0xFFA0B7C4, false);
        super.extractRenderState(g, mx, my, partial);
    }

    private static String shortName(String s) {
        return s.length() > 52 ? s.substring(0, 49) + "..." : s;
    }

    @Override public void onClose() {
        BenchConfig.INSTANCE.save();
        Minecraft.getInstance().gui.setScreen(new BenchScreen());
    }
    @Override public boolean isPauseScreen() { return false; }
}
