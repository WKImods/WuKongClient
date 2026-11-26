package dev.wukong.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class SmartInventoryScreen extends Screen {
    private final BenchConfig cfg=BenchConfig.INSTANCE;
    private int x,y,w;
    public SmartInventoryScreen(){super(Component.literal("Smart Inventory"));}
    private static String tr(String de,String en){return WuKongLanguage.text(de,en);}
    @Override protected void init(){
        w=Math.min(360,width-16);x=(width-w)/2;y=Math.max(4,(height-238)/2);
        toggle(30,tr("Shulker-Vorschau","Shulker Preview"),()->cfg.shulkerPreviewEnabled,v->cfg.shulkerPreviewEnabled=v);
        toggle(54,tr("Bündel-Vorschau","Bundle Preview"),()->cfg.bundlePreviewEnabled,v->cfg.bundlePreviewEnabled=v);
        toggle(78,tr("Inventar sortieren","Inventory Sorter"),()->cfg.inventorySorterEnabled,v->cfg.inventorySorterEnabled=v);
        toggle(102,tr("Auto-Angeln","Auto Fish"),()->cfg.autoFishEnabled,v->cfg.autoFishEnabled=v);
        addRenderableWidget(Button.builder(Component.literal(tr("Jetzt sortieren (Singleplayer)","Sort now (singleplayer)")),b->SmartInventory.sort(Minecraft.getInstance())).bounds(x+10,y+126,w-20,20).build());
        addRenderableWidget(Button.builder(Component.literal(tr("Tastenbelegung","Keybinds")),b->Minecraft.getInstance().gui.setScreen(new KeyBindsScreen(this,Minecraft.getInstance().options))).bounds(x+10,y+154,(w-24)/2,20).build());
        addRenderableWidget(Button.builder(Component.literal(tr("Tooltip-Werkzeuge","Intelligent Tooltips")),b->Minecraft.getInstance().gui.setScreen(new IntelligentTooltipsScreen())).bounds(x+10,y+177,w-20,20).build());
        addRenderableWidget(Button.builder(Component.literal(tr("Zurück","Back")),b->Minecraft.getInstance().gui.setScreen(new BenchScreen())).bounds(x+w/2+2,y+154,(w-24)/2,20).build());
    }
    private void toggle(int pos,String name,BooleanSupplier get,Consumer<Boolean> set){
        addRenderableWidget(Button.builder(Component.literal(name+": "+(get.getAsBoolean()?"ON":"OFF")),b->{set.accept(!get.getAsBoolean());cfg.save();b.setMessage(Component.literal(name+": "+(get.getAsBoolean()?"ON":"OFF")));}).bounds(x+10,y+pos,w-20,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        g.fill(x,y,x+w,y+238,0xE5111A24);g.fill(x,y,x+w,y+25,0xFF204051);g.text(font,"WUKONG / SMART INVENTORY",x+10,y+8,0xFFFFFFFF,false);
        super.extractRenderState(g,mx,my,partial);
        g.text(font,tr("Sortieren / Angeln: nur Einzelspieler.","Sort / Fish: singleplayer only."),x+10,y+207,0xFF9AC4D2,false);
        g.text(font,tr("Angel manuell auswerfen.","Cast the fishing rod manually."),x+10,y+218,0xFF9AC4D2,false);
    }
    @Override public boolean isPauseScreen(){return false;}
}
