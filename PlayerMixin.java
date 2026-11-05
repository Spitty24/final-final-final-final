package dev.freerun.mixin;

import dev.freerun.Freerun;
import dev.freerun.FreerunConfig;
import dev.freerun.ParkourState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    /** No fall damage when touching down mid wall-run, climb, hang, mantle or vault. */
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void freerun$safeMoves(float fallDistance, float multiplier, DamageSource source,
                                   CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        Freerun.ServerEntry entry = Freerun.SERVER_STATES.get(player.getUUID());
        if (entry != null && entry.state.safe) cir.setReturnValue(false);
    }

    /** A roll armed shortly before landing absorbs part of the fall. */
    @ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float freerun$rollLanding(float fallDistance) {
        if (!((Object) this instanceof ServerPlayer player)) return fallDistance;
        Freerun.ServerEntry entry = Freerun.SERVER_STATES.get(player.getUUID());
        if (entry == null) return fallDistance;
        boolean rolling = entry.state == ParkourState.ROLL_READY || entry.state == ParkourState.ROLL
                || player.level().getGameTime() - entry.rollArmedAt <= 40;
        return rolling ? Math.max(0f, fallDistance - FreerunConfig.get().rollFallReduction) : fallDistance;
    }
}
