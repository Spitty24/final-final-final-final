package dev.freerun.mixin;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Creeper.class)
public interface CreeperAccessor {
    @Accessor("swell")
    void freerun$setSwell(int swell);

    @Invoker("explodeCreeper")
    void freerun$explode();
}
