package dev.wukong.client;

import dev.wukong.client.research.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class ResearchScreen extends Screen {
    private final Screen parent;
    private int x,w,tab,page,view;
    private EditBox search;
    private Button viewButton;
    private String query="";
    private static final SyntheticLaboratory LAB=new SyntheticLaboratory();
    private SyntheticLaboratory.Suite suite=SyntheticLaboratory.Suite.ALL;
    private static final int[] TRIALS={20,100,500,1000};
    private int trialIndex=1;
    private String seedText="42",labMessage="";
    private EditBox seedBox;
    private Button suiteButton,trialButton,runButton,exportButton;
    private static final int SCENARIOS=7, REPORTS=8;
    private final ReportStudio studio=new ReportStudio(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("wukong-research-reports"));
    private ReportStudio.Index reportIndex=new ReportStudio.Index(List.of(),0,false);
    private ReportStudio.Document selectedReport;
    private int reportSelection;
    private List<Line> reportRows=List.of();
    private int cachedReportWidth=-1;
    private String reportMessage="";
    private Button[] reportButtons;
    private Button reportViewButton;
    private boolean reportDetails;
    private java.nio.file.Path lastSaved;
    private void archiveCurrent(){
        try{recordSaved(studio.save(LAB.current().orElseThrow()), UiLabels.t("Manual export"));}
        catch(java.io.IOException|RuntimeException e){labMessage=error(UiLabels.t("Save failed"),e);}
    }
    private static String error(String prefix,Exception e){return prefix+": "+e.getClass().getSimpleName()+": "+Objects.toString(e.getMessage(),UiLabels.t("No details"));}
    private void recordSaved(java.nio.file.Path path,String operation)throws java.io.IOException {
        lastSaved=path;labMessage=operation+UiLabels.t(": saved and verified (")+java.nio.file.Files.size(path)+UiLabels.t(" bytes)");
    }
    private void cancelRun(){
        try{var saved=ReportActions.cancel(LAB,studio);if(saved.isPresent())recordSaved(saved.get(),UiLabels.t("Auto-save CANCELLED"));}
        catch(java.io.IOException|RuntimeException e){labMessage=error(UiLabels.t("Auto-save CANCELLED failed"),e);}
    }
    private void refreshReports(){
        try{reportIndex=studio.index();reportSelection=0;reportMessage="";loadSelected();}
        catch(java.io.IOException|RuntimeException e){reportIndex=new ReportStudio.Index(List.of(),0,false);selectedReport=null;reportMessage=error(UiLabels.t("Refresh failed"),e);}
        page=0;
    }
    private java.nio.file.Path selectedPath(){return reportIndex.entries().get(reportSelection).path();}
    private void loadSelected(){
        selectedReport=null;reportRows=List.of();cachedReportWidth=-1;page=0;
        if(reportIndex.entries().isEmpty())return;
        try{selectedReport=studio.read(selectedPath());}
        catch(java.io.IOException|RuntimeException e){reportMessage=error(UiLabels.t("Read failed"),e);}
    }
    private void shiftReport(int delta){if(!reportIndex.entries().isEmpty()){reportSelection=Math.floorMod(reportSelection+delta,reportIndex.entries().size());reportMessage="";loadSelected();}}
    private void openPath(java.nio.file.Path path){
        try{ReportActions.open(path);reportMessage=UiLabels.t("Open requested: ")+path;page=0;}
        catch(java.io.IOException|RuntimeException e){reportMessage=error(UiLabels.t("Open failed"),e);}
    }
    private void copyPath(){
        try {
            var path = reportIndex.entries().isEmpty()
                ? studio.directory() : selectedPath();

            String value = java.util.Base64.getEncoder()
                .encodeToString(path.toString().getBytes(
                    java.nio.charset.StandardCharsets.UTF_8));

            String command =
                "$ErrorActionPreference='Stop'; "
                + "$v=[Text.Encoding]::UTF8.GetString("
                + "[Convert]::FromBase64String('" + value + "')); "
                + "Set-Clipboard -Value $v; "
                + "if ((Get-Clipboard -Raw) -cne $v) "
                + "{ throw 'Clipboard verification failed' }";

            String encoded = java.util.Base64.getEncoder()
                .encodeToString(command.getBytes(
                    java.nio.charset.StandardCharsets.UTF_16LE));

            Process process = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-STA",
                "-EncodedCommand",
                encoded
            ).redirectErrorStream(true)
             .redirectOutput(ProcessBuilder.Redirect.DISCARD)
             .start();

            if (!process.waitFor(
                    5, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new java.io.IOException("Clipboard timeout");
            }

            if (process.exitValue() != 0) {
                throw new java.io.IOException(
                    "Clipboard exit code: " + process.exitValue());
            }

            reportMessage = UiLabels.t("Copied and verified: ") + path;
            page = 0;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            reportMessage = error(UiLabels.t("Copy interrupted"), e);
        } catch (java.io.IOException | RuntimeException e) {
            reportMessage = error(UiLabels.t("Copy failed"), e);
        }
    }
    private static final String[] VIEWS={"Types", "Variants", "Slots", "Container", "Slot history"};
    private Button monitor, previous, next;
    private final Button[] tabs=new Button[9];
    private List<ConservationLab.Result> results=List.of();
    private List<DetectionFixtures.Result> detectionResults=List.of();
    private static final String[] TABS={"Overview","Monitor","Library","Test Lab","Logs","Settings","Findings","Scenarios","Reports"};
    private static final int TEXT=0xFFE0E9F1, MUTED=0xFF9FAFBE, TILE=0xFF202C38;
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    private record Line(String text, String item, int color, net.minecraft.world.item.ItemStack stack) {
        Line(String text,String item,int color){this(text,item,color,null);}
    }
    public ResearchScreen(Screen parent){super(Component.literal(UiLabels.t("WuKong / Research Lab")));this.parent=parent;}
    private Button btn(String name,int a,int b,int c,Button.OnPress fn){return addRenderableWidget(Button.builder(Component.literal(name),fn).bounds(a,b,Math.max(1,c),20).build());}
    @Override protected void init(){
        w=Math.min(560,width-12);x=(width-w)/2;int unit=(w-12)/5;
        for(int i=0;i<TABS.length;i++){final int n=i;tabs[i]=btn(UiLabels.t(TABS[i]),x+4+(i%5)*unit,28+(i/5)*22,unit-3,b->{tab=n;page=0;if(n==REPORTS)refreshReports();});}
        viewButton=btn(UiLabels.t("View: Types"),x+4,76,Math.max(60,w/3),b->{view=(view+1)%VIEWS.length;page=0;});
        search=addRenderableWidget(new EditBox(font,x+10+w/3,76,Math.max(1,w-14-w/3),20,Component.literal(UiLabels.t("Filter items"))));
        search.setMaxLength(80);search.setValue(query);search.setHint(Component.literal(UiLabels.t("Filter by name, mod, ID or slot")));
        search.setResponder(v->{query=v;page=0;});
        viewButton.visible=tab==1;search.visible=tab==1;
        int labWidth=(w-16)/3;
        suiteButton=btn(UiLabels.t("Suite"),x+4,76,labWidth,b->{var a=SyntheticLaboratory.Suite.values();suite=a[(suite.ordinal()+1)%a.length];page=0;});
        trialButton=btn(UiLabels.t("Trials"),x+8+labWidth,76,labWidth,b->{trialIndex=(trialIndex+1)%TRIALS.length;});
        seedBox=addRenderableWidget(new EditBox(font,x+12+2*labWidth,76,labWidth,20,Component.literal(UiLabels.t("Seed"))));
        seedBox.setMaxLength(20);seedBox.setValue(seedText);seedBox.setHint(Component.literal(UiLabels.t("Seed")));seedBox.setResponder(v->seedText=v);
        runButton=btn(UiLabels.t("Run scenarios"),x+4,98,(w-12)/2,b->{
            if(LAB.running()){cancelRun();return;}
            try{LAB.start(new SyntheticLaboratory.Config(Long.parseLong(seedText.strip()),suite,TRIALS[trialIndex]));labMessage="";lastSaved=null;page=0;}
            catch(IllegalArgumentException e){labMessage=UiLabels.t("Invalid seed: use a signed 64-bit integer.");}
        });
        exportButton=btn(UiLabels.t("Export CSV"),x+8+(w-12)/2,98,(w-12)/2,b->{
            archiveCurrent();page=0;
        });
        int rw=(w-16)/3;
        reportButtons=new Button[]{
            btn(UiLabels.t("Refresh"),x+4,76,rw,b->refreshReports()),
            btn(UiLabels.t("Prev report"),x+8+rw,76,rw,b->shiftReport(-1)),
            btn(UiLabels.t("Next report"),x+12+2*rw,76,rw,b->shiftReport(1)),
            btn(UiLabels.t("Open folder"),x+4,98,rw,b->{try{openPath(studio.ensureDirectory());}catch(java.io.IOException|RuntimeException e){reportMessage=error(UiLabels.t("Folder failed"),e);}}),
            btn(UiLabels.t("Open CSV"),x+8+rw,98,rw,b->{if(!reportIndex.entries().isEmpty())openPath(selectedPath());}),
            btn(UiLabels.t("Copy path"),x+12+2*rw,98,rw,b->copyPath()),
            btn(UiLabels.t("Export JSON"),x+4,120,rw,b->{try{var file=studio.exportJson(selectedPath());reportMessage=UiLabels.t("Saved: ")+file+" ("+java.nio.file.Files.size(file)+UiLabels.t(" bytes)");}catch(java.io.IOException|RuntimeException e){reportMessage=error(UiLabels.t("JSON failed"),e);}}),
            btn(UiLabels.t("Save session"),x+8+rw,120,rw,b->{try{var file=studio.saveSession(ResearchRuntime.ledger(),ResearchRuntime.detection());reportMessage=UiLabels.t("Saved: ")+file;}catch(java.io.IOException|RuntimeException e){reportMessage=error(UiLabels.t("Session failed"),e);}}),
            btn(UiLabels.t("Compare next"),x+12+2*rw,120,rw,b->compareReport())
        };
        reportViewButton=btn(UiLabels.t("Results"),x+4+4*unit,50,unit-3,b->{reportDetails=!reportDetails;page=0;});
        updateReportWidgets();
        updateLabWidgets();
        int bw=(w-16)/3;
        monitor=btn(UiLabels.t("Monitor: OFF"),x+4,height-47,bw,b->{BenchConfig.INSTANCE.researchMonitor=!BenchConfig.INSTANCE.researchMonitor;BenchConfig.INSTANCE.save();});
        btn(UiLabels.t("Run fixtures"),x+8+bw,height-47,bw,b->{results=ConservationLab.run();detectionResults=DetectionFixtures.run();tab=3;page=0;});
        btn(UiLabels.t("Clear session"),x+12+2*bw,height-47,bw,b->{ResearchRuntime.reset();page=0;});
        previous=btn("<",x+4,height-24,bw,b->{page=Math.max(0,page-1);});
        next=btn(">",x+8+bw,height-24,bw,b->{page++;});
        btn(UiLabels.t("Back"),x+12+2*bw,height-24,bw,b->onClose());
    }
    private void updateLabWidgets(){
        boolean show=tab==SCENARIOS, busy=LAB.running();
        suiteButton.visible=trialButton.visible=seedBox.visible=runButton.visible=exportButton.visible=show;
        suiteButton.active=trialButton.active=!busy;
        seedBox.setEditable(!busy);
        suiteButton.setMessage(Component.literal(font.plainSubstrByWidth(UiLabels.t(suite),Math.max(1,suiteButton.getWidth()-8))));
        trialButton.setMessage(Component.literal(UiLabels.t("Trials: ")+TRIALS[trialIndex]));
        runButton.setMessage(Component.literal(busy?UiLabels.t("Cancel run"):UiLabels.t("Run scenarios")));
        exportButton.active=!busy&&LAB.current().isPresent();
    }
    private void updateReportWidgets(){
        boolean show=tab==REPORTS;for(var b:reportButtons)b.visible=show;
        reportViewButton.visible=show;reportViewButton.active=selectedReport!=null;reportViewButton.setMessage(Component.literal(reportDetails?UiLabels.t("Summary"):UiLabels.t("Results")));
        boolean any=!reportIndex.entries().isEmpty();reportButtons[1].active=reportButtons[2].active=reportIndex.entries().size()>1;reportButtons[4].active=any;reportButtons[6].active=selectedReport!=null;reportButtons[8].active=selectedReport!=null&&reportIndex.entries().size()>1;
    }
    private void compareReport(){
        try{var other=studio.read(reportIndex.entries().get((reportSelection+1)%reportIndex.entries().size()).path());var a=selectedReport;
            reportMessage=UiLabels.t("Compare next: ")+(a.equals(other)?UiLabels.t("identical parsed results"): UiLabels.t("seed ")+a.seed()+UiLabels.t(" vs ")+other.seed()+UiLabels.t("; passed ")+a.passed()+"/"+a.rows().size()+UiLabels.t(" vs ")+other.passed()+"/"+other.rows().size()+UiLabels.t("; requested ")+a.requested()+UiLabels.t(" vs ")+other.requested()+"; "+UiLabels.t(a.status())+UiLabels.t(" vs ")+UiLabels.t(other.status())+UiLabels.t("; suite ")+UiLabels.t(a.suite())+UiLabels.t(" vs ")+UiLabels.t(other.suite()));page=0;
        }catch(java.io.IOException|RuntimeException e){reportMessage=error(UiLabels.t("Compare failed"),e);}
    }
    @Override public void tick(){super.tick();if(LAB.running()){
        try{var saved=ReportActions.step(LAB,studio,8);if(saved.isPresent()){recordSaved(saved.get(),UiLabels.t("Auto-save COMPLETE"));if(tab==REPORTS)refreshReports();}}
        catch(java.io.IOException|RuntimeException e){labMessage=error(UiLabels.t("Auto-save failed"),e);}
    }}
    private void wrap(List<Line> out,String value){
        String text=value;int max=Math.max(1,w-20);while(!text.isEmpty()){String part=font.plainSubstrByWidth(text,max);if(part.isEmpty())break;add(out,part);text=text.substring(part.length());}
    }
    private void label(GuiGraphicsExtractor g,String s,int a,int b,int max,int color){
        g.text(font,font.plainSubstrByWidth(s,Math.max(1,max)),a,b,color,false);
    }
    private void card(GuiGraphicsExtractor g,int a,int b,int cw,String title,String value){
        g.fill(a,b,a+cw,b+30,TILE);
        label(g,title,a+5,b+4,cw-10,MUTED);
        label(g,value,a+5,b+16,cw-10,EvolutionUi.accent());
    }
    private int overview(GuiGraphicsExtractor g,int top){
        var l=ResearchRuntime.ledger();var snap=l.latest();var stat=ResearchRuntime.metrics().summary();
        int cw=(w-20)/3;
        card(g,x+6,top,cw,UiLabels.t("Samples"),Long.toString(l.samples()));
        card(g,x+8+cw,top,cw,UiLabels.t("Items / main"),snap==null?"--":Long.toString(snap.total()));
        card(g,x+10+2*cw,top,cw,UiLabels.t("Reported ping"),stat.map(s->s.latest()+" ms").orElse("--"));
        int y=top+35;
        if(height>=260){
            int left=x+8,right=x+w-8, bottom=y+43;
            g.fill(left,y,right,bottom,TILE);
            var values=ResearchRuntime.metrics().history();
            label(g,UiLabels.t("PING ESTIMATE / last 120 samples"),left+4,y+3,right-left-8,MUTED);
            if(!values.isEmpty()){
                int peak=ResearchRuntime.metrics().chartCeiling();
                int plotLeft=left+48;
                label(g,peak+" ms",left+4,y+14,44,MUTED);
                label(g,"0",left+4,bottom-10,44,MUTED);
                g.fill(plotLeft,bottom-3,right-4,bottom-2,0xFF455666);
                for(int i=0;i<values.size();i++){
                    int a=plotLeft+(120-values.size()+i)*Math.max(1,right-4-plotLeft)/120;
                    int end=plotLeft+(121-values.size()+i)*Math.max(1,right-4-plotLeft)/120;
                    int h=Math.max(1,(int)(24L*values.get(i)/peak));
                    g.fill(a,bottom-3-h,Math.max(a+1,end-1),bottom-3,EvolutionUi.accent());
                }
            } else label(g,UiLabels.t("Waiting for positive remote latency estimate"),left+4,y+19,right-left-8,MUTED);
            y=bottom+5;
        }
        return y;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        g.fill(x,4,x+w,height-3,EvolutionUi.panel());g.fill(x,4,x+w,6,EvolutionUi.accent());
        label(g,UiLabels.t("WUKONG / RESEARCH LAB")+" "+EvolutionUi.installed(),x+8,12,w-16,EvolutionUi.accent());
        monitor.setMessage(Component.literal("Monitor: "+(BenchConfig.INSTANCE.researchMonitor?UiLabels.t("ON"):UiLabels.t("OFF"))));
        for(int i=0;i<tabs.length;i++)tabs[i].setMessage(Component.literal(i==tab?"[ "+UiLabels.t(TABS[i])+" ]":UiLabels.t(TABS[i])));
        viewButton.visible=tab==1;search.visible=tab==1;
        viewButton.setMessage(Component.literal(UiLabels.t("View: ")+UiLabels.t(VIEWS[view])));
        updateLabWidgets();
        updateReportWidgets();
        int top=tab==REPORTS?145:tab==SCENARIOS?123:tab==1?101:79;
        if(tab==0&&height>=205)top=overview(g,top);
        if(tab==REPORTS&&!reportDetails&&selectedReport!=null&&height>=270){
            var d=selectedReport;int left=x+8,right=x+w-8,span=right-left;
            g.fill(left,top,right,top+6,TILE);
            int passed=(int)(span*d.passed()/d.requested()),executed=(int)((long)span*d.rows().size()/d.requested());
            if(passed>0)g.fill(left,top,left+passed,top+6,0xFF77D9A5);
            if(executed>passed)g.fill(left+passed,top,left+executed,top+6,0xFFFF8888);
            top+=11;
        }
        List<Line> lines=lines();int step=tab==1?20:12;
        int rows=Math.max(0,(height-66-top)/step);
        int pages=rows==0?1:Math.max(1,(lines.size()+rows-1)/rows);
        page=Math.min(page,pages-1);
        previous.active=page>0;next.active=page+1<pages;
        for(int i=page*rows;i<Math.min(lines.size(),(page+1)*rows);i++){
            var line=lines.get(i);int y=top+(i-page*rows)*step;
            if((i%2)==1)g.fill(x+5,y,x+w-5,y+step,0x182F4356);
            int offset=0;
            if(line.item()!=null||line.stack()!=null){var stack=line.stack()!=null?line.stack():ResearchRuntime.icon(line.item());if(!stack.isEmpty()){g.item(stack,x+8,y+1);offset=22;}}
            label(g,line.text(),x+8+offset,y+(step==20?6:2),w-16-offset,line.color());
        }
        label(g,UiLabels.t("Page ")+(page+1)+" / "+pages+(tab==SCENARIOS||tab==REPORTS?UiLabels.t("  |  Local reports"):UiLabels.t("  |  Client observations only")),x+8,height-61,w-16,MUTED);
        super.extractRenderState(g,mx,my,partial);
    }
    private static void add(List<Line> out,String text){out.add(new Line(text,null,TEXT));}
    private static void head(List<Line> out,String text){out.add(new Line(text,null,EvolutionUi.accent()));}
    private List<Line> lines(){
        var out=new ArrayList<Line>();var l=ResearchRuntime.ledger();var s=l.latest();
        switch(tab){
            case 0 -> {
                head(out,UiLabels.t("SESSION INTELLIGENCE / READ ONLY"));
                add(out,UiLabels.t("Target: Minecraft ")+CaseLibrary.TARGET);
                add(out,UiLabels.t("Profile: ")+ServerProfiles.activeLabel());
                add(out,UiLabels.t("Samples: ")+l.samples()+UiLabels.t(" | changes: ")+l.changed());
                add(out,"Monitor: "+(BenchConfig.INSTANCE.researchMonitor?UiLabels.t("Enabled"):UiLabels.t("Disabled")));
                ResearchRuntime.metrics().summary().ifPresent(m->{add(out,String.format(Locale.ROOT,UiLabels.t("Ping min / avg / max: %d / %.0f / %d ms"),m.min(),m.average(),m.max()));});
                add(out,UiLabels.t("Historical cases: ")+CaseLibrary.historical().size());
                add(out,UiLabels.t("Local findings: ")+ResearchRuntime.detection().active().size()+UiLabels.t(" active | ")+ResearchRuntime.detection().history().size()+UiLabels.t(" closed"));
                add(out,UiLabels.t("Server verification: unavailable"));
            }
            case 1 -> {
                head(out,UiLabels.t("INVENTORY INTELLIGENCE / ")+UiLabels.t(VIEWS[view]).toUpperCase(Locale.ROOT));
                if(s==null){add(out,UiLabels.t("No active sample"));break;}
                add(out,UiLabels.t("Main: ")+s.total()+UiLabels.t(" items | ")+s.occupied()+UiLabels.t("/36 slots"));
                if(view==0){
                    s.counts().forEach((id,n)->{
                        var stack=ResearchRuntime.icon(id);String name=stack.isEmpty()?id:stack.getHoverName().getString();
                        if(matches(name+" "+id))out.add(new Line(n+" x "+name,id,TEXT));
                    });
                }else if(view==1){
                    add(out,UiLabels.t("Exact item + component groups (main inventory only)"));
                    int i=0;for(var r:ResearchRuntime.groups())itemRow(out,r,UiLabels.t("Group ")+(++i));
                }else if(view==2){
                    add(out,UiLabels.t("Occupied slots, equipment and cursor; separate from main total"));
                    for(var r:ResearchRuntime.slots())itemRow(out,r,r.location());
                }else if(view==3){
                    if(ResearchRuntime.menuName().isEmpty())add(out,UiLabels.t("Open a container during monitoring first."));
                    else{
                        head(out,(ResearchRuntime.menuOpen()?UiLabels.t("OPEN: "):UiLabels.t("LAST OBSERVED (closed): "))+ResearchRuntime.menuName());
                        add(out,UiLabels.t("Visible non-player menu slots only; may include output/virtual slots"));
                        if(ResearchRuntime.menuTruncated())add(out,UiLabels.t("Partial view: limited to first 256 menu slots"));
                        if(ResearchRuntime.menuRows().isEmpty())add(out,UiLabels.t("No occupied non-player slots observed"));
                        for(var r:ResearchRuntime.menuRows())itemRow(out,r,r.location());
                    }
                }else{
                    add(out,UiLabels.t("Main inventory only | discarded batches: ")+ResearchRuntime.slotHistory().discarded());
                    for(var b:ResearchRuntime.slotHistory().history().reversed()){
                        var found=b.changes().stream().filter(c->matches(c.slot()+" "+c.before()+" "+c.after())).toList();
                        if(found.isEmpty())continue;
                        head(out,UiLabels.t("Observation #")+b.sample());
                        for(var c:found){add(out,UiLabels.slot(c.slot())+": "+c.before());add(out,"  -> "+c.after());}
                    }
                    if(ResearchRuntime.slotHistory().history().isEmpty())add(out,UiLabels.t("No observed slot changes"));
                }
                add(out,UiLabels.t("Local diagnostic evidence: see Findings tab"));
            }
            case 2 -> {
                head(out,UiLabels.t("HISTORICAL REFERENCES / schema ")+CaseLibrary.SCHEMA);
                add(out,UiLabels.t("Target ")+CaseLibrary.TARGET+UiLabels.t(": no verified active cases"));
                for(var c:CaseLibrary.historical()){
                    head(out,c.id()+" | "+UiLabels.t(c.category()));
                    add(out,UiLabels.t("Fixed: ")+c.fixedRelease()+" | "+UiLabels.t(c.status(CaseLibrary.TARGET)));
                    add(out,UiLabels.t(c.documentedFix()));
                }
                add(out,UiLabels.t("Historical records are not executable tests."));
            }
            case 3 -> {
                head(out,UiLabels.t("SYNTHETIC REGRESSION TESTS / no network"));
                if(results.isEmpty())add(out,UiLabels.t("Select Run fixtures to begin."));
                else add(out,UiLabels.t("Passed: ")+(results.stream().filter(ConservationLab.Result::passed).count()+detectionResults.stream().filter(DetectionFixtures.Result::passed).count())+" / "+(results.size()+detectionResults.size()));
                for(var a:results)out.add(new Line((a.passed()?UiLabels.t("PASS | "):UiLabels.t("FAIL | "))+UiLabels.t(a.name())+" | "+UiLabels.t(a.actual()),null,a.passed()?0xFF77D9A5:0xFFFF8888));
                if(!detectionResults.isEmpty())head(out,UiLabels.t("LOCAL DETECTION FIXTURES"));
                for(var a:detectionResults)out.add(new Line((a.passed()?UiLabels.t("PASS | "):UiLabels.t("FAIL | "))+UiLabels.t(a.name()),null,a.passed()?0xFF77D9A5:0xFFFF8888));
            }
            case 4 -> {
                head(out,UiLabels.t("SESSION EVENTS / newest first | discarded: ")+l.dropped());
                for(var e:l.events().reversed()){
                    head(out,"#"+e.sequence()+" "+TIME.format(e.recordedAt())+" | "+UiLabels.t(e.kind()));
                    add(out,UiLabels.t(e.detail()));
                    e.differences().forEach((id,delta)->add(out,(delta>0?"+":"")+delta+"  "+id));
                }
                if(l.events().isEmpty())add(out,UiLabels.t("No recorded events"));
            }
            case 5 -> {
                head(out,UiLabels.t("SETTINGS / per-server monitor preference"));
                add(out,UiLabels.t("Sampling: every 10 client ticks"));
                add(out,UiLabels.t("Latency: vanilla reported estimate, not a packet probe"));
                add(out,UiLabels.t("Latency window: 120 samples; clears when paused"));
                add(out,UiLabels.t("History: 256 events; resets on world/player change"));
                add(out,UiLabels.t("Totals: registry IDs; Variants: exact item/component equality"));
                add(out,UiLabels.t("Slot history: 64 batches, main inventory only"));
                add(out,UiLabels.t("Remote ping: zero and missing values unavailable"));
                add(out,UiLabels.t("Container: last sampled visible menu, not server storage"));
                add(out,UiLabels.t("Nested: component entry counts; custom storage may be hidden"));
                add(out,UiLabels.t("Keybinds: Research Lab / Research Monitor"));
                add(out,UiLabels.t("Findings: 128 closed episodes; active evidence grouped by category"));
                add(out,UiLabels.t("Ordinary count changes are not findings"));
                add(out,UiLabels.t("Completed/cancelled synthetic runs auto-save locally."));
                add(out,UiLabels.t("Save session: explicit aggregate event and finding summary."));
                add(out,UiLabels.t("No packet injection or item mutations"));
            }
            case SCENARIOS -> {
                head(out,UiLabels.t("SYNTHETIC SCENARIO LAB / NO NETWORK"));
                if(!labMessage.isEmpty())wrap(out,labMessage);
                var current=LAB.current();
                if(current.isEmpty()){
                    add(out,UiLabels.t("Choose a suite, trial count and seed; press Run scenarios."));
                    add(out,UiLabels.t("Fixture schema ")+SyntheticLaboratory.SCHEMA+UiLabels.t(" | target ")+CaseLibrary.TARGET+UiLabels.t(" (not server validation)"));
                    add(out,UiLabels.t("Seed: signed 64-bit integer | suite: ")+UiLabels.t(suite));
                }
                else {
                    var r=current.get();
                    head(out,UiLabels.t(r.status())+" | "+r.results().size()+" / "+r.config().trials());
                    add(out,UiLabels.t("Passed: ")+r.passed()+UiLabels.t(" | failed: ")+r.failed());
                    add(out,UiLabels.t("Seed: ")+r.config().seed()+" | "+UiLabels.t(r.config().suite()));
                    add(out,UiLabels.t("Fixture schema ")+SyntheticLaboratory.SCHEMA+UiLabels.t(" | target ")+CaseLibrary.TARGET+UiLabels.t(" (not server validation)"));
                    wrap(out,UiLabels.t("Report folder: ")+studio.directory());
                    if(lastSaved!=null)wrap(out,UiLabels.t("Saved report: ")+lastSaved);
                    head(out,UiLabels.t("RECENT RUNS / in memory"));
                    for(var old:LAB.history().reversed())add(out,UiLabels.t(old.status())+UiLabels.t(" | seed ")+old.config().seed()+" | "+old.passed()+UiLabels.t(" passed | ")+old.results().size()+" / "+old.config().trials());
                    head(out,UiLabels.t("TRIAL RESULTS / expected vs actual"));
                    // Show failures first; retain all trial rows in the CSV.
                    var ordered=new ArrayList<>(r.results());ordered.sort(Comparator.comparing(SyntheticLaboratory.Result::passed));
                    for(var result:ordered){
                        out.add(new Line((result.passed()?UiLabels.t("PASS"):UiLabels.t("FAIL"))+" #"+result.trial()+" | "+UiLabels.t(result.suite()),null,result.passed()?0xFF77D9A5:0xFFFF8888));
                        if(!result.passed()){add(out,UiLabels.t("Input: ")+result.input());for(String value:result.expected().split(";"))add(out,UiLabels.t("Expected: ")+value);for(String value:result.actual().split(";"))add(out,UiLabels.t("Actual: ")+value);}
                    }
                }
            }
            case REPORTS -> {
                if(reportDetails&&selectedReport!=null){
                    var d=selectedReport;
                    head(out,UiLabels.t("CSV RESULTS / EXPECTED VS ACTUAL"));
                    add(out,UiLabels.t("Seed ")+d.seed()+" | "+UiLabels.t(d.status())+" | "+d.passed()+"/"+d.rows().size()+UiLabels.t(" passed"));
                    if(!reportMessage.isEmpty())wrap(out,reportMessage);
                    if(d.rows().isEmpty())add(out,UiLabels.t("No executed trials in this report."));
                    if(cachedReportWidth!=w){
                        var rows=new ArrayList<Line>();
                        for(var r:d.rows()){
                            boolean pass=r.get(8).equals("true");rows.add(new Line((pass?UiLabels.t("PASS"):UiLabels.t("FAIL"))+" #"+r.get(6)+" | "+UiLabels.t(r.get(7)),null,pass?0xFF77D9A5:0xFFFF8888));
                            wrap(rows,UiLabels.t("Input: ")+r.get(9));wrap(rows,UiLabels.t("Expected: ")+r.get(10));wrap(rows,UiLabels.t("Actual: ")+r.get(11));
                        }
                        reportRows=List.copyOf(rows);cachedReportWidth=w;
                    }
                    out.addAll(reportRows);break;
                }
                head(out,UiLabels.t("REPORT STUDIO / LOCAL ARCHIVE"));
                wrap(out,UiLabels.t("Directory: ")+studio.directory());
                if(!reportMessage.isEmpty())wrap(out,reportMessage);
                add(out,UiLabels.t("CSV files: ")+reportIndex.entries().size()+UiLabels.t(" | skipped: ")+reportIndex.skipped()+UiLabels.t(" | duplicates: ")+reportIndex.entries().stream().filter(ReportStudio.Entry::duplicate).count());
                if(reportIndex.capped())add(out,UiLabels.t("Index limited to 256 inspected files. Other files remain on disk."));
                if(reportIndex.entries().isEmpty()){add(out,UiLabels.t("No archived reports. Finish a synthetic run or refresh."));break;}
                var entry=reportIndex.entries().get(reportSelection);
                head(out,UiLabels.t("Selected ")+(reportSelection+1)+" / "+reportIndex.entries().size()+(entry.duplicate()?UiLabels.t(" | DUPLICATE CONTENT"):""));
                wrap(out,entry.path().getFileName().toString());add(out,entry.bytes()+UiLabels.t(" bytes | SHA-256: ")+entry.digest().substring(0,12));
                if(selectedReport==null){add(out,UiLabels.t("Selected CSV could not be parsed."));break;}
                var d=selectedReport;
                add(out,"Schema "+d.schema()+UiLabels.t(" | target ")+d.target()+" | "+UiLabels.t(d.status()));
                add(out,UiLabels.t("Seed ")+d.seed()+UiLabels.t(" | suite ")+UiLabels.t(d.suite()));
                add(out,UiLabels.t("Executed ")+d.rows().size()+" / "+d.requested()+UiLabels.t(" | passed ")+d.passed()+UiLabels.t(" | failed ")+d.failed());
                add(out,UiLabels.t("Select Results beside Reports to inspect Expected / Actual."));
            }
            case 6 -> {
                var d=ResearchRuntime.detection();
                head(out,UiLabels.t("LOCAL FINDINGS / CLIENT EVIDENCE ONLY"));
                add(out,UiLabels.t("Checks: ")+d.samples()+UiLabels.t(" | no local inconsistency: ")+d.consistentSamples());
                add(out,UiLabels.t("Active: ")+d.active().size()+UiLabels.t(" | closed: ")+d.history().size()+UiLabels.t(" | discarded: ")+d.discarded());
                add(out,UiLabels.t("Count changes alone do not create findings."));
                add(out,UiLabels.t("Server-side verification: unavailable"));
                if(d.active().isEmpty())add(out,UiLabels.t("No active local findings"));
                for(var f:d.active())finding(out,f);
                if(!d.history().isEmpty())head(out,UiLabels.t("CLOSED EPISODES / newest first"));
                for(var f:d.history().reversed())finding(out,f);
            }
        }return out;
    }
    private static void finding(List<Line> out, DetectionEngine.Finding f){
        int color=switch(f.state()){
            case SINGLE_OBSERVATION,REPEATED_OBSERVATION -> 0xFFE0B56B;
            case CLEARED,INTERRUPTED -> MUTED;
        };
        out.add(new Line(UiLabels.t(f.code())+" | "+UiLabels.t(f.state()),null,color));
        add(out,UiLabels.t("Observations: ")+f.observations()+UiLabels.t(" | peak affected: ")+f.peakAffected());
        add(out,UiLabels.t("Samples: ")+f.firstSample()+" - "+f.lastSample());
        // Two short lines to avoid clipping the qualification on small screens.
        if(f.code()==DetectionEngine.Code.REPORTED_STACK_LIMIT){
            add(out,UiLabels.t("Count above local reported limit; custom rules possible."));
        } else add(out,UiLabels.t("Local views disagree; capture quality issue."));
        add(out,UiLabels.t("Not evidence of a server-side duplication."));
    }
    private boolean matches(String text){return text.toLowerCase(Locale.ROOT).contains(query.strip().toLowerCase(Locale.ROOT));}
    private void itemRow(List<Line> out,ResearchRuntime.ItemRow r,String prefix){
        String name=r.stack().getHoverName().getString();
        if(!matches(prefix+" "+r.location()+" "+r.id()+" "+name))return;
        out.add(new Line(UiLabels.slot(prefix)+" | "+r.count()+" x "+name,null,TEXT,r.stack()));
        if(!r.nested().isEmpty())add(out,"  "+UiLabels.nested(r.nested()));
    }
    @Override public void onClose(){cancelRun();Minecraft.getInstance().gui.setScreen(parent);}
    @Override public void removed(){cancelRun();super.removed();}
    @Override public boolean isPauseScreen(){return false;}
}
