package dev.wukong.client;
import com.google.gson.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import java.nio.file.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Global UI preferences, opt-in release lookup and bounded session notifications. */
final class EvolutionUi {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final ArrayDeque<String> notices=new ArrayDeque<>();
    private static final AtomicBoolean checking=new AtomicBoolean();
    private static volatile String updateStatus="Not checked";
    private static boolean autoChecked,uiWriteBlocked;
    static final String[] THEMES={"Cyan","Amber","Violet","Forest"};
    private static final int[] COLORS={0xFF5BDCE7,0xFFFFAD57,0xFFC1A2FF,0xFF85D59A};
    private static final int[] PANELS={0xF0101924,0xF0241B15,0xF01B1728,0xF013241D};
    private static final class Settings {
        String theme="Cyan"; Set<String> favorites=new LinkedHashSet<>(); boolean automaticUpdateCheck=false;
    }
    private static final Settings settings=load();
    private EvolutionUi(){}
    private static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("wukong-evolution-ui.json");}
    private static Settings load(){
        try{if(Files.isRegularFile(path())){
            var s=JSON.fromJson(Files.readString(path()),Settings.class);
            if(s==null)throw new IllegalArgumentException("Empty UI preferences");
            if(s.favorites==null)s.favorites=new LinkedHashSet<>();if(!Arrays.asList(THEMES).contains(s.theme))s.theme=THEMES[0];return s;
        }}catch(Exception e){
            try{Files.copy(path(),path().resolveSibling("wukong-evolution-ui.corrupt-"+UUID.randomUUID()+".bak"));}catch(Exception ignored){uiWriteBlocked=true;}
            System.err.println("[WuKong] UI config read: "+e);
        }return new Settings();
    }
    static void save(){
        if(uiWriteBlocked){note("UI preferences write blocked; original preserved");return;}
        Path dest=path();Path tmp=null;
        try{Files.createDirectories(dest.getParent());tmp=Files.createTempFile(dest.getParent(),"wukong-ui-",".tmp");Files.writeString(tmp,JSON.toJson(settings));
            try{Files.move(tmp,dest,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,dest,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception e){note("Unable to save UI preferences");}finally{if(tmp!=null)try{Files.deleteIfExists(tmp);}catch(Exception ignored){}}
    }
    static String theme(){return settings.theme;}
    static void nextTheme(){int i=Arrays.asList(THEMES).indexOf(settings.theme);settings.theme=THEMES[(i+1)%THEMES.length];save();}
    static int accent(){return COLORS[Math.max(0,Arrays.asList(THEMES).indexOf(settings.theme))];}
    static int panel(){return PANELS[Math.max(0,Arrays.asList(THEMES).indexOf(settings.theme))];}
    static boolean favorite(String id){return settings.favorites.contains(id);}
    static void toggleFavorite(String id){if(!settings.favorites.remove(id))settings.favorites.add(id);save();}
    static boolean autoCheck(){return settings.automaticUpdateCheck;}
    static void toggleAutoCheck(){settings.automaticUpdateCheck=!settings.automaticUpdateCheck;save();}
    static String updateStatus(){return updateStatus;}
    static String installed(){return FabricLoader.getInstance().getModContainer("wukong").map(x->x.getMetadata().getVersion().getFriendlyString()).orElse("unknown");}
    static synchronized void note(String message){notices.addFirst(message);while(notices.size()>40)notices.removeLast();}
    static synchronized List<String> notifications(){return List.copyOf(notices);}
    static synchronized void clearNotifications(){notices.clear();}
    static void checkNow(){
        if(!checking.compareAndSet(false,true))return;
        updateStatus="Checking GitHub...";
        try{
            var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
            var request=HttpRequest.newBuilder(URI.create("https://api.github.com/repos/WKImods/WuKongClient/releases?per_page=10"))
                .timeout(Duration.ofSeconds(12)).header("Accept","application/vnd.github+json").header("User-Agent","WuKongClient").GET().build();
            client.sendAsync(request,HttpResponse.BodyHandlers.ofString()).whenComplete((r,error)->{
                try{
                    if(error!=null)throw new IllegalStateException("Connection failed");
                    if(r.statusCode()!=200)throw new IllegalStateException("GitHub HTTP "+r.statusCode());
                    String tag=null;
                    for(var e:JsonParser.parseString(r.body()).getAsJsonArray()){
                        var o=e.getAsJsonObject();if(!o.get("draft").getAsBoolean()){tag=o.get("tag_name").getAsString();break;}
                    }
                    if(tag==null)updateStatus="No published releases found";
                    else {
                        Integer comparison=VersionOrder.compare(tag,installed());
                        if(comparison==null)updateStatus="Latest published: "+tag+" | Installed: "+installed();
                        else if(comparison>0)updateStatus="Update available: "+tag;
                        else if(comparison==0)updateStatus="Installed release matches "+tag;
                        else updateStatus="Installed version is newer than "+tag;
                    }
                    note(updateStatus);
                }catch(Exception e){updateStatus="Release check failed: "+e.getMessage();note(updateStatus);}finally{checking.set(false);}
            });
        }catch(RuntimeException e){checking.set(false);updateStatus="Release check unavailable";note(updateStatus);}
    }
    static void install(){ClientTickEvents.END_CLIENT_TICK.register(EvolutionUi::tick);}
    private static void tick(Minecraft mc){
        DynamicZoom.tick();
        while(WuKongKeys.QUICK_MENU.consumeClick())if(!WuKongKeys.QUICK_MENU.isUnbound()&&mc.player!=null&&mc.level!=null&&mc.gui.screen()==null)mc.gui.setScreen(new BenchScreen(true));
        if(autoCheck()&&!autoChecked){autoChecked=true;checkNow();}
    }
}
