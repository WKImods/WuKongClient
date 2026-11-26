package dev.wukong.client;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.*;

/** Named settings templates; world coordinates are never copied between worlds. */
final class ProfileLibrary {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    static Path folder(boolean exports) throws java.io.IOException {
        return Files.createDirectories(FabricLoader.getInstance().getConfigDir().resolve("wukong").resolve(exports ? "profile-exports" : "profile-library"));
    }
    static Path file(String name, boolean exports) throws java.io.IOException {
        if (!name.matches("[A-Za-z0-9][A-Za-z0-9 _-]{0,39}") || name.endsWith(" ")) throw new IllegalArgumentException("Invalid name (1-40 letters, digits, spaces, _ or -)");
        return folder(exports).resolve(name + ".json");
    }
    static List<String> names() throws java.io.IOException {
        try (var stream = Files.list(folder(false))) {
            return stream.filter(f -> Files.isRegularFile(f, LinkOption.NOFOLLOW_LINKS)).map(f -> f.getFileName().toString()).filter(n -> n.endsWith(".json")).map(n -> n.substring(0,n.length()-5)).sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
    }
    static JsonObject read(String name, boolean exports) throws java.io.IOException {
        Path path = file(name, exports);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > 1048576) throw new IllegalArgumentException("Missing or oversized profile");
        JsonObject data = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        JsonObject clean = new JsonObject();
        JsonObject defaults = ServerProfiles.conservativeProfile();
        for (var entry : data.entrySet()) {
            String key = entry.getKey(); JsonElement value = entry.getValue(); JsonElement expected = defaults.get(key);
            if (key.equals("waypoints") || key.equals("lastDeath") || key.equals("appliedProfileName") || expected == null || expected.isJsonNull()) continue;
            if (value.isJsonPrimitive() && expected.isJsonPrimitive()) {
                JsonPrimitive v=value.getAsJsonPrimitive(), e=expected.getAsJsonPrimitive();
                if (v.isBoolean() != e.isBoolean() || v.isNumber() != e.isNumber() || v.isString() != e.isString()) throw new IllegalArgumentException("Invalid setting: " + key);
                // Import only booleans; preserve numeric/string tuning from local defaults.
                if (!exports || v.isBoolean()) clean.add(key,value.deepCopy());
            } else if (!exports && key.equals("oreTypes") && value.isJsonObject()) {
                for (var item : value.getAsJsonObject().entrySet()) if (!item.getValue().isJsonPrimitive() || !item.getValue().getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Invalid ore setting");
                clean.add(key,value.deepCopy());
            }
        }
        // Local exports carry settings, but imported numeric values are deliberately not trusted.
        return clean;
    }
    static void write(String name, boolean exports, JsonObject data) throws java.io.IOException {
        JsonObject copy=data.deepCopy(); copy.remove("waypoints"); copy.remove("lastDeath"); copy.remove("appliedProfileName");
        Files.writeString(file(name,exports), JSON.toJson(copy), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }
    static void rename(String old, String name) throws java.io.IOException { Files.move(file(old,false),file(name,false)); }
    static void delete(String name) throws java.io.IOException { Files.delete(file(name,false)); }
    private ProfileLibrary() {}
}
