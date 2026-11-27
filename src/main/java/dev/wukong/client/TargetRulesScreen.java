package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Separate configuration page; does not change the existing main screen layout. */
public final class TargetRulesScreen extends Screen {
    private final BenchConfig cfg = BenchConfig.INSTANCE;
    private int x, y, w;
    private Button priorityButton;

    public TargetRulesScreen() { super(Component.literal(UiLabels.t("WuKong Target Rules"))); }

    @Override protected void init() {
        w = Math.min(340, width - 20);
        x = (width - w) / 2;
        y = (height - 366) / 2;
        int row = y + 34;
        priorityButton = addRenderableWidget(Button.builder(priorityText(), b -> {
            cfg.aimTargetPriority = TargetRules.next(cfg.aimTargetPriority);
            cfg.save();
            priorityButton.setMessage(priorityText());
        }).bounds(x + 10, row, w - 20, 20).build());
        row += 30;
        addToggle(row, UiLabels.t("Aim till Death (lock)"), () -> cfg.aimTillDeath,
                value -> cfg.aimTillDeath = value);
        row += 30;
        addToggle(row, UiLabels.t("Hit till Death (auto melee)"), () -> cfg.aimHitTillDeath,
                value -> cfg.aimHitTillDeath = value);
        row += 30;
        addToggle(row, UiLabels.t("Bow trigger (auto release)"), () -> cfg.aimTriggerBow,
                value -> cfg.aimTriggerBow = value);
        row += 30;
        addToggle(row, UiLabels.t("Steady Bow View (smoother)"), () -> cfg.steadyBowView,
                value -> cfg.steadyBowView = value);
        row += 30;
        addToggle(row, UiLabels.t("Separate Camera (bow view)"), () -> cfg.separateBowCamera,
                value -> cfg.separateBowCamera = value);
        row += 30;
        addRenderableWidget(Button.builder(spotText(), b -> {
            cfg.aimSpot = AimSpot.next(cfg.aimSpot);
            cfg.save();
            b.setMessage(spotText());
        }).bounds(x + 10, row, w - 20, 20).build());
        row += 42;
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Back to Combat")), b -> {
            cfg.save();
            Minecraft.getInstance().gui.setScreen(new CombatScreen(1));
        }).bounds(x + 10, row, w - 20, 20).build());
    }

    private Component spotText() {
        return Component.literal(UiLabels.t("Aim Spot: ") + AimSpot.label(cfg.aimSpot));
    }

    private Component priorityText() {
        return Component.literal(UiLabels.t("Target priority: ") + TargetRules.label(cfg.aimTargetPriority));
    }

    private interface Reader { boolean get(); }
    private interface Writer { void set(boolean value); }

    private void addToggle(int row, String label, Reader reader, Writer writer) {
        addRenderableWidget(Button.builder(Component.literal(label + ": " + onOff(reader.get())), b -> {
            writer.set(!reader.get());
            cfg.save();
            b.setMessage(Component.literal(label + ": " + onOff(reader.get())));
        }).bounds(x + 10, row, w - 20, 20).build());
    }

    private static String onOff(boolean value) { return value ? UiLabels.t("ON") : UiLabels.t("OFF"); }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        g.fill(x, y, x + w, y + 366, 0xE5111A24);
        g.fill(x, y, x + w, y + 26, 0xFF204051);
        g.text(font, UiLabels.t("WUKONG / TARGET RULES"), x + 10, y + 9, 0xFFFFFFFF, false);
        super.extractRenderState(g, mx, my, partial);
        g.text(font, UiLabels.t("Lock ends on death, loss of sight or range"), x + 10, y + 320, 0xFF9AC4D2, false);
        g.text(font, UiLabels.t("Auto melee: vanilla reach + attack cooldown"), x + 10, y + 338, 0xFF9AC4D2, false);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { cfg.save(); super.onClose(); }
}
