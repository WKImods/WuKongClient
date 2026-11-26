package dev.wukong.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.storage.LevelResource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Loads the complete WuKong configuration separately for each multiplayer
 * address and each singleplayer world folder. Stable BenchConfig.INSTANCE
 * identity matters: menu screens keep references to that object.
 */
public final class ServerProfiles {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static Context active;
    private static JsonObject defaultSettings;
    private static boolean initialized;

    private record Context(String key, String label, String filename) {}

    public static void install() {
        if (initialized) return;
        // Keep the original wukong.json as an unmodified fallback/template.
        defaultSettings = JSON.toJsonTree(BenchConfig.INSTANCE).getAsJsonObject().deepCopy();
        initialized = true;
        ClientTickEvents.START_CLIENT_TICK.register(ServerProfiles::onTick);
    }

    /** Called from BenchConfig.save(). Null means write the global default file. */
    public static Path activeConfigPath() {
        return active == null ? null : profilesFolder().resolve(active.filename());
    }

    public static String activeLabel() {
        return active == null ? "Global (Hauptmen\u00FC)" : active.label();
    }

    public static String activeFileLabel() {
        return active == null ? "wukong.json" : active.filename();
    }

    /** No network calls; the address is from Minecraft's existing server data. */
    private static Context context(Minecraft mc) {
        if (mc == null || mc.level == null || mc.player == null) return null;
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            Path directory = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
                    .toAbsolutePath().normalize();
            String folder = directory.getFileName() == null ? "world" : directory.getFileName().toString();
            String key = "world|" + directory.toString().toLowerCase(Locale.ROOT);
            return new Context(key, "Singleplayer: " + folder, fileName("world", folder, key));
        }
        ServerData data = mc.getCurrentServer();
        if (data == null || data.ip == null || data.ip.isBlank()) return null;
        String host = data.ip.trim().toLowerCase(Locale.ROOT);
        String key = "server|" + host;
        return new Context(key, "Server: " + data.ip.trim(), fileName("server", host, key));
    }

    private static String fileName(String kind, String hint, String key) {
        String slug = hint.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (slug.isEmpty()) slug = kind;
        if (slug.length() > 40) slug = slug.substring(0, 40);
        return kind + "-" + slug + "-" + shortHash(key) + ".json";
    }

    private static String shortHash(String text) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes, 0, 8);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static Path profilesFolder() {
        return FabricLoader.getInstance().getConfigDir().resolve("wukong").resolve("profiles");
    }

    /** Main-client-thread state transition. Saves previous settings before loading the next. */
    private static void onTick(Minecraft mc) {
        if (!initialized) return;
        Context next;
        try {
            next = context(mc);
        } catch (Exception e) {
            System.err.println("[WuKong] Profile identity unavailable: " + e);
            return;
        }
        if (Objects.equals(active == null ? null : active.key(), next == null ? null : next.key())) return;
        // Save the old server before redirecting BenchConfig.save() to another file.
        if (active != null) BenchConfig.INSTANCE.save();
        else defaultSettings = snapshot();
        if (next == null) {
            active = null;
            apply(defaultSettings);
            System.out.println("[WuKong] Switched to global configuration");
            return;
        }
        try {
            Path destination = profilesFolder().resolve(next.filename());
            JsonObject settings;
            boolean firstUse = !Files.exists(destination);
            if (firstUse) {
                settings = next.key().startsWith("server|") ? conservativeProfile() : freshProfile();
                // Preserve existing waypoints exactly once when upgrading from
                // the old global file. Other servers start with empty waypoints.
                Path marker = profilesFolder().resolve(".legacy-waypoints-imported");
                boolean importLegacy = next.key().startsWith("world|") && !Files.exists(marker) && hasLegacyWaypoints();
                if (importLegacy) {
                    settings.add("waypoints", defaultSettings.get("waypoints").deepCopy());
                }
                apply(settings);
                active = next;
                BenchConfig.INSTANCE.save();
                if (importLegacy && Files.isRegularFile(destination)) {
                    Files.writeString(marker, "Initial legacy waypoints copied to " + next.filename());
                }
            } else {
                settings = readExisting(destination);
                apply(settings);
                active = next;
            }
            System.out.println("[WuKong] Active profile: " + next.label() + " (" + next.filename() + ")");
        } catch (Exception e) {
            // Do not overwrite an unreadable profile. Preserve its contents.
            active = next;
            ConfigRecovery.preserve(profilesFolder().resolve(next.filename()),e);
            apply(freshProfile());
            System.err.println("[WuKong] Unable to load profile; safe defaults loaded: " + e);
        }
    }

    static JsonObject snapshot() { return JSON.toJsonTree(BenchConfig.INSTANCE).getAsJsonObject(); }
    static JsonObject conservativeProfile() {
        JsonObject copy = freshProfile();
        for (String field : new String[]{"espEnabled", "localGlow", "oreFinderEnabled", "aimAssist", "aimTillDeath", "aimHitTillDeath", "aimTriggerBow", "autoToolEnabled", "autoRefillEnabled", "autoEatEnabled", "autoFishEnabled", "inventorySorterEnabled", "fullbrightEnabled", "enhancedNametags", "itemEspEnabled", "spawnerEspEnabled", "steadyBowView", "separateBowCamera"}) copy.addProperty(field, false);
        return copy;
    }
    static void applyTemplate(JsonObject template) {
        JsonObject old = snapshot();
        JsonObject merged = conservativeProfile();
        template=template.deepCopy(); BenchConfig.migrateMasters(template);
        for (var entry : template.entrySet()) merged.add(entry.getKey(), entry.getValue().deepCopy());
        merged.addProperty("appliedProfileName", "");
        merged.add("waypoints", old.get("waypoints"));
        merged.add("lastDeath", old.get("lastDeath"));
        apply(merged);
        BenchConfig.INSTANCE.save();
        if (active == null) defaultSettings = snapshot();
    }
    private static JsonObject freshProfile() {
        JsonObject copy = JSON.toJsonTree(new BenchConfig()).getAsJsonObject();
        copy.add("waypoints", JSON.toJsonTree(new ArrayList<BenchConfig.Waypoint>()));
        copy.add("lastDeath", com.google.gson.JsonNull.INSTANCE);
        return copy;
    }

    private static boolean hasLegacyWaypoints() {
        JsonElement value = defaultSettings.get("waypoints");
        return value != null && value.isJsonArray() && !value.getAsJsonArray().isEmpty();
    }

    private static JsonObject readExisting(Path source) throws Exception {
        JsonElement parsed = JsonParser.parseString(Files.readString(source));
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("Profile root must be a JSON object: " + source);
        BenchConfig.migrateMasters(parsed.getAsJsonObject());
        JsonObject merged = freshProfile();
        // Old profiles may be missing fields added by future WuKong versions.
        for (var entry : parsed.getAsJsonObject().entrySet()) {
            merged.add(entry.getKey(), entry.getValue().deepCopy());
        }
        return merged;
    }

    private static void apply(JsonObject data) {
        BenchConfig incoming = JSON.fromJson(data, BenchConfig.class);
        if (incoming == null) throw new IllegalArgumentException("Empty configuration");
        // Copy instance fields in place: all existing GUI screens reference INSTANCE.
        try {
            for (Field field : BenchConfig.class.getFields()) {
                if (!Modifier.isStatic(field.getModifiers())) field.set(BenchConfig.INSTANCE, field.get(incoming));
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot apply profile settings", e);
        }
    }

    private ServerProfiles() {}
}
