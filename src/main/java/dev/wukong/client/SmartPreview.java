package dev.wukong.client;

import java.util.List;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

/** Read-only snapshot of item components, never queries a remote inventory. */
public record SmartPreview(List<ItemStack> items, int columns) implements TooltipComponent {
    public static final class Renderer implements ClientTooltipComponent {
        private final SmartPreview preview;
        public Renderer(SmartPreview preview) { this.preview = preview; }
        private int rows() { return Math.max(1, (preview.items.size() + preview.columns - 1) / preview.columns); }
        public int getHeight(Font font) { return rows() * 18 + 4; }
        public int getWidth(Font font) { return preview.columns * 18 + 4; }
        public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor g) {
            for (int i=0; i<rows()*preview.columns; i++) {
                int xx=x+2+(i%preview.columns)*18, yy=y+2+(i/preview.columns)*18;
                g.fill(xx,yy,xx+18,yy+18,0xFF61546E);
                g.fill(xx+1,yy+1,xx+17,yy+17,0xFF28232F);
                if (i>=preview.items.size()) continue;
                ItemStack stack=preview.items.get(i);
                if (!stack.isEmpty()) { g.item(stack,xx+1,yy+1); g.itemDecorations(font,stack,xx+1,yy+1); }
            }
        }
    }
}
