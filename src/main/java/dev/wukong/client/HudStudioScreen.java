package dev.wukong.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class HudStudioScreen extends Screen {
    public HudStudioScreen(){super(Component.literal(UiLabels.t("HUD Studio 6.0")));}
    static String tr(String de,String en){return WuKongLanguage.text(de,en);}
    @Override protected void init(){int w=Math.min(300,width-16),x=(width-w)/2,h=Math.max(13,Math.min(24,(height-25)/9)),y=18;
        addRenderableWidget(Button.builder(Component.literal(UiLabels.t("HUD Master: ")+(HudStudio.c.hudMasterEnabled?UiLabels.t("ON"):UiLabels.t("OFF"))),b->{HudStudio.c.hudMasterEnabled=!HudStudio.c.hudMasterEnabled;HudStudio.c.save();rebuildWidgets();}).bounds(x,y,w,h-2).build());y+=h;
        for(int i=0;i<4;i++){final int n=i;int toggleW=w-84;addRenderableWidget(Button.builder(Component.literal(label(n)),b->{HudStudio.toggle(n);HudStudio.c.save();b.setMessage(Component.literal(label(n)));}).bounds(x,y,toggleW,h-2).build());
            addRenderableWidget(Button.builder(Component.literal(tr("Einstellen","Configure")),btn->Minecraft.getInstance().gui.setScreen(new HudLayoutScreen(n,this))).bounds(x+toggleW+4,y,w-toggleW-4,h-2).build());y+=h;}
        addRenderableWidget(Button.builder(Component.literal(tr("HUD bearbeiten","Edit HUD layout")),b->Minecraft.getInstance().gui.setScreen(new HudLayoutScreen(0,this))).bounds(x,y,w,h-2).build());y+=h;
        addRenderableWidget(Button.builder(Component.literal(tr("Ressourcenziel","Resource goal")),b->Minecraft.getInstance().gui.setScreen(new ResourceGoalScreen(this))).bounds(x,y,w,h-2).build());y+=h;
        addRenderableWidget(Button.builder(Component.literal(tr("Bisherige HUD-Optionen","Legacy HUD / Utilities")),b->Minecraft.getInstance().gui.setScreen(new ModulesScreen(1))).bounds(x,y,w,h-2).build());y+=h;
        addRenderableWidget(Button.builder(Component.literal(tr("Zur\u00FCck","Back")),b->onClose()).bounds(x,y,w,h-2).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){super.extractRenderState(g,mx,my,partial);g.text(font,UiLabels.t("HUD Studio 6.0"),4,4,0xFF5BDCE7,true);}
    private String label(int i){return HudStudio.name(i)+": "+(!HudStudio.enabled(i)?UiLabels.t("OFF"):HudStudio.c.hudMasterEnabled?UiLabels.t("ON"):UiLabels.t("PAUSED"));}
    @Override public void onClose(){HudStudio.c.save();Minecraft.getInstance().gui.setScreen(new BenchScreen());}
    @Override public boolean isPauseScreen(){return false;}
}
