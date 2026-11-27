package dev.wukong.client;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

final class ResourceGoalScreen extends Screen {
    private final Screen parent;
    private record Entry(Identifier id,Item item,String name,String search){}
    private List<Entry> all=List.of(),filtered=List.of(); private EditBox search,goal; private Button[] rows;
    private Button prev,next; private int page,x,w,top;private Identifier selected;private String error="";
    ResourceGoalScreen(){this(null);}
    ResourceGoalScreen(Screen parent){super(Component.literal(UiLabels.t("Resource Tracker")));this.parent=parent;selected=Identifier.tryParse(HudStudio.c.resourceHudItem==null?"minecraft:stone":HudStudio.c.resourceHudItem);}
    private String tr(String de,String en){return HudStudioScreen.tr(de,en);}
    @Override protected void init(){
        w=Math.min(340,width-12);x=(width-w)/2;top=8;
        String query=search==null?"":search.getValue(),amount=goal==null?Integer.toString(HudStudio.c.resourceHudGoal):goal.getValue();
        if(all.isEmpty()){
            var mc=Minecraft.getInstance();ClientLanguage de=null;try{de=ClientLanguage.loadFrom(mc.getResourceManager(),List.of("en_us","de_de"),false);}catch(RuntimeException ignored){}
            var list=new ArrayList<Entry>();for(var id:BuiltInRegistries.ITEM.keySet()){var opt=BuiltInRegistries.ITEM.getOptional(id);if(opt.isEmpty()||id.toString().equals("minecraft:air"))continue;Item it=opt.get();String name=it.getDefaultInstance().getHoverName().getString();String key=it.getDescriptionId();String en=net.minecraft.locale.Language.DEFAULT_INSTANCE.getOrDefault(key);String german=de==null?"":de.getOrDefault(key);list.add(new Entry(id,it,name,(name+" "+id+" "+en+" "+german).toLowerCase(Locale.ROOT)));}list.sort(Comparator.comparing(Entry::name,String.CASE_INSENSITIVE_ORDER).thenComparing(e->e.id().toString()));all=List.copyOf(list);
        }
        search=addRenderableWidget(new EditBox(font,x,top+13,w,20,Component.literal(tr("Items suchen","Search items"))));search.setMaxLength(100);search.setValue(query);
        int count=Math.max(1,(height-130)/22);rows=new Button[count];for(int i=0;i<count;i++){final int j=i;rows[i]=addRenderableWidget(Button.builder(Component.empty(),b->{int k=page*rows.length+j;if(k<filtered.size()){selected=filtered.get(k).id();refresh();}}).bounds(x+23,top+38+i*22,w-23,20).build());}
        int y=top+38+count*22;
        prev=addRenderableWidget(Button.builder(Component.literal("<"),b->{page--;refresh();}).bounds(x,y,36,20).build());next=addRenderableWidget(Button.builder(Component.literal(">"),b->{page++;refresh();}).bounds(x+w-36,y,36,20).build());
        goal=addRenderableWidget(new EditBox(font,x,y+24,w/2-2,20,Component.literal(tr("Zielmenge","Target amount"))));goal.setMaxLength(7);goal.setValue(amount);
        addRenderableWidget(Button.builder(Component.literal(tr("Item in Hand","Use held item")),b->{var player=Minecraft.getInstance().player;if(player!=null&&!player.getMainHandItem().isEmpty()){selected=BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem());refresh();}}).bounds(x+w/2+2,y+24,w/2-2,20).build());
        addRenderableWidget(Button.builder(Component.literal(tr("Speichern","Save")),b->saveGoal()).bounds(x,y+48,w/2-2,20).build());addRenderableWidget(Button.builder(Component.literal(tr("Zur\u00FCck","Back")),b->onClose()).bounds(x+w/2+2,y+48,w/2-2,20).build());
        search.setResponder(q->{page=0;filter();});filter();
    }
    private void filter(){String q=search.getValue().trim().toLowerCase(Locale.ROOT);filtered=all.stream().filter(e->e.search().contains(q)).toList();refresh();}
    private void refresh(){page=Math.max(0,Math.min(page,Math.max(0,(filtered.size()-1)/rows.length)));for(int i=0;i<rows.length;i++){int k=page*rows.length+i;rows[i].visible=k<filtered.size();if(k<filtered.size()){var e=filtered.get(k);rows[i].setMessage(Component.literal((e.id().equals(selected)?"\u2713 ":"")+font.plainSubstrByWidth(e.name()+"  ["+e.id()+"]",w-37)));}}prev.active=page>0;next.active=(page+1)*rows.length<filtered.size();}
    private void saveGoal(){try{int n=Integer.parseInt(goal.getValue().trim());if(selected==null||BuiltInRegistries.ITEM.getOptional(selected).isEmpty()||n<1||n>1000000)throw new IllegalArgumentException();HudStudio.c.resourceHudItem=selected.toString();HudStudio.c.resourceHudGoal=n;HudStudio.c.save();onClose();}catch(RuntimeException e){error=tr("Ung\u00FCltiges Item oder Ziel","Invalid item or goal");}}
    @Override public boolean mouseScrolled(double a,double b,double sx,double sy){if(b>=top+38&&b<top+38+rows.length*22&&sy!=0){page+=sy>0?-1:1;refresh();return true;}return super.mouseScrolled(a,b,sx,sy);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float p){super.extractRenderState(g,mx,my,p);g.text(font,font.plainSubstrByWidth(tr("Ausgew\u00E4hlt: ","Selected: ")+selected,w),x,top,0xFF89F0CF,true);for(int i=0;i<rows.length;i++){int k=page*rows.length+i;if(k<filtered.size())g.item(filtered.get(k).item().getDefaultInstance(),x+2,top+40+i*22);}g.text(font,(page+1)+" / "+Math.max(1,(filtered.size()+rows.length-1)/rows.length),x+42,top+44+rows.length*22,0xFFFFFFFF,true);if(!error.isEmpty())g.text(font,error,x,height-10,0xFFFF7777,true);}
    @Override public void onClose(){Minecraft.getInstance().gui.setScreen(parent==null?new HudStudioScreen():parent);}
    @Override public boolean isPauseScreen(){return false;}
}
