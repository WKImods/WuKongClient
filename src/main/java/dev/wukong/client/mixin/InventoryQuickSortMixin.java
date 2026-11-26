package dev.wukong.client.mixin;

import dev.wukong.client.BenchConfig;
import dev.wukong.client.SmartInventory;
import dev.wukong.client.WuKongLanguage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a vanilla button to the survival inventory, using the existing local sorter. */
@Mixin(InventoryScreen.class)
public abstract class InventoryQuickSortMixin extends AbstractRecipeBookScreen<InventoryMenu> {
    @Unique private Button wukong$sortButton;
    @Unique private long wukong$lastSort;
    @Unique private boolean wukong$sorting;

    protected InventoryQuickSortMixin(InventoryMenu menu, RecipeBookComponent<?> recipes,
                                      Inventory inventory, Component title) {
        super(menu, recipes, inventory, title);
    }

    @Inject(method="init", at=@At("TAIL"), remap=false)
    private void wukong$initQuickSort(CallbackInfo ci) {
        wukong$sortButton = null;
        if (minecraft.player == null || minecraft.player.hasInfiniteMaterials()) return;
        wukong$sortButton = addRenderableWidget(Button.builder(
                Component.literal(WuKongLanguage.text("Sort.", "Sort")), button -> wukong$sort())
            .bounds(leftPos + 132, topPos + 60, 36, 18)
            .tooltip(Tooltip.create(Component.literal(WuKongLanguage.text(
                "Inventar sortieren (Einzelspieler)", "Sort inventory (singleplayer)"))))
            .build());
        wukong$refresh();
    }

    @Inject(method="extractRenderState", at=@At("HEAD"), remap=false)
    private void wukong$updateQuickSort(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                       float partialTick, CallbackInfo ci) {
        wukong$refresh();
    }

    @Inject(method="onRecipeBookButtonClick", at=@At("TAIL"), remap=false)
    private void wukong$recipeBookMoved(CallbackInfo ci) {
        wukong$refresh();
    }

    @Unique private boolean wukong$visible() {
        var player = minecraft.player;
        return player != null && minecraft.level != null && minecraft.hasSingleplayerServer()
            && !player.hasInfiniteMaterials() && BenchConfig.INSTANCE.inventoryMasterEnabled && BenchConfig.INSTANCE.inventorySorterEnabled
            && !(width < 379 && player.getRecipeBook().isOpen(RecipeBookType.CRAFTING));
    }

    @Unique private boolean wukong$canSort() {
        var player = minecraft.player;
        return wukong$visible() && player != null && player.isAlive()
            && minecraft.gameMode != null && player.containerMenu == player.inventoryMenu
            && player.inventoryMenu.getCarried().isEmpty() && !isQuickCrafting
            && !wukong$sorting && (wukong$lastSort == 0L
                || System.nanoTime() - wukong$lastSort >= 400_000_000L);
    }

    @Unique private void wukong$refresh() {
        if (wukong$sortButton == null) return;
        wukong$sortButton.setX(leftPos + 132);
        wukong$sortButton.setY(topPos + 60);
        wukong$sortButton.visible = wukong$visible();
        wukong$sortButton.active = wukong$canSort();
        if (!wukong$sortButton.visible && getFocused() == wukong$sortButton) setFocused(null);
    }

    @Unique private void wukong$sort() {
        if (!wukong$canSort()) return;
        wukong$sorting = true;
        wukong$sortButton.active = false;
        try {
            SmartInventory.sort(minecraft);
        } finally {
            wukong$lastSort = System.nanoTime();
            wukong$sorting = false;
            wukong$refresh();
        }
    }
}
