package dev.wukong.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Global UI language. Never stored in per-server gameplay profiles. */
public final class WuKongLanguage {
    private static String current = load();
    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("wukong-ui.json");
    }
    private static String load() {
        try {
            if (Files.isRegularFile(path())) {
                JsonObject obj = JsonParser.parseString(Files.readString(path())).getAsJsonObject();
                return normalize(obj.has("language") ? obj.get("language").getAsString() : "de");
            }
        } catch (Exception e) { System.err.println("[WuKong] UI language read: " + e); }
        return "de";
    }
    private static String normalize(String s) {
        return "en".equalsIgnoreCase(s) ? "en" : "de";
    }
    public static boolean german() { return "de".equals(current); }
    public static String text(String de, String en) { return german() ? de : en; }
    public static void choose(String language) {
        current = normalize(language);
        try {
            Path file = path();
            Files.createDirectories(file.getParent());
            Files.writeString(file, "{\n  \"language\": \"" + current + "\"\n}\n", StandardCharsets.UTF_8);
        } catch (Exception e) { System.err.println("[WuKong] UI language save: " + e); }
    }
    private WuKongLanguage() {}
}
