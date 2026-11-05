package dev.freerun.item;

import dev.freerun.evil.EvilConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** The shell fired by the Nuke Launcher. Flies until it hits something, then makes a huge boom. */
public class NukeProjectile extends ThrowableItemProjectile {

    public NukeProjectile(EntityType<? extends NukeProjectile> type, Level level) {
        super(type, level);
    }

    public NukeProjectile(Level level, LivingEntity shooter) {
        super(ItemRegistry.NUKE_PROJECTILE, shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.TNT;   // renders as a flying block of TNT
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        detonate();
    }

    private void detonate() {
        EvilConfig cfg = EvilConfig.get();
        Level.ExplosionInteraction interaction = cfg.nukeDestroysBlocks
                ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
        level().explode(this, getX(), getY(), getZ(), cfg.nukeRadius, cfg.nukeFire, interaction);

        // Mushroom cloud: a stack of big explosion puffs rising above the blast.
        if (level() instanceof ServerLevel server) {
            for (int i = 0; i < 8; i++) {
                server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        getX(), getY() + i * 1.5, getZ(), 1, 0.6, 0.2, 0.6, 0.0);
            }
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    getX(), getY() + 12, getZ(), 6, 2.5, 0.5, 2.5, 0.0);
        }
        discard();
    }
}
