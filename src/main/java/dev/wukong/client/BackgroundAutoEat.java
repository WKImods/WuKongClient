package dev.wukong.client;

import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Opt-in local-player background consumption. Inventory mutations occur on the server thread. */
final class BackgroundAutoEat {
    private record Request(MinecraftServer server, UUID playerId, long session, int threshold) {}
    private static volatile Request requested;
    // Client-thread identity tracking; never dereferenced by the server thread.
    private static Object clientPlayer, clientWorld;
    private static long session;
    // Server-thread-only state. All progress is measured in actual server ticks.
    private static Request active;
    private static ServerPlayer owner;
    private static Object level;
    private static int slot = -1, remaining, cooldown;
    private static ItemStack expected = ItemStack.EMPTY;

    static void install() {
        ServerTickEvents.END_SERVER_TICK.register(BackgroundAutoEat::tick);
    }

    static void publish(Minecraft mc, boolean allowed) {
        if (clientPlayer != mc.player || clientWorld != mc.level) {
            clientPlayer = mc.player;
            clientWorld = mc.level;
            session++;
        }
        var cfg = BenchConfig.INSTANCE;
        if (!allowed || mc.player == null || mc.level == null || mc.gameMode == null
                || mc.gui.screen() != null || mc.isPaused() || !mc.player.isAlive()
                || mc.player.isSpectator() || mc.player.hasInfiniteMaterials()
                || !cfg.survivalMasterEnabled || !cfg.autoEatEnabled || !cfg.autoEatBackground
                || !mc.hasSingleplayerServer() || mc.getSingleplayerServer() == null) {
            requested = null;
            return;
        }
        var next = new Request(mc.getSingleplayerServer(), mc.player.getUUID(), session,
                Math.max(1, Math.min(19, cfg.autoEatHungerThreshold)));
        if (!next.equals(requested)) requested = next;
    }

    private static void cancelMeal() {
        slot = -1;
        remaining = 0;
        expected = ItemStack.EMPTY;
    }

    private static void reset(Request request) {
        active = request;
        owner = null;
        level = null;
        cooldown = 0;
        cancelMeal();
    }

    private static boolean eligible(ServerPlayer player, int threshold) {
        return player != null && player.isAlive() && !player.isSpectator()
                && !player.hasInfiniteMaterials() && !player.isUsingItem()
                && player.containerMenu == player.inventoryMenu
                && player.inventoryMenu.getCarried().isEmpty()
                && SurvivalRules.shouldEat(player.getFoodData().getFoodLevel(), threshold);
    }

    private static boolean edible(ServerPlayer player, ItemStack stack) {
        // Also skip unpredictable teleportation and stew effects in this mode.
        if (!SurvivalAutomation.isSafeFood(stack) || stack.getItem() == Items.CHORUS_FRUIT
                || stack.getItem() == Items.SUSPICIOUS_STEW || player.getCooldowns().isOnCooldown(stack)) return false;
        var consumable = stack.get(DataComponents.CONSUMABLE);
        return consumable != null && consumable.canConsume(player, stack);
    }

    private static int chooseSlot(ServerPlayer player) {
        var inventory = player.getInventory();
        // Prefer storage, then unselected hotbar slots. Never change the selected slot.
        for (int i = 9; i < 36; i++) if (edible(player, inventory.getItem(i))) return i;
        int selected = inventory.getSelectedSlot();
        for (int i = 0; i < 9; i++) if (i != selected && edible(player, inventory.getItem(i))) return i;
        return edible(player, inventory.getItem(selected)) ? selected : -1;
    }

    private static void tick(MinecraftServer server) {
        var request = requested;
        // No request can ever target a dedicated or remote server, or a LAN guest.
        if (request == null || request.server() != server) {
            reset(null);
            return;
        }
        if (active != request) reset(request);
        var player = server.getPlayerList().getPlayer(request.playerId());
        if (player == null) { reset(request); return; }
        if (owner != player || level != player.level()) {
            reset(request);
            owner = player;
            level = player.level();
        }
        if (cooldown > 0) cooldown--;
        if (!eligible(player, request.threshold())) { cancelMeal(); return; }
        if (cooldown > 0) return;
        var inventory = player.getInventory();
        if (slot < 0) {
            slot = chooseSlot(player);
            if (slot < 0) return;
            ItemStack stack = inventory.getItem(slot);
            expected = stack.copy();
            remaining = Math.max(1, stack.getUseDuration(player));
            return;
        }
        ItemStack stack = inventory.getItem(slot);
        // A moved, replaced, consumed or otherwise changed stack cancels this meal.
        if (!edible(player, stack) || !ItemStack.matches(expected, stack)) {
            cancelMeal();
            return;
        }
        if (--remaining > 0) return;
        if (requested != request || !eligible(player, request.threshold())) { cancelMeal(); return; }
        // Vanilla finish processing updates hunger, saturation, effects, statistics,
        // item count, container remainders and use cooldowns. No use key or hand swap.
        ItemStack result = stack.finishUsingItem(player.level(), player);
        inventory.setItem(slot, result);
        inventory.setChanged();
        player.inventoryMenu.broadcastFullState();
        cancelMeal();
        cooldown = 12;
    }

    private BackgroundAutoEat() {}
}
