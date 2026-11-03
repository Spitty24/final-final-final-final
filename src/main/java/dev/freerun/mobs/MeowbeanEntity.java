package dev.freerun.mobs;

import dev.freerun.evil.EvilConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Meowbean: a bean-shaped little guy with a cat face and skinny arms. He toddles over to the
 * nearest player and sings an original, extremely repetitive chiptune jingle on loop, with the
 * lyrics shown above the hotbar. Turn the singing off with meowbeanSings=false in freerun-evil.json.
 */
public class MeowbeanEntity extends PathfinderMob {
    private static final int STEP_TICKS = 3;     // one note every 3 ticks
    private static final int REST = -1;

    /**
     * Original melody (note-block pitches: 6=C, 8=D, 10=E, 13=G, 15=A, 18=high C, 22=high E).
     * Four 16-step bars, looping forever.
     */
    private static final int[] MELODY = {
            10, REST, 10, REST, 13, REST, 10, REST,   6, REST, 6, REST, 8, REST, REST, REST,
            10, REST, 10, REST, 13, REST, 15, REST,  13, REST, 10, REST, 8, REST, 6, REST,
            18, 18, 18, 18, 15, 15, 13, 13,          10, 10, 13, 13, 15, REST, REST, REST,
            13, REST, 10, REST, 8, REST, 6, REST,    22, REST, 22, REST, 22, REST, REST, REST
    };
    private static final String[] LYRICS = {
            "♪ Meow-bean, meow-bean, I'm a little bean ♪",
            "♪ Meow-bean, meow-bean, roundest that you've seen ♪",
            "♪ Bean bean bean bean bean bean bean bean ♪",
            "♪ BEAN! BEAN! BEAN! (one more time!) ♪"
    };

    private int songTick;

    public MeowbeanEntity(EntityType<? extends MeowbeanEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        // Toddle over to the nearest player and stand right next to them.
        Player audience = level().getNearestPlayer(this, 12);
        if (audience != null) {
            getLookControl().setLookAt(audience, 30.0f, 30.0f);
            if (distanceTo(audience) > 3.0f) getNavigation().moveTo(audience, 1.0);
            else getNavigation().stop();
        }

        // Sing whenever anyone is close enough to suffer.
        if (!EvilConfig.get().meowbeanSings || level().getNearestPlayer(this, 24) == null) return;
        songTick++;
        if (songTick % STEP_TICKS != 0) return;

        int step = (songTick / STEP_TICKS) % MELODY.length;
        int note = MELODY[step];
        if (note != REST) {
            float pitch = (float) Math.pow(2.0, (note - 12) / 12.0);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.NOTE_BLOCK_BIT.value(),
                    SoundSource.NEUTRAL, 1.0f, pitch);
        }
        if (step % 8 == 0) {   // cowbell on every half-bar
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.NOTE_BLOCK_COW_BELL.value(),
                    SoundSource.NEUTRAL, 0.8f, 1.0f);
        }
        if (step % 16 == 0) {  // new lyric line every bar
            Component line = Component.literal("§d" + LYRICS[step / 16]);
            for (Player p : level().players()) {
                if (distanceToSqr(p) < 16 * 16) p.displayClientMessage(line, true);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAT_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;   // behaves like an animal: sticks around
    }
}
