package dev.freerun.item.gun;

import dev.freerun.economy.ModSounds;
import dev.freerun.item.ItemRegistry;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Shared gun definitions and firing logic for player-held guns, soldiers and gangsters. Every shot
 * is a real bullet (see {@link BulletEntity}) with a muzzle flash, smoke, an ejected casing and its
 * own gunshot sound.
 */
public enum GunType {
    //        id          damage speed spread pellets cooldown mag  reload auto   ammo
    PISTOL(   "pistol",   5.0f,  7.0f, 1.6f,  1,      5,       12,  30,    false, Ammo.BULLET),
    RIFLE(    "rifle",    6.0f,  9.0f, 0.9f,  1,      3,       30,  50,    true,  Ammo.BULLET),
    SHOTGUN(  "shotgun",  3.0f,  6.0f, 7.0f,  8,      22,      6,   60,    false, Ammo.SHELL);

    public enum Ammo { BULLET, SHELL }

    public final String id;
    public final float damage;
    public final float speed;
    public final float spread;
    public final int pellets;
    public final int cooldown;
    public final int magazine;
    public final int reloadTicks;
    public final boolean automatic;
    public final Ammo ammo;

    GunType(String id, float damage, float speed, float spread, int pellets, int cooldown, int magazine,
            int reloadTicks, boolean automatic, Ammo ammo) {
        this.id = id;
        this.damage = damage;
        this.speed = speed;
        this.spread = spread;
        this.pellets = pellets;
        this.cooldown = cooldown;
        this.magazine = magazine;
        this.reloadTicks = reloadTicks;
        this.automatic = automatic;
        this.ammo = ammo;
    }

    public Item ammoItem() {
        return ammo == Ammo.SHELL ? ItemRegistry.SHOTGUN_SHELL : ItemRegistry.BULLET;
    }

    public SoundEvent sound() {
        return switch (this) {
            case PISTOL -> ModSounds.PISTOL_SHOT;
            case RIFLE -> ModSounds.RIFLE_SHOT;
            case SHOTGUN -> ModSounds.SHOTGUN_SHOT;
        };
    }

    /** Recoil kick for the shooter's view, in degrees. */
    public float recoil() {
        return switch (this) {
            case PISTOL -> 2.5f;
            case RIFLE -> 1.2f;
            case SHOTGUN -> 6.0f;
        };
    }

    /** Where the barrel is: in front of the shooter's eyes, a little right and down (the gun hand). */
    public static Vec3 muzzle(LivingEntity shooter, Vec3 dir) {
        float yaw = shooter.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
        return shooter.getEyePosition().add(dir.normalize().scale(0.8)).add(right.scale(0.28)).add(0.0, -0.18, 0.0);
    }

    /** Fire this gun from a shooter along a direction (needn't be normalised). Server side. */
    public void fire(Level level, LivingEntity shooter, Vec3 dir) {
        if (!(level instanceof ServerLevel server)) return;
        Vec3 d = dir.normalize();
        Vec3 from = muzzle(shooter, d);
        for (int i = 0; i < pellets; i++) {
            BulletEntity bullet = new BulletEntity(ItemRegistry.BULLET_ENTITY, level);
            bullet.setOwner(shooter);
            bullet.setDamage(damage);
            bullet.setPos(shooter.getX(), shooter.getEyeY() - 0.12, shooter.getZ());
            bullet.shoot(d.x, d.y, d.z, speed, spread);
            level.addFreshEntity(bullet);
        }
        // Muzzle flash, smoke and a spent casing flicked out to the right.
        server.sendParticles(ItemRegistry.MUZZLE_FLASH, from.x, from.y, from.z, 1, 0.0, 0.0, 0.0, 0.0);
        server.sendParticles(ParticleTypes.SMOKE, from.x + d.x * 0.2, from.y + d.y * 0.2, from.z + d.z * 0.2,
                this == SHOTGUN ? 6 : 3, 0.04, 0.04, 0.04, 0.02);
        server.sendParticles(ParticleTypes.SMALL_FLAME, from.x, from.y, from.z, this == SHOTGUN ? 4 : 1, 0.03, 0.03, 0.03, 0.02);
        float yaw = shooter.getYRot() * Mth.DEG_TO_RAD;
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(ammoItem())),
                from.x - d.x * 0.5, from.y + 0.05, from.z - d.z * 0.5, 1, -Mth.cos(yaw) * 0.3, 0.1, -Mth.sin(yaw) * 0.3, 0.12);
        level.playSound(null, shooter.getX(), shooter.getEyeY(), shooter.getZ(), sound(), SoundSource.PLAYERS,
                this == RIFLE ? 2.4f : 2.0f, 0.94f + level.random.nextFloat() * 0.12f);
    }
}
