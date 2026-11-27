package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

/** ESP dashboard: global switches plus navigation to independent feature groups. */
public final class EspScreen extends Screen {
    private final BenchConfig cfg = BenchConfig.INSTANCE;
    private int x, y, w;
    private interface Reader { boolean get(); }
    private interface Writer { void set(boolean value); }
    public EspScreen() { super(Component.literal(UiLabels.t("WuKong ESP Dashboard"))); }

    @Override protected void init() {
        w = Math.min(430, width - 16);
        x = (width - w) / 2;
        y = (height - 244) / 2;
        toggle(y + 35, UiLabels.t("ESP master [") + WuKongKeys.ESP.getTranslatedKeyMessage().getString() + "]",
                () -> cfg.espEnabled, v -> cfg.espEnabled = v);
        toggle(y + 63, UiLabels.t("Through Walls"), () -> cfg.espThroughWalls, v -> cfg.espThroughWalls = v);
        int c = (w - 30) / 2;
        button(x+10,y+103,c,UiLabels.t("Entity ESP / Mobs..."), () -> new EntityEspScreen());
        button(x+20+c,y+103,c,UiLabels.t("Ore / Block Finder..."), () -> new OreScreen());
        button(x+10,y+132,c,UiLabels.t("Storage / Items..."), () -> new WorldEspScreen());
        button(x+20+c,y+132,c,UiLabels.t("Visual settings..."), () -> new VisualEspScreen());
        button(x+10,y+161,c,UiLabels.t("HUD / Utilities..."), () -> new ModulesScreen(1));
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Keybinds...")), b ->
                Minecraft.getInstance().gui.setScreen(new KeyBindsScreen(this, Minecraft.getInstance().options)))
                .bounds(x+20+c,y+161,c,20).build());
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Back to WuKong")), b -> {
            cfg.save(); Minecraft.getInstance().gui.setScreen(new BenchScreen());
        }).bounds(x+10,y+204,w-20,20).build());
    }
    private void button(int xx,int yy,int ww,String title,java.util.function.Supplier<Screen> target) {
        addRenderableWidget(Button.builder(Component.literal(title), b -> {
            cfg.save(); Minecraft.getInstance().gui.setScreen(target.get());
        }).bounds(xx,yy,ww,20).build());
    }
    private void toggle(int yy,String label,Reader get,Writer set) {
        addRenderableWidget(Button.builder(Component.literal(label+": "+(get.get()?UiLabels.t("ON"):UiLabels.t("OFF"))),b -> {
            set.set(!get.get());cfg.save();
            b.setMessage(Component.literal(label+": "+(get.get()?UiLabels.t("ON"):UiLabels.t("OFF"))));
        }).bounds(x+10,yy,w-20,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial) {
        g.fill(x,y,x+w,y+244,0xE5111A24);
        g.fill(x,y,x+w,y+26,0xFF204051);
        g.text(font,UiLabels.t("WUKONG  /  ESP DASHBOARD"),x+10,y+9,0xFFFFFFFF,false);
        super.extractRenderState(g,mx,my,partial);
        g.text(font,UiLabels.t("MODULES  -  individually adjustable"),x+10,y+91,0xFF9AC4D2,false);
        g.text(font,UiLabels.t("Independent Mob / Ore visuals & keybinds"),x+10,y+188,0xFF9AC4D2,false);
        g.text(font,UiLabels.t("Master OFF hides every ESP module"),x+10,y+231,0xFF9AC4D2,false);
    }
    @Override public void onClose() { cfg.save(); super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }
}
