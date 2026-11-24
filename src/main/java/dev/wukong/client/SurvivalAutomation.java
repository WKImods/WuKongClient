package dev.wukong.client;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Survival helpers. Only client-side, uses vanilla inventory packets for swaps. */
public final class SurvivalAutomation {
    private static Object world;
    private static final ItemStack[] refillMemory = new ItemStack[9];
    private static final int[] refillRetry = new int[9];
    private static int clock;
    private static boolean autoEating;
    private static int previousSelected = -1;
    private static int eatingSlot = -1;
    private static int eatCooldown;
    private static int eatTicks;
    private static int foodCountAtStart;
    private static ItemStack foodAtStart = ItemStack.EMPTY;
    private static int toolPreviousSlot = -1, toolSelectedSlot = -1;

    public static void install() {
        // Prevent starting a new mining action with a critically worn tool.
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!level.isClientSide() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            var mc = Minecraft.getInstance();
            if (!BenchConfig.INSTANCE.toolSaverEnabled || mc.player != player) return InteractionResult.PASS;
            return protectedTool(player.getMainHandItem()) ? InteractionResult.FAIL : InteractionResult.PASS;
        });
    }

    public static void tick(Minecraft mc, boolean gameplayAllowed) {
        if (mc.level == null || mc.player == null || mc.level != world) {
            world = mc.level;
            for (int i=0;i<9;i++) { refillMemory[i]=null; refillRetry[i]=0; }
            // Never leak an artificial held-use key into another world.
            if (autoEating) mc.options.keyUse.setDown(false);
            autoEating = false; previousSelected = -1; eatingSlot = -1;
            eatTicks = 0; foodCountAtStart = 0; foodAtStart = ItemStack.EMPTY;
            eatCooldown = 0; clock = 0; toolPreviousSlot=-1; toolSelectedSlot=-1;
            if (mc.player == null || mc.level == null) return;
        }
        clock++;
        if (eatCooldown>0) eatCooldown--;
        var cfg = BenchConfig.INSTANCE;
        // Never automate inventory or held items while a GUI is open.
        if (!gameplayAllowed || mc.gameMode == null) {
            if (autoEating) finishEating(mc, true);
            rememberHotbar(mc);
            return;
        }
        // Autorefill only observes normal inventory contents; it never writes to
        // the client-only Inventory directly, and never touches a damaged tool.
        if (cfg.autoRefillEnabled && !autoEating) refill(mc);
        else rememberHotbar(mc);
        if (autoEating) { /* Auto Eat owns the selected hotbar slot. */ }
        else if (cfg.autoToolEnabled && mc.options.keyAttack.isDown()) chooseTool(mc);
        else restoreAfterMining(mc);
        if (cfg.toolSaverEnabled) protectSelectedTool(mc);
        handleAutoEat(mc);
    }

    private static boolean protectedTool(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isDamageableItem()
                && SurvivalRules.wornOut(stack.getMaxDamage(),stack.getDamageValue(),
                    BenchConfig.INSTANCE.toolSaverMinDurability);
    }

    private static void protectSelectedTool(Minecraft mc) {
        if (!(mc.hitResult instanceof BlockHitResult) || !mc.options.keyAttack.isDown()) return;
        if (protectedTool(mc.player.getMainHandItem())) {
            // Stop *ongoing* mining as well as the AttackBlockCallback start check.
            mc.gameMode.stopDestroyBlock();
            mc.options.keyAttack.setDown(false);
        }
    }

    private static void chooseTool(Minecraft mc) {
        if (!(mc.hitResult instanceof BlockHitResult hit)) return;
        BlockState state = mc.level.getBlockState(hit.getBlockPos());
        if (state.isAir()) return;
        var inv=mc.player.getInventory();
        int selected=inv.getSelectedSlot();
        double bestScore=-1;
        int best=selected;
        for(int i=0;i<9;i++) {
            ItemStack stack=inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (BenchConfig.INSTANCE.toolSaverEnabled && protectedTool(stack)) continue;
            double score=SurvivalRules.miningScore(stack.getDestroySpeed(state),stack.isCorrectToolForDrops(state));
            // For identical performance, prefer the already selected tool.
            if (score > bestScore+0.001 || (Math.abs(score-bestScore)<0.001 && i==selected)) {
                best=i;bestScore=score;
            }
        }
        if (best!=selected && bestScore>1.01) {
            if (toolPreviousSlot<0) toolPreviousSlot=selected;
            inv.setSelectedSlot(best);
            toolSelectedSlot=best;
        }
    }

    private static void restoreAfterMining(Minecraft mc) {
        if (toolPreviousSlot>=0 && mc.player.getInventory().getSelectedSlot()==toolSelectedSlot) {
            mc.player.getInventory().setSelectedSlot(toolPreviousSlot);
        }
        toolPreviousSlot=-1;
        toolSelectedSlot=-1;
    }

    private static void refill(Minecraft mc) {
        var inv=mc.player.getInventory();
        for(int i=0;i<9;i++) {
            ItemStack stack=inv.getItem(i);
            if (!stack.isEmpty()) {
                if (!stack.isDamageableItem()) refillMemory[i]=stack.copy();
                else refillMemory[i]=null;
                continue;
            }
            ItemStack remembered=refillMemory[i];
            if (remembered==null || clock<refillRetry[i]) continue;
            for (int slot=9;slot<36;slot++) {
                ItemStack candidate=inv.getItem(slot);
                if (!candidate.isEmpty() && ItemStack.isSameItemSameComponents(remembered,candidate)) {
                    // Player inventory menu: main storage indices 9..35, hotbar button 0..8.
                    mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId,
                            slot,i,ContainerInput.SWAP,mc.player);
                    refillRetry[i]=clock+12; // avoid sending repeated clicks during server delay
                    break;
                }
            }
        }
    }

    private static void rememberHotbar(Minecraft mc) {
        var inv=mc.player.getInventory();
        for(int i=0;i<9;i++) {
            ItemStack stack=inv.getItem(i);
            if (!stack.isEmpty()) refillMemory[i] = stack.isDamageableItem()?null:stack.copy();
        }
    }

    private static boolean isSafeFood(ItemStack stack) {
        if (stack.isEmpty() || !stack.has(DataComponents.FOOD) || !stack.has(DataComponents.CONSUMABLE)) return false;
        return stack.getItem()!=Items.PUFFERFISH && stack.getItem()!=Items.POISONOUS_POTATO
                && stack.getItem()!=Items.SPIDER_EYE && stack.getItem()!=Items.ROTTEN_FLESH
                && stack.getItem()!=Items.CHICKEN;
    }

    /**
     * Auto Eat owns the use key until the server/client consumption finishes.
     * Calling useItem once without holding keyUse made vanilla release the
     * action on the very next tick and caused rapid hotbar flicker.
     */
    private static void handleAutoEat(Minecraft mc) {
        var cfg = BenchConfig.INSTANCE;
        var inv = mc.player.getInventory();
        if (autoEating) {
            // An explicit slot change, GUI, disabled module or attack wins.
            if (!cfg.autoEatEnabled || inv.getSelectedSlot() != eatingSlot
                    || mc.options.keyAttack.isDown()) {
                finishEating(mc, true);
                return;
            }
            ItemStack held = mc.player.getMainHandItem();
            boolean sameFood = isSafeFood(held)
                    && ItemStack.isSameItemSameComponents(foodAtStart, held);
            // One item eaten, or the stack was exhausted. Do not keep holding
            // use indefinitely (which would consume extra food).
            if (!sameFood || held.getCount() < foodCountAtStart) {
                finishEating(mc, false);
                return;
            }
            if (eatTicks >= 90) { // failed/blocked use: don't trap the hotbar.
                finishEating(mc, true);
                return;
            }
            // Vanilla's client input loop needs the use key to remain down
            // throughout the food-use duration; reassert after each client tick.
            mc.options.keyUse.setDown(true);
            eatTicks++;
            // Allow the selected-slot update to reach the server before
            // diagnosing a failed interaction; retry only twice.
            if (!mc.player.isUsingItem()) {
                if (eatTicks == 2 || eatTicks == 5) {
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                }
                if (eatTicks >= 18) finishEating(mc, true);
            }
            return;
        }
        if (!cfg.autoEatEnabled || eatCooldown > 0 || mc.options.keyUse.isDown()
                || mc.options.keyAttack.isDown() || mc.player.isUsingItem()
                || !SurvivalRules.shouldEat(mc.player.getFoodData().getFoodLevel(),
                    cfg.autoEatHungerThreshold)) return;

        int food = -1;
        if (isSafeFood(mc.player.getMainHandItem())) food = inv.getSelectedSlot();
        else for (int i = 0; i < 9; i++) {
            if (isSafeFood(inv.getItem(i))) { food = i; break; }
        }
        if (food < 0) return; // Only consume known food from the hotbar.

        previousSelected = inv.getSelectedSlot();
        eatingSlot = food;
        foodAtStart = inv.getItem(food).copy();
        foodCountAtStart = foodAtStart.getCount();
        eatTicks = 0;
        autoEating = true;
        if (food != previousSelected) inv.setSelectedSlot(food);
        // Synthetic hold must begin BEFORE the vanilla use interaction.
        mc.options.keyUse.setDown(true);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        eatCooldown = 8;
    }

    private static void finishEating(Minecraft mc, boolean interrupted) {
        if (!autoEating) return;
        // Always release the synthetic hold (also on GUI open/unload).
        mc.options.keyUse.setDown(false);
        if (interrupted && mc.player != null && mc.gameMode != null
                && mc.player.isUsingItem()) {
            mc.gameMode.releaseUsingItem(mc.player);
        }
        autoEating = false;
        if (mc.player != null && previousSelected >= 0
                && mc.player.getInventory().getSelectedSlot() == eatingSlot) {
            mc.player.getInventory().setSelectedSlot(previousSelected);
        }
        previousSelected = -1;
        eatingSlot = -1;
        eatTicks = 0;
        foodAtStart = ItemStack.EMPTY;
        foodCountAtStart = 0;
        eatCooldown = 12;
    }

    private SurvivalAutomation() {}
}
