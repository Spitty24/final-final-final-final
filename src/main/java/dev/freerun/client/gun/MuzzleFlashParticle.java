package dev.freerun.client.gun;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** A two-tick, full-bright starburst at the end of the barrel. */
public class MuzzleFlashParticle extends TextureSheetParticle {
    protected MuzzleFlashParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites);
        lifetime = 2;
        quadSize = 0.22f + random.nextFloat() * 0.1f;
        roll = random.nextFloat() * (float) (Math.PI * 2);
        oRoll = roll;
        hasPhysics = false;
        gravity = 0.0f;
        xd = yd = zd = 0.0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public void tick() {
        super.tick();
        quadSize *= 0.7f;
        alpha = 0.7f;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new MuzzleFlashParticle(level, x, y, z, sprites);
        }
    }
}
