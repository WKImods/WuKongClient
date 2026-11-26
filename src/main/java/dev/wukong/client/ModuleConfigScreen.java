package dev.wukong.client;

import java.util.*;
import java.util.function.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

/** Dedicated settings; existing specialized editors remain reachable without rewriting their logic. */
final class ModuleConfigScreen extends Screen {
    private final Screen parent;
    private final ModuleCatalog.Module module;
    private final BenchConfig c=BenchConfig.INSTANCE;
    private final List<Consumer<Integer>> controls=new ArrayList<>();
    private int x,w,page,perPage;
    ModuleConfigScreen(Screen parent,ModuleCatalog.Module m){super(Component.literal(m.name()));this.parent=parent;module=m;}
    private static String tr(String de,String en){return ModuleCatalog.tr(de,en);}
    private void open(Screen s){c.save();Minecraft.getInstance().gui.setScreen(s);}
    private Button button(String label,int x,int y,int w,Button.OnPress action){return addRenderableWidget(Button.builder(Component.literal(label),action).bounds(x,y,w,20).build());}
    private void action(String label,Runnable r){controls.add(y->button(label,x+8,y,w-16,b->r.run()));}
    private void toggle(String label,BooleanSupplier get,Consumer<Boolean> set){controls.add(y->button(label+": "+(get.getAsBoolean()?"ON":"OFF"),x+8,y,w-16,b->{set.accept(!get.getAsBoolean());c.save();b.setMessage(Component.literal(label+": "+(get.getAsBoolean()?"ON":"OFF")));}));}
    private void slider(String label,int min,int max,IntSupplier get,IntConsumer set){controls.add(y->addRenderableWidget(new AbstractSliderButton(x+8,y,w-16,20,Component.literal(label+": "+get.getAsInt()),Math.max(0,Math.min(1,(get.getAsInt()-min)/(double)(max-min)))){
        @Override protected void updateMessage(){setMessage(Component.literal(label+": "+get.getAsInt()));}
        @Override protected void applyValue(){set.accept(min+(int)Math.round(value*(max-min)));c.save();}
    }));}
    private String eatModeLabel(){
        return tr("Modus: ","Mode: ")+(c.autoEatBackground
                ? tr("Hintergrund (Einzelspieler)","Background (singleplayer)")
                : tr("Standard (Hotbar)","Standard (hotbar)"));
    }
    private void configOptions(){
        switch(module.id()){
            case "players","items","spawner" -> action(tr("Darstellung / Reichweite","Style / range"),()->open(new VisualEspScreen()));
            case "mobs" -> action(tr("Mob-Einstellungen","Mob settings"),()->open(new EntityEspScreen()));
            case "ore" -> action(tr("Erz-Einstellungen","Ore settings"),()->open(new OreScreen()));
            case "storage" -> {toggle("Storage ESP+",()->c.storageEspPlus,v->c.storageEspPlus=v);action(tr("Lager-Einstellungen","Storage settings"),()->open(new WorldEspScreen()));}
            case "nametags" -> action(tr("Darstellung","Visual settings"),()->open(new VisualEspScreen()));
            case "waypoints","death" -> action(tr("Wegpunktliste","Waypoint list"),()->open(new WaypointListScreen()));
            case "bow_prediction","bow_view" -> action(tr("Vorhandene Bogeneinstellungen","Existing bow settings"),()->open(new CombatScreen(1)));
            case "zoom" -> {
                slider(tr("Startzoom (bis Max.)","Initial zoom (up to max)")+" (x)",2,50,()->c.zoomStart,v->c.zoomStart=v);
                slider(tr("Maximalzoom","Maximum zoom")+" (x)",2,50,()->c.zoomMax,v->c.zoomMax=v);
                toggle(tr("Weicher Zoom","Smooth zoom"),()->c.zoomSmooth,v->c.zoomSmooth=v);
                toggle(tr("Mausempfindlichkeit anpassen","Adaptive mouse sensitivity"),()->c.zoomAdaptive,v->c.zoomAdaptive=v);
                toggle(tr("Zoomstufe merken","Remember zoom level"),()->c.zoomRemember,v->c.zoomRemember=v);
            }
            case "toolsaver" -> slider(tr("Resthaltbarkeit","Durability reserve"),1,100,()->c.toolSaverMinDurability,v->c.toolSaverMinDurability=v);
            case "eat" -> {
                action(eatModeLabel(),()->{c.autoEatBackground=!c.autoEatBackground;c.save();rebuildWidgets();});
                slider(tr("Hungergrenze","Hunger threshold"),1,19,()->c.autoEatHungerThreshold,v->c.autoEatHungerThreshold=v);
            }
            case "enchant" -> toggle(tr("Details nur mit SHIFT","Details only with SHIFT"),()->c.enchantmentInfoShiftOnly,v->c.enchantmentInfoShiftOnly=v);
            case "sort" -> action(tr("Jetzt sortieren (Einzelspieler)","Sort now (singleplayer)"),()->SmartInventory.sort(Minecraft.getInstance()));
            case "armor" -> action(tr("HUD Layout bearbeiten","Edit HUD layout"),()->open(new HudLayoutScreen(0,this)));
            case "potion" -> action(tr("HUD Layout bearbeiten","Edit HUD layout"),()->open(new HudLayoutScreen(1,this)));
            case "performance" -> action(tr("HUD Layout bearbeiten","Edit HUD layout"),()->open(new HudLayoutScreen(2,this)));
            case "resource" -> {action(tr("HUD Layout bearbeiten","Edit HUD layout"),()->open(new HudLayoutScreen(3,this)));action(tr("Item & Sammelziel","Item & collection goal"),()->open(new ResourceGoalScreen(this)));}
            default -> {}
        }
        action(tr("Tastenbelegung","Keybinds"),()->open(new KeyBindsScreen(this,Minecraft.getInstance().options)));
        action(tr("Modul zur\u00FCcksetzen","Reset module"),()->open(new ResetModuleScreen(this,module)));
    }
    private void reset(){
        module.set().accept(false);
        switch(module.id()){
            case "zoom" -> {c.zoomStart=4;c.zoomMax=50;c.zoomRemembered=4;c.zoomSmooth=c.zoomAdaptive=c.zoomRemember=false;}
            case "toolsaver" -> c.toolSaverMinDurability=8;
            case "eat" -> {c.autoEatHungerThreshold=14;c.autoEatBackground=false;}
            case "enchant" -> c.enchantmentInfoShiftOnly=true;
            case "storage" -> c.storageEspPlus=false;
            case "armor","potion","performance","resource" -> {
                int i=switch(module.id()){case "armor"->0;case "potion"->1;case "performance"->2;default->3;};
                int[] xs={20,20,750,750},ys={200,450,20,300};HudStudio.position(i,xs[i],ys[i]);HudStudio.scale(i,100);HudStudio.opacity(i,65);
                if(i==3){c.resourceHudItem="minecraft:stone";c.resourceHudGoal=64;}
            }
            default -> {}
        }
        c.save();EvolutionUi.note(module.name()+" reset");
    }
    @Override protected void init(){
        w=Math.min(410,width-12);x=(width-w)/2;controls.clear();
        var group=ModuleCatalog.group(module.group());
        button("Master: "+(group.on().getAsBoolean()?"ON":"OFF"),x+8,34,(w-20)/2,b->{ModuleCatalog.toggle(group);rebuildWidgets();});
        button(tr("Modul: ","Module: ")+ModuleCatalog.state(module),x+w/2+2,34,(w-20)/2,b->{ModuleCatalog.toggle(module);rebuildWidgets();});
        configOptions();perPage=Math.max(1,(height-91)/23);page=Math.max(0,Math.min(page,Math.max(0,(controls.size()-1)/perPage)));
        for(int i=0;i<perPage&&page*perPage+i<controls.size();i++)controls.get(page*perPage+i).accept(59+i*23);
        int f=height-26,unit=(w-24)/3;
        var prev=button("<",x+8,f,unit,b->{page--;rebuildWidgets();});prev.active=page>0;
        var next=button(">",x+12+unit,f,unit,b->{page++;rebuildWidgets();});next.active=(page+1)*perPage<controls.size();
        button(tr("Zur\u00FCck","Back"),x+16+unit*2,f,unit,b->onClose());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float p){
        g.fill(x,4,x+w,height-3,EvolutionUi.panel());g.fill(x,4,x+w,6,EvolutionUi.accent());
        g.text(font,font.plainSubstrByWidth(module.name(),w-16),x+8,9,EvolutionUi.accent(),true);
        g.text(font,font.plainSubstrByWidth(module.description(),w-16),x+8,21,0xFFCDD9E3,false);
        super.extractRenderState(g,mx,my,p);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){open(parent);}
    private static final class ResetModuleScreen extends Screen{
        private final ModuleConfigScreen parent;private final ModuleCatalog.Module module;
        ResetModuleScreen(ModuleConfigScreen parent,ModuleCatalog.Module m){super(Component.literal("Reset module"));this.parent=parent;module=m;}
        @Override protected void init(){int w=Math.min(width-16,280),x=(width-w)/2,y=height/2;
            addRenderableWidget(Button.builder(Component.literal(tr("Zur\u00FCcksetzen","Reset")),b->{parent.reset();onClose();}).bounds(x,y,w/2-2,20).build());
            addRenderableWidget(Button.builder(Component.literal(tr("Abbrechen","Cancel")),b->onClose()).bounds(x+w/2+2,y,w/2-2,20).build());}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float p){g.fill(0,0,width,height,0xD0101924);g.text(font,font.plainSubstrByWidth(tr("Modul zur\u00FCcksetzen: ","Reset module: ")+module.name(),width-12),6,height/2-20,EvolutionUi.accent(),true);super.extractRenderState(g,mx,my,p);}
        @Override public void onClose(){Minecraft.getInstance().gui.setScreen(parent);}
        @Override public boolean isPauseScreen(){return false;}
    }
}
