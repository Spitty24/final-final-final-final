package dev.freerun.mixin;

import dev.freerun.alien.Form;
import dev.freerun.alien.Transformations;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A transformed player has the creature's hitbox and eye height, and its damage rules. */
@Mixin(Player.class)
public abstract class PlayerFormMixin {
    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void freerun$formSize(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Player self = (Player) (Object) this;
        Form f = Transformations.form(self);
        if (!f.transformed()) return;
        cir.setReturnValue(f.alien().dimensions(pose == Pose.CROUCHING));
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void freerun$formDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (Transformations.blocksDamage(self, source)) cir.setReturnValue(false);
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void freerun$formHit(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (target instanceof LivingEntity l && !self.level().isClientSide) Transformations.onHit(self, l);
    }
}
