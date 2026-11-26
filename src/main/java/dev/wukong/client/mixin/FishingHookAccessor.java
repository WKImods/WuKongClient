package dev.wukong.client.mixin;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(FishingHook.class)
public interface FishingHookAccessor {
    @Accessor(value="biting",remap=false) boolean wukong$isBiting();
}
