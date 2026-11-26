package dev.wukong.client;
import java.nio.file.*;
import java.util.*;
/** Preserve unreadable config files before allowing subsequent writes. */
final class ConfigRecovery {
    private static final Set<Path> blocked=new HashSet<>();
    private ConfigRecovery(){}
    static synchronized boolean canWrite(Path p){return !blocked.contains(p.toAbsolutePath().normalize());}
    static synchronized void preserve(Path p,Exception problem){
        p=p.toAbsolutePath().normalize();
        if(!Files.exists(p))return;
        try{
            Path backup=p.resolveSibling(p.getFileName()+".corrupt-"+UUID.randomUUID()+".bak");
            Files.copy(p,backup);
            EvolutionUi.note("Config recovery: backup created for "+p.getFileName());
        }catch(Exception error){
            blocked.add(p);EvolutionUi.note("Config recovery: write blocked for "+p.getFileName());
            System.err.println("[WuKong] Config backup failed: "+error);
        }
    }
}
