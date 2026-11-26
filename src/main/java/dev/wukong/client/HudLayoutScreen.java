package dev.wukong.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

final class HudLayoutScreen extends Screen {
    private final Screen parent;
    private int selected=0,drag=-1; private double offsetX,offsetY;
    private int canvasH,renderW,renderH; private Button choose; private Control[] controls;
    HudLayoutScreen(){this(0);}
    HudLayoutScreen(int initial){this(initial,null);}
    HudLayoutScreen(int initial,Screen parent){super(Component.literal("HUD Editor 6.0"));this.parent=parent;selected=Math.max(0,Math.min(3,initial));}
    private String tr(String de,String en){return HudStudioScreen.tr(de,en);}
    @Override protected void init(){
        canvasH=Math.max(1,height-108);renderW=width;renderH=canvasH;drag=-1;
        int w=Math.max(40,(width-16)/2),x=5,y=canvasH+17;
        choose=addRenderableWidget(Button.builder(Component.literal(HudStudio.name(selected)),b->{selected=(selected+1)%4;sync();}).bounds(x,y,w,20).build());
        addRenderableWidget(Button.builder(Component.literal(tr("Layout zur\u00FCcksetzen","Reset layout")),b->{int[] xs={20,20,750,750},ys={200,450,20,300};HudStudio.position(selected,xs[selected],ys[selected]);HudStudio.scale(selected,100);HudStudio.opacity(selected,65);sync();HudStudio.c.save();}).bounds(x+w+4,y,w,20).build());
        controls=new Control[]{new Control(x,y+22,w,0),new Control(x+w+4,y+22,w,1),new Control(x,y+44,w,2),new Control(x+w+4,y+44,w,3)};
        for(Control c:controls)addRenderableWidget(c);
        addRenderableWidget(Button.builder(Component.literal(tr("Fertig","Done")),b->onClose()).bounds(width/2-45,height-20,90,18).build());sync();
    }
    private class Control extends AbstractSliderButton {
        final int kind;
        Control(int x,int y,int w,int kind){super(x,y,w,20,Component.empty(),0);this.kind=kind;}
        void syncValue(){value=switch(kind){case 0->HudStudio.x(selected)/1000.;case 1->HudStudio.y(selected)/1000.;case 2->(HudStudio.scale(selected)-50)/150.;default->HudStudio.opacity(selected)/100.;};updateMessage();}
        @Override protected void updateMessage(){setMessage(Component.literal(switch(kind){case 0->"X: "+HudStudio.x(selected)/10+"%";case 1->"Y: "+HudStudio.y(selected)/10+"%";case 2->tr("Gr\u00F6\u00DFe","Scale")+": "+HudStudio.scale(selected)+"%";default->tr("Hintergrund","Background")+": "+HudStudio.opacity(selected)+"%";}));}
        @Override protected void applyValue(){switch(kind){case 0->HudStudio.position(selected,(int)Math.round(value*1000),HudStudio.y(selected));case 1->HudStudio.position(selected,HudStudio.x(selected),(int)Math.round(value*1000));case 2->HudStudio.scale(selected,50+(int)Math.round(value*150));default->HudStudio.opacity(selected,(int)Math.round(value*100));}HudStudio.c.save();}
    }
    private void sync(){if(choose!=null)choose.setMessage(Component.literal(HudStudio.name(selected)));if(controls!=null)for(Control c:controls)c.syncValue();}
    private HudStudio.Rect bounds(int i){var mc=Minecraft.getInstance();return HudStudio.bounds(mc,i,renderW,renderH,HudStudio.lines(mc,i,true));}
    private int hit(double x,double y){if(y>=canvasH)return -1; if(bounds(selected).contains(x,y))return selected;for(int i=3;i>=0;i--)if(bounds(i).contains(x,y))return i;return -1;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        renderW=width;renderH=canvasH;g.fill(0,0,width,height,0x88000000);var mc=Minecraft.getInstance();
        for(int i=0;i<4;i++)if(i!=selected)HudStudio.drawOne(g,mc,i,true,renderW,renderH);HudStudio.drawOne(g,mc,selected,true,renderW,renderH);
        var b=bounds(selected);g.fill(b.x(),b.y(),b.x()+b.w(),b.y()+1,0xFF5BDCE7);g.fill(b.x(),b.y()+b.h()-1,b.x()+b.w(),b.y()+b.h(),0xFF5BDCE7);
        g.fill(0,canvasH,width,height,0xFF17232E);g.text(font,"HUD Editor 6.0 | "+HudStudio.name(selected),5,canvasH+4,0xFF5BDCE7,true);super.extractRenderState(g,mx,my,partial);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean dbl){if(super.mouseClicked(e,dbl))return true;if(e.y()>=canvasH)return false;if(e.button()!=1)return false;int i=hit(e.x(),e.y());if(i<0)return false;setFocused(null);selected=drag=i;var b=bounds(i);offsetX=e.x()-b.x();offsetY=e.y()-b.y();setDragging(true);sync();return true;}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(drag<0)return super.mouseDragged(e,dx,dy);var b=bounds(drag);HudStudio.position(drag,(int)Math.round((e.x()-offsetX)*1000/Math.max(1,renderW-b.w())),(int)Math.round((e.y()-offsetY)*1000/Math.max(1,renderH-b.h())));sync();return true;}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(drag>=0&&e.button()==1){drag=-1;setDragging(false);HudStudio.c.save();return true;}return super.mouseReleased(e);}
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){if(sy==0 || y>=canvasH)return super.mouseScrolled(x,y,sx,sy);int i=hit(x,y);if(i>=0)selected=i;int d=sy>0?10:-10;if(Minecraft.getInstance().hasShiftDown())HudStudio.opacity(selected,HudStudio.opacity(selected)+d);else HudStudio.scale(selected,HudStudio.scale(selected)+d);sync();HudStudio.c.save();return true;}
    @Override public void onClose(){HudStudio.c.save();Minecraft.getInstance().gui.setScreen(parent==null?new HudStudioScreen():parent);}
    @Override public boolean isPauseScreen(){return false;}
}
