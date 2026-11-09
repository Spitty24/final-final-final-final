package dev.freerun.mixin;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnderDragon.class)
public interface EnderDragonAccessor {
    @Accessor("positions")
    double[][] freerun$positions();

    @Accessor("posPointer")
    int freerun$posPointer();

    @Accessor("posPointer")
    void freerun$setPosPointer(int p);
}
