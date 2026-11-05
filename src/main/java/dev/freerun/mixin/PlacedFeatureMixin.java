package dev.freerun.mixin;

import dev.freerun.city.CityCleaner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No trees, grass or flowers grow in (or spill into) a city. */
@Mixin(PlacedFeature.class)
public abstract class PlacedFeatureMixin {
    @Shadow @Final private Holder<ConfiguredFeature<?, ?>> feature;

    @Inject(method = "placeWithBiomeCheck", at = @At("HEAD"), cancellable = true)
    private void freerun$noVegetationInCities(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos pos,
                                              CallbackInfoReturnable<Boolean> cir) {
        try {
            if (CityCleaner.suppress(feature.value().feature(), level, generator, pos)) cir.setReturnValue(false);
        } catch (RuntimeException ignored) {
            // never break world generation over this
        }
    }
}
