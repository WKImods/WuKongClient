package dev.wukong.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class InventorySorterScreen extends Screen {
    private final Screen parent;private final BenchConfig cfg=BenchConfig.INSTANCE;private int x,y,w;
    InventorySorterScreen(Screen parent){super(Component.literal(UiLabels.t("Inventory Sorter")));this.parent=parent;}
    private static String tr(String de,String en){return WuKongLanguage.text(de,en);}
    private Button button(String label,int x,int y,int w,Button.OnPress action){return addRenderableWidget(Button.builder(Component.literal(label),action).bounds(x,y,w,20).build());}
    private String orderLabel(){return tr("Reihenfolge: ","Order: ")+switch(InventorySortPlan.order(cfg.inventorySortOrder)){
        case TYPE->tr("Item-ID","Item ID");case NAME->tr("Alphabetisch","Alphabetical");case AMOUNT->tr("Menge je Variante","Quantity per variant");case MOD->tr("Mod / Name","Mod / name");};}
    private String mergeLabel(){return tr("Stacks zusammenfassen: ","Merge stacks: ")+(cfg.inventorySortMerge?UiLabels.t("ON"):UiLabels.t("OFF"));}
    private String slotLabel(int i){return ((cfg.inventorySortLocked&(1<<i))!=0?"X ":"")+(i+9);}
    @Override protected void init(){
        w=Math.min(360,width-12);x=(width-w)/2;y=Math.max(3,(height-174)/2);
        button(orderLabel(),x+6,y+24,w-12,b->{var values=InventorySortPlan.Order.values();cfg.inventorySortOrder=values[(InventorySortPlan.order(cfg.inventorySortOrder).ordinal()+1)%values.length].name();cfg.save();b.setMessage(Component.literal(orderLabel()));});
        button(mergeLabel(),x+6,y+47,w-12,b->{cfg.inventorySortMerge=!cfg.inventorySortMerge;cfg.save();b.setMessage(Component.literal(mergeLabel()));});
        int cell=(w-12)/9;
        for(int i=0;i<27;i++){final int n=i;button(slotLabel(i),x+6+(i%9)*cell,y+84+(i/9)*22,cell-2,b->{cfg.inventorySortLocked^=1<<n;cfg.save();b.setMessage(Component.literal(slotLabel(n)));});}
        button(tr("Zurück","Back"),x+6,y+151,w-12,b->onClose());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        g.fill(x,y,x+w,y+174,EvolutionUi.panel());g.fill(x,y,x+w,y+2,EvolutionUi.accent());
        g.text(font,UiLabels.t("WUKONG / INVENTORY SORTER"),x+6,y+9,EvolutionUi.accent(),true);
        g.text(font,tr("X = geschützt | Hotbar immer geschützt","X = locked | Hotbar always protected"),x+6,y+70,0xFFCDD9E3,false);
        super.extractRenderState(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){cfg.save();Minecraft.getInstance().gui.setScreen(parent);}
}
