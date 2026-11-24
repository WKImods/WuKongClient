package dev.wukong.client;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla ore palette; IDs stay stable in config and are independent of translations. */
public enum OreKind {
    DIAMOND("Diamond", 0xFF51EBEC),
    EMERALD("Emerald", 0xFF38E384),
    ANCIENT_DEBRIS("Netherite (Debris)", 0xFFBC74D9),
    GOLD("Gold", 0xFFFFCF52),
    IRON("Iron", 0xFFF3CBA8),
    COPPER("Copper", 0xFFEA895C),
    REDSTONE("Redstone", 0xFFFF626D),
    LAPIS("Lapis", 0xFF6F9FFF),
    COAL("Coal", 0xFFB4BBC4),
    QUARTZ("Quartz", 0xFFF4E9DD);

    public final String displayName;
    public final int color;
    OreKind(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public static OreKind from(BlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE) return DIAMOND;
        if (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE) return EMERALD;
        if (b == Blocks.ANCIENT_DEBRIS) return ANCIENT_DEBRIS;
        if (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE || b == Blocks.NETHER_GOLD_ORE) return GOLD;
        if (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE) return IRON;
        if (b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE) return COPPER;
        if (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE) return REDSTONE;
        if (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE) return LAPIS;
        if (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE) return COAL;
        if (b == Blocks.NETHER_QUARTZ_ORE) return QUARTZ;
        return null;
    }
}
