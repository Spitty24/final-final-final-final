package dev.freerun.mixin;

import dev.freerun.world.GrillBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives the Grill a 25% faster cook time than a normal smoker. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Inject(method = "getTotalCookTime", at = @At("RETURN"), cancellable = true)
    private static void freerun$grillSpeed(Level level, AbstractFurnaceBlockEntity furnace,
                                           CallbackInfoReturnable<Integer> cir) {
        if (furnace instanceof GrillBlockEntity) {
            cir.setReturnValue(Math.max(1, (int) (cir.getReturnValueI() * 0.75f)));
        }
    }
}
