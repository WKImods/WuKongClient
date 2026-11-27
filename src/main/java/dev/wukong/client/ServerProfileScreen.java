package dev.wukong.client;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ServerProfileScreen extends Screen {
    private int x,w,page; private List<String> names=List.of();private String selected="",status="",confirm="";
    private EditBox name;private Button[] rows;private Button prev,next;private int listTop=42;
    public ServerProfileScreen(){super(Component.literal(UiLabels.t("WuKong - Profile Manager+")));}
    private String tr(String de,String en){return WuKongLanguage.text(de,en);}
    private interface Task {void run() throws Exception;}
    private String selected(){if(!names.contains(selected))throw new IllegalStateException(tr("Profil ausw\u00E4hlen","Select a profile"));return selected;}
    private String input(){return name.getValue().trim();}
    private void select(String n){selected=n;confirm="";name.setValue(n);}
    private void action(Task t){try{t.run();status="OK";reload();}catch(Exception e){status=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}}
    private void reload() throws Exception {names=ProfileLibrary.names();if(!names.contains(selected))selected=names.isEmpty()?"":names.get(0);if(!selected.isEmpty())page=names.indexOf(selected)/rows.length;refresh();}
    private void refresh(){page=Math.max(0,Math.min(page,Math.max(0,(names.size()-1)/rows.length)));for(int i=0;i<rows.length;i++){int k=page*rows.length+i;rows[i].visible=k<names.size();if(k<names.size()){String n=names.get(k);rows[i].setMessage(Component.literal((n.equals(selected)?"\u25B6 ":"")+n+(n.equals(BenchConfig.INSTANCE.appliedProfileName)?"  \u2713":"")));}}prev.active=page>0;next.active=(page+1)*rows.length<names.size();}
    private void button(String de,String en,int col,int y,Task t){int bw=(w-4)/2;addRenderableWidget(Button.builder(Component.literal(tr(de,en)),b->action(t)).bounds(x+col*(bw+4),y,bw,20).build());}
    @Override protected void init(){w=Math.min(420,width-12);x=(width-w)/2;int count=Math.max(1,(height-194)/22);rows=new Button[count];for(int i=0;i<count;i++){final int j=i;rows[i]=addRenderableWidget(Button.builder(Component.empty(),b->{int k=page*rows.length+j;if(k<names.size()){select(names.get(k));refresh();}}).bounds(x,listTop+i*22,w,20).build());}
        int y=listTop+count*22;prev=addRenderableWidget(Button.builder(Component.literal("<"),b->{page--;refresh();}).bounds(x,y,36,20).build());next=addRenderableWidget(Button.builder(Component.literal(">"),b->{page++;refresh();}).bounds(x+w-36,y,36,20).build());
        name=addRenderableWidget(new EditBox(font,x,y+22,w,20,Component.literal(tr("Vorlagenname","Template name"))));name.setMaxLength(40);name.setValue(selected.isEmpty()?"New profile":selected);
        button("Neu / Aktuell sichern","New / Save current",0,y+44,()->{String n=input();ProfileLibrary.write(n,false,ServerProfiles.snapshot());select(n);});
        button("Anwenden","Apply to current environment",1,y+44,()->{String n=selected();ServerProfiles.applyTemplate(ProfileLibrary.read(n,false));BenchConfig.INSTANCE.appliedProfileName=n;BenchConfig.INSTANCE.save();});
        button("Duplizieren","Duplicate",0,y+66,()->{String n=input();ProfileLibrary.write(n,false,ProfileLibrary.read(selected(),false));select(n);});
        button("Umbenennen","Rename",1,y+66,()->{String old=selected(),n=input();ProfileLibrary.rename(old,n);if(old.equals(BenchConfig.INSTANCE.appliedProfileName)){BenchConfig.INSTANCE.appliedProfileName=n;BenchConfig.INSTANCE.save();}select(n);});
        button("Exportieren","Export",0,y+88,()->ProfileLibrary.write(input(),true,ProfileLibrary.read(selected(),false)));
        button("Importieren (Schalter)","Import (toggles)",1,y+88,()->{String n=input();ProfileLibrary.write(n,false,ProfileLibrary.read(n,true));select(n);});
        button("L\u00F6schen (2x)","Delete (2x)",0,y+110,()->{String n=selected();if(!confirm.equals(n)){confirm=n;throw new IllegalStateException(tr("Erneut klicken zum L\u00F6schen","Click again to delete"));}ProfileLibrary.delete(n);if(n.equals(BenchConfig.INSTANCE.appliedProfileName)){BenchConfig.INSTANCE.appliedProfileName="";BenchConfig.INSTANCE.save();}selected="";confirm="";});
        button("Sichere Defaults","Safe defaults",1,y+110,()->ServerProfiles.applyTemplate(ServerProfiles.conservativeProfile()));
        addRenderableWidget(Button.builder(Component.literal(tr("Zur\u00FCck","Back")),b->onClose()).bounds(x,height-20,w,18).build());
        if(selected.isEmpty()&&BenchConfig.INSTANCE.appliedProfileName!=null)selected=BenchConfig.INSTANCE.appliedProfileName;action(()->{});if(!selected.isEmpty())name.setValue(selected);
    }
    @Override public boolean mouseScrolled(double a,double b,double sx,double sy){if(b>=listTop&&b<listTop+rows.length*22&&sy!=0){page+=sy>0?-1:1;refresh();return true;}return super.mouseScrolled(a,b,sx,sy);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){g.fill(0,0,width,height,0xEE111925);g.text(font,"WUKONG / PROFILE MANAGER+",x,3,0xFF5BDCE7,true);g.text(font,font.plainSubstrByWidth(ServerProfiles.activeLabel(),w),x,15,0xFFFFFFFF,true);String n=BenchConfig.INSTANCE.appliedProfileName;g.text(font,font.plainSubstrByWidth(tr("Zuletzt angewendet: ","Last applied: ")+(n==null||n.isBlank()?tr("Keine Vorlage","No template"):n),w),x,27,0xFF89F0CF,true);super.extractRenderState(g,mx,my,partial);g.text(font,font.plainSubstrByWidth(status,Math.max(1,w-84)),x+42,listTop+rows.length*22+6,0xFFE0E9EF,true);}
    @Override public void onClose(){BenchConfig.INSTANCE.save();Minecraft.getInstance().gui.setScreen(new BenchScreen());}
    @Override public boolean isPauseScreen(){return false;}
}
