package dev.freerun.client.vehicle;

import dev.freerun.economy.ModSounds;
import dev.freerun.vehicle.CarEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/** A looping engine note that follows a running car, rising in pitch with speed. */
public class CarEngineSound extends AbstractTickableSoundInstance {
    private static final Map<Integer, CarEngineSound> PLAYING = new HashMap<>();
    private final CarEntity car;
    private float rpm;

    private CarEngineSound(CarEntity car) {
        super(ModSounds.CAR_ENGINE, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.car = car;
        looping = true;
        delay = 0;
        volume = 0.01f;
        pitch = 0.6f;
        attenuation = SoundInstance.Attenuation.LINEAR;
        x = car.getX();
        y = car.getY();
        z = car.getZ();
    }

    @Override
    public void tick() {
        if (car.isRemoved() || !car.isVehicle()) {
            PLAYING.remove(car.getId());
            stop();
            return;
        }
        x = car.getX();
        y = car.getY() + 0.6;
        z = car.getZ();
        float speed = (float) Math.abs(car.renderSpeed());
        // A fake gearbox: the revs climb, drop at each shift, and climb again.
        float gearSpan = 0.28f;
        float inGear = (speed % gearSpan) / gearSpan;
        float target = 0.55f + Math.min(speed, 1.0f) * 0.35f + inGear * 0.45f;
        rpm += (target - rpm) * 0.25f;
        pitch = Mth.clamp(rpm, 0.5f, 1.6f);
        volume = Mth.clamp(0.45f + speed * 0.5f, 0.0f, 1.0f);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    /** Called every client tick: start a sound for every running car nearby. */
    public static void tickAll(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            PLAYING.clear();
            return;
        }
        if (mc.level.getGameTime() % 10 != 0) return;
        for (CarEntity car : mc.level.getEntitiesOfClass(CarEntity.class, mc.player.getBoundingBox().inflate(48.0))) {
            if (car.isVehicle() && !PLAYING.containsKey(car.getId())) {
                CarEngineSound s = new CarEngineSound(car);
                PLAYING.put(car.getId(), s);
                mc.getSoundManager().play(s);
            }
        }
        PLAYING.values().removeIf(CarEngineSound::isStopped);
    }
}
