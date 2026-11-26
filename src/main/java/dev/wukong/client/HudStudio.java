package dev.wukong.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

/** Read-only HUD. Coordinates are normalized to 0..1000 for GUI resize. */
final class HudStudio {
    static final String[] EN={"Armor HUD","Potion HUD","FPS & Ping","Resource Tracker"};
    static final String[] DE={"R\u00FCstung","Statuseffekte","FPS & Ping","Ressourcen"};
    static String name(int i){return WuKongLanguage.text(DE[i],EN[i]);}
    static final BenchConfig c=BenchConfig.INSTANCE;
    static int clamp(int v,int a,int b){return Math.max(a,Math.min(b,v));}
    static boolean enabled(int i){return switch(i){case 0->c.armorHudEnabled;case 1->c.potionHudEnabled;case 2->c.performanceHudEnabled;default->c.resourceHudEnabled;};}
    static void toggle(int i){switch(i){case 0->c.armorHudEnabled=!c.armorHudEnabled;case 1->c.potionHudEnabled=!c.potionHudEnabled;case 2->c.performanceHudEnabled=!c.performanceHudEnabled;default->c.resourceHudEnabled=!c.resourceHudEnabled;}}
    static int x(int i){return switch(i){case 0->c.armorHudX;case 1->c.potionHudX;case 2->c.performanceHudX;default->c.resourceHudX;};}
    static int y(int i){return switch(i){case 0->c.armorHudY;case 1->c.potionHudY;case 2->c.performanceHudY;default->c.resourceHudY;};}
    static int scale(int i){return clamp(switch(i){case 0->c.armorHudScale;case 1->c.potionHudScale;case 2->c.performanceHudScale;default->c.resourceHudScale;},50,200);}
    static int opacity(int i){return clamp(switch(i){case 0->c.armorHudOpacity;case 1->c.potionHudOpacity;case 2->c.performanceHudOpacity;default->c.resourceHudOpacity;},0,100);}
    static void position(int i,int x,int y){x=clamp(x,0,1000);y=clamp(y,0,1000);switch(i){case 0->{c.armorHudX=x;c.armorHudY=y;}case 1->{c.potionHudX=x;c.potionHudY=y;}case 2->{c.performanceHudX=x;c.performanceHudY=y;}default->{c.resourceHudX=x;c.resourceHudY=y;}}}
    static void scale(int i,int v){v=clamp(v,50,200);switch(i){case 0->c.armorHudScale=v;case 1->c.potionHudScale=v;case 2->c.performanceHudScale=v;default->c.resourceHudScale=v;}}
    static void opacity(int i,int v){v=clamp(v,0,100);switch(i){case 0->c.armorHudOpacity=v;case 1->c.potionHudOpacity=v;case 2->c.performanceHudOpacity=v;default->c.resourceHudOpacity=v;}}
    static List<String> lines(Minecraft mc,int i,boolean preview){
        var out=new ArrayList<String>();out.add(name(i));
        if(mc.player==null)return out;
        switch(i){
            case 0->{for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
                ItemStack s=mc.player.getItemBySlot(slot);if(s.isEmpty())continue;
                out.add(s.getHoverName().getString()+(s.isDamageableItem()?"  "+(s.getMaxDamage()-s.getDamageValue())+"/"+s.getMaxDamage():""));}
                if(out.size()==1)out.add(WuKongLanguage.text("Keine R\u00FCstung","No armor"));}
            case 1->{for(var e:mc.player.getActiveEffects()){
                int sec=Math.max(0,e.getDuration()/20);String time=e.isInfiniteDuration()?"\u221E":String.format(java.util.Locale.ROOT,"%d:%02d",sec/60,sec%60);
                out.add(e.getEffect().value().getDisplayName().getString()+" "+(e.getAmplifier()+1)+"  "+time);if(out.size()>=12)break;}
                if(out.size()==1)out.add(WuKongLanguage.text("Keine Effekte","No effects"));}
            case 2->{out.add(mc.getFps()+" FPS");var connection=mc.getConnection();var info=connection==null?null:connection.getPlayerInfo(mc.player.getUUID());out.add(mc.hasSingleplayerServer()?"Local":info==null?"Ping: \u2014":"Ping: "+info.getLatency()+" ms");}
            default->{var id=Identifier.tryParse(c.resourceHudItem==null?"":c.resourceHudItem);int count=0;
                if(id!=null)for(int j=0;j<36;j++){var stack=mc.player.getInventory().getItem(j);if(!stack.isEmpty()&&id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())))count+=stack.getCount();}
                int goal=clamp(c.resourceHudGoal,1,1000000);out.add(c.resourceHudItem==null?"minecraft:stone":c.resourceHudItem);out.add(count+" / "+goal+" ("+Math.min(100,(int)(100L*count/goal))+"%)");}
        }return out;
    }
    record Rect(int x,int y,int w,int h){boolean contains(double a,double b){return a>=x&&a<x+w&&b>=y&&b<y+h;}}
    static Rect bounds(Minecraft mc,int i,int w,int h,List<String> lines){int tw=40;for(String s:lines)tw=Math.max(tw,mc.font.width(s));float scale=scale(i)/100f;int bw=Math.min(w,(int)Math.ceil((tw+8)*scale)),bh=Math.min(h,(int)Math.ceil((lines.size()*11+8)*scale));return new Rect(clamp(x(i),0,1000)*Math.max(0,w-bw)/1000,clamp(y(i),0,1000)*Math.max(0,h-bh)/1000,bw,bh);}
    static void draw(GuiGraphicsExtractor g,Minecraft mc,boolean preview){for(int i=0;i<4;i++)if(preview||enabled(i))drawOne(g,mc,i,preview);}
    static void drawOne(GuiGraphicsExtractor g,Minecraft mc,int i,boolean preview){drawOne(g,mc,i,preview,g.guiWidth(),g.guiHeight());}
    static void drawOne(GuiGraphicsExtractor g,Minecraft mc,int i,boolean preview,int width,int height){var lines=lines(mc,i,preview);var b=bounds(mc,i,width,height,lines);float s=scale(i)/100f;g.pose().pushMatrix();try{g.pose().translate(b.x,b.y);g.pose().scale(s,s);int w=40;for(String line:lines)w=Math.max(w,mc.font.width(line));g.fill(0,0,w+8,lines.size()*11+8,(opacity(i)*255/100<<24)|0x101924);int y=4;for(String line:lines){g.text(mc.font,line,4,y,y==4?0xFF5BDCE7:0xFFE1EBF3,true);y+=11;}}finally{g.pose().popMatrix();}}
    private HudStudio(){}
}
