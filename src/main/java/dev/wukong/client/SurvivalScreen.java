package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

/** Each survival module can be toggled separately and bound in vanilla Controls. */
public final class SurvivalScreen extends Screen {
    private final BenchConfig cfg=BenchConfig.INSTANCE;
    private int x,y,w;
    private interface Get { boolean run(); }
    private interface Set { void run(boolean value); }
    public SurvivalScreen() { super(Component.literal(UiLabels.t("WuKong Survival Automation"))); }

    @Override protected void init() {
        w=Math.min(400,width-16); x=(width-w)/2;y=(height-308)/2;
        toggle(36,UiLabels.t("AutoTool (mining)"),()->cfg.autoToolEnabled,v->cfg.autoToolEnabled=v);
        toggle(64,UiLabels.t("Tool Saver"),()->cfg.toolSaverEnabled,v->cfg.toolSaverEnabled=v);
        int col=(w-30)/2;
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("-1 use")),b->{
            cfg.toolSaverMinDurability=Math.max(1,cfg.toolSaverMinDurability-1);cfg.save();rebuildWidgets();
        }).bounds(x+10,y+91,col,20).build());
        addRenderableWidget(Button.builder(Component.literal("Reserve: "+cfg.toolSaverMinDurability+"  (+1)"),b->{
            cfg.toolSaverMinDurability=Math.min(100,cfg.toolSaverMinDurability+1);cfg.save();rebuildWidgets();
        }).bounds(x+20+col,y+91,col,20).build());
        toggle(124,UiLabels.t("Auto Refill (hotbar)"),()->cfg.autoRefillEnabled,v->cfg.autoRefillEnabled=v);
        toggle(152,UiLabels.t("Auto Eat"),()->cfg.autoEatEnabled,v->cfg.autoEatEnabled=v);
        addRenderableWidget(Button.builder(Component.literal("Hunger -1"),b->{
            cfg.autoEatHungerThreshold=Math.max(1,cfg.autoEatHungerThreshold-1);cfg.save();rebuildWidgets();
        }).bounds(x+10,y+179,col,20).build());
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Eat at <= ")+cfg.autoEatHungerThreshold+"  (+1)"),b->{
            cfg.autoEatHungerThreshold=Math.min(19,cfg.autoEatHungerThreshold+1);cfg.save();rebuildWidgets();
        }).bounds(x+20+col,y+179,col,20).build());
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Keybinds...")),b->
            Minecraft.getInstance().gui.setScreen(new KeyBindsScreen(this,Minecraft.getInstance().options)))
            .bounds(x+10,y+252,col,20).build());
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("Back")),b->
            Minecraft.getInstance().gui.setScreen(new BenchScreen()))
            .bounds(x+20+col,y+252,col,20).build());
    }
    private void toggle(int offset,String name,Get get,Set set) {
        addRenderableWidget(Button.builder(Component.literal(name+": "+(get.run()?UiLabels.t("ON"):UiLabels.t("OFF"))),b->{
            set.run(!get.run());cfg.save();
            b.setMessage(Component.literal(name+": "+(get.run()?UiLabels.t("ON"):UiLabels.t("OFF"))));
        }).bounds(x+10,y+offset,w-20,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial) {
        g.fill(x,y,x+w,y+308,0xE5111A24);
        g.fill(x,y,x+w,y+27,0xFF204051);
        g.text(font,UiLabels.t("WUKONG  /  SURVIVAL"),x+10,y+9,0xFFFFFFFF,false);
        super.extractRenderState(g,mx,my,partial);
        g.text(font,UiLabels.t("Tool Saver blocks mining below remaining durability"),x+10,y+217,0xFF9AC4D2,false);
        g.text(font,UiLabels.t("Auto Refill: hotbar items | Eat: food in hotbar"),x+10,y+231,0xFF9AC4D2,false);
        g.text(font,UiLabels.t("Bind modules in Minecraft Controls"),x+10,y+285,0xFF9AC4D2,false);
    }
    @Override public void onClose() { cfg.save();super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }
}
