package dev.freerun.item.gun;

import dev.freerun.item.ItemRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Shared gun definitions and firing logic for both player-held guns and soldiers. */
public enum GunType {
    //        id          damage vel    spread pellets cooldown  ammo
    PISTOL(   "pistol",   4.0,   2.4f,  3.0f,  1,      10,       Ammo.BULLET),
    RIFLE(    "rifle",    6.0,   3.2f,  1.0f,  1,      16,       Ammo.BULLET),
    SHOTGUN(  "shotgun",  2.5,   2.0f,  12.0f, 6,      28,       Ammo.SHELL);

    public enum Ammo { BULLET, SHELL }

    public final String id;
    public final double damage;
    public final float velocity;
    public final float spread;
    public final int pellets;
    public final int cooldown;
    public final Ammo ammo;

    GunType(String id, double damage, float velocity, float spread, int pellets, int cooldown, Ammo ammo) {
        this.id = id;
        this.damage = damage;
        this.velocity = velocity;
        this.spread = spread;
        this.pellets = pellets;
        this.cooldown = cooldown;
        this.ammo = ammo;
    }

    public Item ammoItem() {
        return ammo == Ammo.SHELL ? ItemRegistry.SHOTGUN_SHELL : ItemRegistry.BULLET;
    }

    /** Fire this gun from a shooter along a direction (unit or not — it's normalised by Arrow#shoot). */
    public void fire(Level level, LivingEntity shooter, Vec3 dir) {
        for (int i = 0; i < pellets; i++) {
            Arrow arrow = new Arrow(level, shooter, new ItemStack(Items.ARROW), null);
            arrow.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
            arrow.shoot(dir.x, dir.y, dir.z, velocity, spread);
            arrow.setBaseDamage(damage);
            arrow.setCritArrow(false);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;   // bullets aren't collectable arrows
            level.addFreshEntity(arrow);
        }
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0f,
                this == SHOTGUN ? 0.8f : 1.3f);
    }
}
