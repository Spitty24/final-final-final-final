package dev.freerun.client.skate;

import dev.freerun.skate.Skating;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** The wheels rolling: louder and higher the faster you go, silent in the air and on rails. */
public final class RollSound extends AbstractTickableSoundInstance {
    private static RollSound current;

    private RollSound() {
        super(Skating.ROLL, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        looping = true;
        delay = 0;
        volume = 0.01f;
        relative = false;
    }

    public static void start(Minecraft mc) {
        if (current != null && !current.isStopped()) return;
        current = new RollSound();
        mc.getSoundManager().play(current);
    }

    @Override
    public void tick() {
        var p = Minecraft.getInstance().player;
        if (p == null || !SkateClient.active()) {
            stop();
            return;
        }
        x = p.getX();
        y = p.getY();
        z = p.getZ();
        double v = SkateClient.speed();
        boolean rolling = p.onGround() && !SkateClient.grinding();
        float want = rolling ? (float) Math.min(1.0, v * 2.4) : 0f;
        volume += (want - volume) * 0.3f;
        pitch = 0.7f + (float) Math.min(0.7, v * 1.2);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
