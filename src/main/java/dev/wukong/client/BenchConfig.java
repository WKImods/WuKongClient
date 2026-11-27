package dev.wukong.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BenchConfig {
    public boolean researchMonitor = false;
    public boolean players = false;
    public boolean hostile = true;
    public boolean passive = true;
    public boolean containers = false;
    public boolean distanceLabels = true;
    public boolean localGlow = false;
    // ESP layer. Defaults to off; existing category/range settings are reused.
    public boolean espEnabled = false;
    public boolean espThroughWalls = true; // Preserve the old ESP default
    public boolean espBoxes = true;
    public boolean espTracers = false;
    public boolean espNames = true;
    // Split visuals: entity mobs and ore veins do not share box/name toggles.
    // Players, storage and world ESP still use espBoxes/espNames.
    public boolean mobEspEnabled = false;
    public boolean mobEspBoxes = true;
    public boolean mobEspNames = true;
    public boolean oreEspBoxes = true;
    public boolean oreEspNames = true;
    // Individual module switches. The global espEnabled flag always wins.
    public boolean itemEspEnabled = false;
    public boolean spawnerEspEnabled = false;
    public boolean enhancedNametags = false;
    public boolean fullbrightEnabled = false;
    // Category switches are independent of stored child preferences.
    public boolean visualMasterEnabled=false, survivalMasterEnabled=false,
        inventoryMasterEnabled=false, tooltipsMasterEnabled=false, hudMasterEnabled=false;
    // 6.1 world mining: independent profile-backed options, disabled on fresh install.
    public boolean worldMiningMaster=false, areaNuker=false, precisionNuker=false;
    // Explicit per-module engine selection; old profiles default to local mining.
    public boolean areaMultiplayer=false, precisionMultiplayer=false;
    public int areaRadius=2, areaInterval=8, areaBatch=3;
    public boolean areaSphere=true, areaLayer=false, areaSneak=true, areaPreserveOres=true;
    public boolean areaProtectFloor=true;
    public String areaInclude="", areaExclude="minecraft:obsidian,minecraft:crying_obsidian";
    public int precisionWidth=3, precisionHeight=3, precisionDepth=1;
    public boolean precisionSameBlock=true, precisionSneak=false, precisionPreview=true, precisionPreserveOres=true;
    public String precisionInclude="", precisionExclude="minecraft:obsidian,minecraft:crying_obsidian";
    public boolean zoomEnabled=false, zoomSmooth=false, zoomAdaptive=false, zoomRemember=false;
    public int zoomStart=4, zoomMax=50;
    public double zoomRemembered=4.0;
    static void migrateMasters(com.google.gson.JsonObject data) {
        addMaster(data,"visualMasterEnabled","fullbrightEnabled","zoomEnabled");
        addMaster(data,"survivalMasterEnabled","autoToolEnabled","toolSaverEnabled","autoRefillEnabled","autoEatEnabled");
        addMaster(data,"inventoryMasterEnabled","shulkerPreviewEnabled","bundlePreviewEnabled","inventorySorterEnabled","autoFishEnabled");
        addMaster(data,"tooltipsMasterEnabled","enchantmentInfoEnabled","tradeCalculatorEnabled");
        addMaster(data,"hudMasterEnabled","inventoryHudEnabled","armorHudEnabled","potionHudEnabled","performanceHudEnabled","resourceHudEnabled");
    }
    private static void addMaster(com.google.gson.JsonObject data,String name,String... children) {
        if(data.has(name)) return;
        boolean enabled=false;
        for(String child:children) {
            var v=data.get(child);
            if(v!=null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean() && v.getAsBoolean()) enabled=true;
        }
        data.addProperty(name,enabled);
    }
    public boolean waypointsEnabled = false;
    public boolean deathMarkerEnabled = false;
    public boolean inventoryHudEnabled = false;
    public boolean armorHudEnabled = false, potionHudEnabled = false, performanceHudEnabled = false, resourceHudEnabled = false;
    public int armorHudX=20, armorHudY=200, potionHudX=20, potionHudY=450,
        performanceHudX=750, performanceHudY=20, resourceHudX=750, resourceHudY=300;
    public int armorHudScale=100, potionHudScale=100, performanceHudScale=100, resourceHudScale=100;
    public int armorHudOpacity=65, potionHudOpacity=65, performanceHudOpacity=65, resourceHudOpacity=65;
    public String appliedProfileName="";
    public String resourceHudItem="minecraft:stone";
    public int resourceHudGoal=64;
    public boolean storageEspPlus = false;
    // Survival automation: disabled by default; hotkeys configured in Minecraft controls.
    public boolean autoToolEnabled = false;
    public boolean toolSaverEnabled = false;
    public int toolSaverMinDurability = 8; // Remaining uses, 1..100.
    public boolean autoRefillEnabled = false;
    public boolean autoEatEnabled = false;
    public boolean autoEatBackground = false; // Opt-in, integrated server only.
    public boolean shulkerPreviewEnabled = false;
    public boolean bundlePreviewEnabled = false;
    public boolean inventorySorterEnabled = false;
    public String inventorySortOrder = "TYPE";
    public boolean inventorySortMerge = false;
    public int inventorySortLocked = 0; // bits 0..26 correspond to main inventory slots 9..35
    public boolean autoFishEnabled = false;
    public boolean enchantmentInfoEnabled = false;
    public boolean enchantmentInfoShiftOnly = true;
    public boolean tradeCalculatorEnabled = false;
    public int autoEatHungerThreshold = 14; // 1..19 food points.

    public java.util.Map<String, Boolean> oreTypes = defaultOreTypes();
    public java.util.List<Waypoint> waypoints = new java.util.ArrayList<>();
    // Compatibility with the pre-5.3 separate death marker. Migrated on load.
    public Waypoint lastDeath = null;

    public static final class Waypoint {
        public String id = java.util.UUID.randomUUID().toString();
        public String name, dimension;
        public int x, y, z;
        public String icon = "PIN";
        public boolean showInWorld = true;
        public boolean death = false;
        public Waypoint() {}
        public Waypoint(String name, String dimension, int x, int y, int z) {
            this.name=name; this.dimension=dimension; this.x=x; this.y=y; this.z=z;
        }
    }
    private static java.util.Map<String, Boolean> defaultOreTypes() {
        var types = new java.util.LinkedHashMap<String,Boolean>();
        for (OreKind kind : OreKind.values()) types.put(kind.name(),true);
        return types;
    }
    public boolean oreVisible(OreKind kind) {
        return oreTypes == null || !Boolean.FALSE.equals(oreTypes.get(kind.name()));
    }
    public void setOreVisible(OreKind kind, boolean visible) {
        if (oreTypes == null) oreTypes = defaultOreTypes();
        oreTypes.put(kind.name(),visible);
    }
    public boolean oreFinderEnabled = false;
    public boolean oreDotsEnabled = true;
    public int oreRange = 32; // 16..64 blocks; independent of mob ESP range.
    public boolean aimAssist = false;
    public String aimActivationMode = "HOLD"; // HOLD or TOGGLE; default keeps the old behavior
    public boolean aimPlayers = true;
    public boolean aimHostile = true;
    public boolean aimPassive = false;
    public boolean aimRequireVisible = true;
    public int aimRange = 32; // Adjustable 4..128 blocks; separate from ESP range.
    public String aimSpot = "TORSO"; // ALL_VISIBLE, HEAD, NECK, TORSO
    public String aimType = "STANDARD"; // STANDARD, PRECISION, FOCUS, MELEE
    public String aimTargetPriority = "CROSSHAIR"; // TargetRules order; CROSSHAIR keeps existing selection
    public boolean aimTillDeath = false; // Retain selected target while valid
    public boolean aimHitTillDeath = false; // Optional melee only; disabled by default
    public double aimFov = 25.0; // angular radius in degrees
    public boolean aimPrediction = false;
    public boolean bowPredictionDebug = false;
    public boolean bowPredictionLatency = false;
    public boolean aimTriggerBow = false;
    public int aimBowMinChargeTicks = 20;
    public double aimSmoothing = 0.25; // Legacy setting from Combat Step 1
    public double aimAggression = 0.75; // 0.10 to 1.00 (higher = faster tracking)
    public String aimResponseProfile = "CUSTOM"; // Stable profile ID
    public int range = 96;
    public boolean steadyBowView = false;
    public boolean separateBowCamera = false;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public static final BenchConfig INSTANCE = read();
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("wukong.json"); }
    private static Path legacyPath() { return FabricLoader.getInstance().getConfigDir().resolve("lunyxbench.json"); }
    private static BenchConfig read() {
        try {
            Path configPath=path();
            // One-time migration: never modify or delete the older Forge/Fabric config.
            if (!Files.isRegularFile(configPath) && Files.isRegularFile(legacyPath())) {
                Files.createDirectories(configPath.getParent());
                Files.copy(legacyPath(), configPath);
            }
            if (Files.isRegularFile(configPath)) {
                String raw = Files.readString(configPath);
                var data=com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
                migrateMasters(data);
                BenchConfig cfg=JSON.fromJson(data,BenchConfig.class);
                if(cfg!=null) {
                    // Gson initializes missing numeric fields to 0 instead of the field default.
                    // Gson does not execute field initializers when fields are absent.
                    // Previously saved configs get sensible first-run ESP defaults.
                    if (!raw.contains("\"storageEspPlus\"")) cfg.storageEspPlus = false;
                    if (!raw.contains("\"toolSaverMinDurability\"")) cfg.toolSaverMinDurability = 8;
                    if (!raw.contains("\"autoEatHungerThreshold\"")) cfg.autoEatHungerThreshold = 14;
                    if (!data.has("areaProtectFloor")) cfg.areaProtectFloor = true;
                    cfg.toolSaverMinDurability = Math.max(1, Math.min(100, cfg.toolSaverMinDurability));
                    cfg.autoEatHungerThreshold = Math.max(1, Math.min(19, cfg.autoEatHungerThreshold));
                    if (cfg.oreTypes == null) cfg.oreTypes = defaultOreTypes();
                    for (OreKind kind : OreKind.values()) cfg.oreTypes.putIfAbsent(kind.name(),true);
                    if (cfg.waypoints == null) cfg.waypoints = new java.util.ArrayList<>();
                    // Convert the former single death coordinate into a normal removable waypoint.
                    boolean waypointMigration = false;
                    if (!raw.contains("\"showInWorld\"")) {
                        for (Waypoint p : cfg.waypoints) if (p != null) p.showInWorld = true;
                        waypointMigration = !cfg.waypoints.isEmpty();
                    }
                    for (Waypoint p : cfg.waypoints) {
                        if (p == null) continue;
                        if (p.id == null || p.id.isBlank()) { p.id = java.util.UUID.randomUUID().toString(); waypointMigration = true; }
                        if (p.icon == null || !WaypointIcons.isValid(p.icon)) { p.icon = "PIN"; waypointMigration = true; }
                        if (p.name == null || p.name.isBlank()) { p.name = "Waypoint"; waypointMigration = true; }
                    }
                    if (cfg.lastDeath != null) {
                        Waypoint d = cfg.lastDeath;
                        d.death = true; d.icon = "SKULL"; d.showInWorld = true;
                        if (d.id == null || d.id.isBlank()) d.id = java.util.UUID.randomUUID().toString();
                        if (d.name == null || d.name.isBlank()) d.name = "Last death";
                        boolean duplicate = false;
                        for (Waypoint p : cfg.waypoints) {
                            if (p != null && p.death && p.dimension != null && p.dimension.equals(d.dimension)
                                    && p.x == d.x && p.y == d.y && p.z == d.z) { duplicate = true; break; }
                        }
                        if (!duplicate) cfg.waypoints.add(d);
                        cfg.lastDeath = null;
                        waypointMigration = true;
                    }
                    // Write only after all fields are validated below; do not risk a partial migration.
                    if (!raw.contains("\"espBoxes\"")) cfg.espBoxes = true;
                    if (!raw.contains("\"espNames\"")) cfg.espNames = true;
                    if (!raw.contains("\"espThroughWalls\"")) cfg.espThroughWalls = true;
                    if (!raw.contains("\"mobEspEnabled\"")) cfg.mobEspEnabled = false;
                    // Older ESP visuals apply on first migration, then become independent.
                    if (!raw.contains("\"mobEspBoxes\"")) cfg.mobEspBoxes = cfg.espBoxes;
                    if (!raw.contains("\"mobEspNames\"")) cfg.mobEspNames = cfg.espNames;
                    if (!raw.contains("\"oreEspBoxes\"")) cfg.oreEspBoxes = cfg.espBoxes;
                    if (!raw.contains("\"oreEspNames\"")) cfg.oreEspNames = cfg.espNames;
                    if (!raw.contains("\"oreDotsEnabled\"")) cfg.oreDotsEnabled = true;
                    if (cfg.oreRange == 0) cfg.oreRange = 32;
                    cfg.oreRange = Math.max(16, Math.min(64, cfg.oreRange));
                    cfg.aimSpot = AimSpot.normalize(cfg.aimSpot);
                    if (cfg.range <= 0) cfg.range = 96;
                    cfg.range = Math.max(16, Math.min(128, cfg.range));
                    if (cfg.aimAggression <= 0) cfg.aimAggression = 0.75;
                    cfg.aimAggression = Math.max(0.10, Math.min(1.0, cfg.aimAggression));
                    cfg.aimResponseProfile = dev.wukong.client.combat.AimResponse.fromId(cfg.aimResponseProfile).name();
                    if (!"TOGGLE".equals(cfg.aimActivationMode)) cfg.aimActivationMode = "HOLD";
                    if (cfg.aimType == null || !java.util.Set.of("STANDARD", "PRECISION", "FOCUS", "MELEE").contains(cfg.aimType)) cfg.aimType = "STANDARD";
                    cfg.aimTargetPriority = TargetRules.normalize(cfg.aimTargetPriority);
                    if (cfg.aimRange <= 0) cfg.aimRange = 32; // Older saved configs can omit this field.
                    cfg.aimRange = Math.max(4, Math.min(128, cfg.aimRange));
                    if (cfg.aimFov <= 0) cfg.aimFov = 25.0;
                    cfg.aimFov = Math.max(2.0, Math.min(360.0, cfg.aimFov));
                    if (cfg.aimBowMinChargeTicks <= 0) cfg.aimBowMinChargeTicks = 20;
                    cfg.aimBowMinChargeTicks = Math.max(3, Math.min(40, cfg.aimBowMinChargeTicks));
                    if (waypointMigration) cfg.save();
                    return cfg;
                }
            }
        } catch (Exception e) { ConfigRecovery.preserve(path(),e); System.err.println("[WuKong] Config read: "+e); }
        return new BenchConfig();
    }
    /** Save settings to the active world/server profile, never to another world. */
    public void save() {
        Path destination = ServerProfiles.activeConfigPath();
        if (destination == null) destination = path();
        if(!ConfigRecovery.canWrite(destination)) return;
        try {
            Files.createDirectories(destination.getParent());
            Path temp = Files.createTempFile(destination.getParent(), "wukong-save-", ".tmp");
            try {
                Files.writeString(temp, JSON.toJson(this));
                try {
                    Files.move(temp, destination,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                            java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                    Files.move(temp, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (Exception e) {
            System.err.println("[WuKong] Config save failed for " + destination + ": " + e);
        }
    }
}
