package dev.wukong.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Responsive module browser: grouped masters, individual toggles, config and favorites. */
public final class BenchScreen extends Screen {
    private final Set<String> expanded=new HashSet<>();
    private final boolean quick;
    private boolean favorites;
    private String query="";
    private int page,x,w,top,bottom;
    private List<Row> data=List.of();
    private RowButtons[] widgets;
    private Button prev,next,filter;
    private EditBox search;
    private record Row(ModuleCatalog.Group group,ModuleCatalog.Module module){}
    private record RowButtons(Button name,Button toggle,Button config,Button star){}
    public BenchScreen(){this(false);}
    public BenchScreen(boolean quick){super(Component.literal(quick?UiLabels.t("WuKong Quick Menu"):"WuKongClient "+EvolutionUi.installed()));this.quick=quick;favorites=quick;}
    private static String tr(String de,String en){return ModuleCatalog.tr(de,en);}
    private void open(Screen s){BenchConfig.INSTANCE.save();Minecraft.getInstance().gui.setScreen(s);}
    private Button button(String text,int x,int y,int w,int h,Button.OnPress action){return addRenderableWidget(Button.builder(Component.literal(text),action).bounds(x,y,Math.max(1,w),h).build());}
    @Override protected void init(){
        w=Math.min(550,width-12);x=(width-w)/2;top=59;bottom=Math.max(top+24,height-32);
        int side=Math.min(92,(w-12)/4);
        search=addRenderableWidget(new EditBox(font,x+4,32,w-2*side-16,20,Component.literal(tr("Module suchen","Search modules"))));
        search.setMaxLength(80);search.setValue(query);search.setHint(Component.literal(tr("Module suchen...","Search modules...")));
        filter=button(favorites?tr("Favoriten","Favorites"):tr("Alle","All"),x+w-2*side-8,32,side,20,b->{favorites=!favorites;page=0;refresh();});
        button(tr("Client","Client"),x+w-side-4,32,side,20,b->open(new EvolutionSettingsScreen(this)));
        int count=Math.max(1,(bottom-top)/23);widgets=new RowButtons[count];
        int starW=24,cfgW=Math.min(72,w/5),toggleW=Math.min(82,Math.max(55,w/4)),nameW=w-starW-cfgW-toggleW-18;
        for(int i=0;i<count;i++){
            final int n=i;int y=top+i*23;
            Button a=button("",x+4,y,nameW,20,b->action(n,0));
            Button t=button("",x+8+nameW,y,toggleW,20,b->action(n,1));
            Button c=button(UiLabels.t("Config"),x+12+nameW+toggleW,y,cfgW,20,b->action(n,2));
            Button f=button("*",x+w-starW-4,y,starW,20,b->action(n,3));
            widgets[i]=new RowButtons(a,t,c,f);
        }
        int fy=height-25, unit=(w-28)/6;
        prev=button("<",x+4,fy,unit,20,b->{page--;refresh();});
        next=button(">",x+8+unit,fy,unit,20,b->{page++;refresh();});
        button(tr("Profile","Profiles"),x+12+2*unit,fy,unit,20,b->open(new ServerProfileScreen()));
        button(quick?tr("Alle","All"):tr("Schnell","Quick"),x+16+3*unit,fy,unit-2,20,b->open(new BenchScreen(!quick)));
        button(UiLabels.t("Research"),x+20+4*unit,fy,unit,20,b->open(new ResearchScreen(this)));
        button(tr("Zur\u00FCck","Back"),x+w-unit-4,fy,unit,20,b->onClose());
        search.setResponder(s->{query=s;page=0;refresh();});refresh();
    }
    private boolean matches(ModuleCatalog.Group g,ModuleCatalog.Module m,String q){return q.isBlank()||(g.en()+" "+g.de()+" "+m.en()+" "+m.de()+" "+m.id()).toLowerCase(Locale.ROOT).contains(q);}
    private void refresh(){
        var list=new ArrayList<Row>();String q=query.trim().toLowerCase(Locale.ROOT);
        for(var g:ModuleCatalog.GROUPS){
            var children=ModuleCatalog.MODULES.stream().filter(m->m.group().equals(g.id())&&(!favorites||EvolutionUi.favorite(m.id()))&&matches(g,m,q)).toList();
            if(children.isEmpty()&&(favorites||!q.isBlank()))continue;
            list.add(new Row(g,null));
            if(favorites||!q.isBlank()||expanded.contains(g.id()))for(var m:children)list.add(new Row(g,m));
        }
        data=List.copyOf(list);page=Math.max(0,Math.min(page,Math.max(0,(data.size()-1)/widgets.length)));
        filter.setMessage(Component.literal(favorites?tr("Favoriten","Favorites"):tr("Alle","All")));
        for(int i=0;i<widgets.length;i++){
            var b=widgets[i];int k=page*widgets.length+i;boolean present=k<data.size();
            b.name.visible=b.toggle.visible=b.config.visible=b.star.visible=present;
            if(!present)continue;
            var row=data.get(k);var g=row.group();var m=row.module();
            if(m==null){
                String name=(expanded.contains(g.id())||favorites||!q.isBlank()?"- ":"+ ")+g.name();
                b.name.setMessage(Component.literal(font.plainSubstrByWidth(name,b.name.getWidth()-8)));
                b.name.setTooltip(Tooltip.create(Component.literal(g.name())));
                b.toggle.setMessage(Component.literal((b.toggle.getWidth()>=68?UiLabels.t("MASTER "):"M: ")+(g.on().getAsBoolean()?UiLabels.t("ON"):UiLabels.t("OFF"))));
                b.toggle.setTooltip(Tooltip.create(Component.literal(tr("Master pausiert alle Module.","Master pauses all child modules."))));
                b.config.visible=g.advanced()!=null;b.config.active=g.advanced()!=null;b.star.visible=false;
            }else{
                b.name.setMessage(Component.literal(font.plainSubstrByWidth("  "+m.name(),b.name.getWidth()-8)));
                b.name.setTooltip(Tooltip.create(Component.literal(m.description())));
                b.toggle.setMessage(Component.literal(ModuleCatalog.state(m)));
                b.toggle.setTooltip(Tooltip.create(Component.literal(tr("Modul umschalten","Toggle module"))));
                b.config.active=true;b.star.visible=true;b.star.active=true;
                b.star.setMessage(Component.literal(EvolutionUi.favorite(m.id())?"\u2605":"\u2606"));
                b.star.setTooltip(Tooltip.create(Component.literal(tr("Favorit umschalten","Toggle favorite"))));
            }
        }
        prev.active=page>0;next.active=(page+1)*widgets.length<data.size();
    }
    private void action(int i,int kind){
        int k=page*widgets.length+i;if(k>=data.size())return;var row=data.get(k);var m=row.module();var g=row.group();
        if(m==null){
            if(kind==0){if(!expanded.remove(g.id()))expanded.add(g.id());}
            if(kind==1)ModuleCatalog.toggle(g);
            if(kind==2&&g.advanced()!=null){open(g.advanced().get());return;}
        }else{
            if(kind==0||kind==2){open(new ModuleConfigScreen(this,m));return;}
            if(kind==1)ModuleCatalog.toggle(m);
            if(kind==3)EvolutionUi.toggleFavorite(m.id());
        }refresh();
    }
    @Override public boolean mouseScrolled(double mx,double my,double sx,double sy){if(my>=top&&my<bottom&&sy!=0){page+=sy>0?-1:1;refresh();return true;}return super.mouseScrolled(mx,my,sx,sy);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float p){
        g.fill(x,4,x+w,height-3,EvolutionUi.panel());g.fill(x,4,x+w,6,EvolutionUi.accent());
        g.text(font,quick?UiLabels.t("WUKONG / QUICK MENU"):"WUKONGCLIENT / "+EvolutionUi.installed(),x+6,10,EvolutionUi.accent(),true);
        String active=font.plainSubstrByWidth(ServerProfiles.activeLabel(),w-12);g.text(font,active,x+6,20,0xFFB7C7D2,false);
        if(data.isEmpty())g.text(font,tr("Keine Module gefunden.","No matching modules."),x+8,top+4,0xFFB7C7D2,true);
        String pText=(page+1)+" / "+Math.max(1,(data.size()+widgets.length-1)/widgets.length);
        g.text(font,pText,x+6,height-36,0xFFB7C7D2,true);super.extractRenderState(g,mx,my,p);
    }
    @Override public void onClose(){BenchConfig.INSTANCE.save();if(quick)open(new BenchScreen());else super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
