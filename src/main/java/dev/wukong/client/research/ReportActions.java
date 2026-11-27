package dev.wukong.client.research;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Local report actions. No inventory or network access. */
public final class ReportActions {
    private ReportActions() {}
    public static Optional<Path> step(SyntheticLaboratory lab, ReportStudio studio, int budget)throws IOException {
        if(!lab.running())return Optional.empty();
        lab.step(budget);
        return lab.running()?Optional.empty():Optional.of(studio.save(lab.current().orElseThrow()));
    }
    public static Optional<Path> cancel(SyntheticLaboratory lab, ReportStudio studio)throws IOException {
        if(!lab.running())return Optional.empty();
        lab.cancel();return Optional.of(studio.save(lab.current().orElseThrow()));
    }
    static List<String> windowsCommand(Path path) {
        return List.of("explorer.exe", path.toAbsolutePath().normalize().toString());
    }
    public static void open(Path path)throws IOException {
        Path target=path.toAbsolutePath().normalize();
        if(!Files.isDirectory(target)&&!Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS))throw new IOException("Path does not exist or is not a regular file: "+target);
        if(System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")){
            new ProcessBuilder(windowsCommand(target)).start();
            return;
        }
        if(!java.awt.Desktop.isDesktopSupported()||!java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN))throw new IOException("Desktop open unavailable; use Copy path");
        java.awt.Desktop.getDesktop().open(target.toFile());
    }
}
