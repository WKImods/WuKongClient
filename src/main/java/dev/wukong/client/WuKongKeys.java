package dev.wukong.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * Vanilla-configurable bindings (Options -> Controls -> Key Binds -> WuKong).
 * Use Minecraft's SDL scancode key mappings, not direct InputConstants polling.
 * Bindings are persisted by Minecraft in options.txt, not in wukong.json.
 */
public final class WuKongKeys {
    // Dedicated Ctrl chords. Their base key is customizable in Minecraft Controls.
    // Ctrl remains required even after rebinding, and normal Numpad input does nothing.
    private static final java.util.Set<KeyMapping> CONTROL_CHORDS =
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(WuKong.ID, "controls"));

    public static final KeyMapping MENU = registerKey("menu", InputConstants.KEY_INSERT);
    public static final KeyMapping AIM = registerKey("aim", InputConstants.KEY_R);
    public static final KeyMapping ESP = registerKey("esp", InputConstants.KEY_F7);
    public static final KeyMapping ORE = registerKey("ore", InputConstants.KEY_F9);
    public static final KeyMapping THROUGH_WALLS = ctrlKey("through_walls", InputConstants.KEY_NUMPAD5);
    public static final KeyMapping BOXES = unbound("boxes");
    public static final KeyMapping TRACERS = unbound("tracers");
    public static final KeyMapping LABELS = unbound("labels");
    public static final KeyMapping ORE_DOTS = unbound("ore_dots");
    public static final KeyMapping PLAYERS = ctrlKey("players", InputConstants.KEY_NUMPAD1);
    public static final KeyMapping MOBS = registerKey("mobs", InputConstants.KEY_F8);
    public static final KeyMapping MOB_BOXES = unbound("mob_boxes");
    public static final KeyMapping MOB_NAMES = unbound("mob_names");
    public static final KeyMapping ORE_BOXES = unbound("ore_boxes");
    public static final KeyMapping ORE_NAMES = unbound("ore_names");
    public static final KeyMapping HOSTILE = unbound("hostile");
    public static final KeyMapping PASSIVE = unbound("passive");
    public static final KeyMapping STORAGE = ctrlKey("storage", InputConstants.KEY_NUMPAD2);
    public static final KeyMapping ITEMS = ctrlKey("items", InputConstants.KEY_NUMPAD3);
    public static final KeyMapping SPAWNERS = ctrlKey("spawners", InputConstants.KEY_NUMPAD4);
    public static final KeyMapping NAMETAGS = ctrlKey("nametags", InputConstants.KEY_NUMPAD6);
    public static final KeyMapping FULLBRIGHT = registerKey("fullbright", InputConstants.KEY_F6);
    public static final KeyMapping WAYPOINTS = unbound("waypoints");
    public static final KeyMapping ADD_WAYPOINT = registerKey("add_waypoint", InputConstants.KEY_F10);
    public static final KeyMapping DEATH_MARKER = unbound("death_marker");
    public static final KeyMapping INVENTORY = unbound("inventory");
    public static final KeyMapping AUTO_TOOL = ctrlKey("auto_tool", InputConstants.KEY_NUMPAD7);
    public static final KeyMapping TOOL_SAVER = ctrlKey("tool_saver", InputConstants.KEY_NUMPAD8);
    public static final KeyMapping AUTO_REFILL = ctrlKey("auto_refill", InputConstants.KEY_NUMPAD9);
    public static final KeyMapping AUTO_EAT = ctrlKey("auto_eat", InputConstants.KEY_NUMPAD0);
    public static final KeyMapping SORT_INVENTORY = unbound("sort_inventory");
    public static final KeyMapping AUTO_FISH = unbound("auto_fish");
    public static final KeyMapping ZOOM=registerKey("zoom",InputConstants.KEY_C);
    public static final KeyMapping QUICK_MENU=unbound("quick_menu");
    // Each ore type can also have its own independent hotkey.
    public static final java.util.Map<OreKind, KeyMapping> ORE_TYPES = registerOreTypes();

    private static KeyMapping ctrlKey(String name, int defaultKey) {
        KeyMapping key = registerKey(name, defaultKey);
        CONTROL_CHORDS.add(key);
        return key;
    }
    public static boolean requiresControl(KeyMapping key) {
        return CONTROL_CHORDS.contains(key);
    }
    public static boolean controlPressed() {
        return InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
                || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
    }
    /** Modifier is checked only when consuming a click. */
    public static boolean permits(KeyMapping key, boolean controlDown) {
        return !requiresControl(key) || controlDown;
    }
    public static String displayedBinding(KeyMapping key) {
        String display = key.getTranslatedKeyMessage().getString();
        return requiresControl(key) ? "Ctrl + " + display : display;
    }

    private static KeyMapping unbound(String name) {
        return registerKey(name, InputConstants.UNKNOWN.getValue());
    }
    private static java.util.Map<OreKind, KeyMapping> registerOreTypes() {
        var result = new java.util.EnumMap<OreKind, KeyMapping>(OreKind.class);
        for (OreKind kind : OreKind.values()) result.put(kind, unbound("ore_" + kind.name().toLowerCase(java.util.Locale.ROOT)));
        return result;
    }


    private static KeyMapping registerKey(String name, int defaultKey) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.wukong." + name,
                InputConstants.Type.KEYBOARD,
                defaultKey,
                CATEGORY));
    }

    /** Call during ClientModInitializer to register before Minecraft loads controls. */
    public static void initialize() {
        // Class initialization registers all user-rebindable key mappings.
    }

    private WuKongKeys() {}
}
