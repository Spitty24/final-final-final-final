package dev.freerun.economy;

import dev.freerun.Freerun;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/** The mod's own sound effects (synthesized gunshots, engine, horn, register, break buzzer). */
public final class ModSounds {
    public static SoundEvent PISTOL_SHOT;
    public static SoundEvent RIFLE_SHOT;
    public static SoundEvent SHOTGUN_SHOT;
    public static SoundEvent BULLET_IMPACT;
    public static SoundEvent GUN_RELOAD;
    public static SoundEvent CAR_ENGINE;
    public static SoundEvent CAR_HORN;
    public static SoundEvent SIREN, CUFFS;
    public static SoundEvent CAR_CRASH;
    public static SoundEvent CASH_REGISTER;
    public static SoundEvent BREAK_BUZZER;
    public static SoundEvent BUS_BRAKE, BUS_ENGINE;

    private ModSounds() {}

    public static void register() {
        PISTOL_SHOT = sound("pistol_shot");
        RIFLE_SHOT = sound("rifle_shot");
        SHOTGUN_SHOT = sound("shotgun_shot");
        BULLET_IMPACT = sound("bullet_impact");
        GUN_RELOAD = sound("gun_reload");
        CAR_ENGINE = sound("car_engine");
        CAR_HORN = sound("car_horn");
        SIREN = sound("siren");
        CUFFS = sound("cuffs");
        CAR_CRASH = sound("car_crash");
        CASH_REGISTER = sound("cash_register");
        BREAK_BUZZER = sound("break_buzzer");
        BUS_BRAKE = sound("bus_brake");
        BUS_ENGINE = sound("bus_engine");
    }

    private static SoundEvent sound(String name) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, Freerun.id(name),
                SoundEvent.createVariableRangeEvent(Freerun.id(name)));
    }
}
