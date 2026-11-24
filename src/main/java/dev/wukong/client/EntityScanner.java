package dev.wukong.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;

/** Reads only entities and block entities already in the client's loaded world. */
public final class EntityScanner {
    public record Target(String category, String name, double x, double y, double z, double distance) {}
    public static List<Target> scan() {
        var mc=Minecraft.getInstance();
        var cfg=BenchConfig.INSTANCE;
        List<Target> found=new ArrayList<>();
        if(mc.level==null||mc.player==null) return found;
        final double max=cfg.range*cfg.range;
        for(Entity entity:mc.level.entitiesForRendering()) {
            if(entity==mc.player||entity.isRemoved()||entity.distanceToSqr(mc.player)>max) continue;
            String category=null;
            if(entity instanceof Player && cfg.players) category="PLAYER";
            else if(entity instanceof Mob && entity instanceof Enemy && cfg.hostile) category="HOSTILE";
            else if(entity instanceof Mob && !(entity instanceof Enemy) && cfg.passive) category="MOB";
            if(category!=null) found.add(new Target(category,entity.getName().getString(),entity.getX(),entity.getY(),entity.getZ(),Math.sqrt(entity.distanceToSqr(mc.player))));
        }
        if(cfg.containers || cfg.spawnerEspEnabled) {
            int cx=mc.player.blockPosition().getX()>>4;
            int cz=mc.player.blockPosition().getZ()>>4;
            int radius=Math.min(8,(cfg.range+15)/16);
            for(int dx=-radius;dx<=radius;dx++) for(int dz=-radius;dz<=radius;dz++) {
                LevelChunk chunk=mc.level.getChunkSource().getChunk(cx+dx,cz+dz,false);
                if(chunk==null) continue;
                for(BlockEntity be:chunk.getBlockEntities().values()) {
                    boolean container = cfg.containers && isContainer(be);
                    boolean spawner = cfg.spawnerEspEnabled && be instanceof SpawnerBlockEntity;
                    if (!container && !spawner) continue;
                    BlockPos pos=be.getBlockPos();
                    double dist=pos.distSqr(mc.player.blockPosition());
                    if(dist>max) continue;
                    found.add(new Target(spawner ? "SPAWNER" : "CONTAINER",
                            spawner ? "Mob Spawner" : storageLabel(be),
                            pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,Math.sqrt(dist)));
                }
            }
        }
        found.sort((a,b)->Double.compare(a.distance(),b.distance()));
        return found;
    }
    private static String storageLabel(BlockEntity b) {
        if (b instanceof EnderChestBlockEntity) return "Ender Chest";
        if (b instanceof ChestBlockEntity) return "Chest";
        if (b instanceof BarrelBlockEntity) return "Barrel";
        if (b instanceof ShulkerBoxBlockEntity) return "Shulker Box";
        if (b instanceof HopperBlockEntity) return "Hopper";
        if (b instanceof DispenserBlockEntity) return "Dispenser";
        if (b instanceof DropperBlockEntity) return "Dropper";
        if (b instanceof AbstractFurnaceBlockEntity) return "Furnace";
        return "Container";
    }
    private static boolean isContainer(BlockEntity b) {
        return b instanceof ChestBlockEntity || b instanceof EnderChestBlockEntity || b instanceof BarrelBlockEntity ||
            b instanceof ShulkerBoxBlockEntity || b instanceof HopperBlockEntity || b instanceof DispenserBlockEntity ||
            b instanceof DropperBlockEntity || b instanceof AbstractFurnaceBlockEntity;
    }
    private EntityScanner() {}
}
