package dev.freerun.chaos;

import dev.freerun.mixin.EnderDragonAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Reverse Mode's Ender Dragon. She turns up at your bed, lost. She follows you, slowly, a little
 * above and behind, and every so often says something quietly. Get her to an End portal (or go
 * through one with her close by) and she's home. She can't be hurt and she doesn't break anything.
 * It's sad and you don't know why.
 */
public class HomesickDragon extends EnderDragon {
    private static final String[] LINES = {"is it much further?", "I don't remember the way.", "the sky here is the wrong colour.",
            "thank you for walking with me.", "do you think they still remember me?", "I used to be big there. important.",
            "it's so bright here. everything is so bright.", "sorry. I'm slow today.", "the endermen used to sing at night.",
            "I'm not crying. dragons don't cry.", "will you visit? after?", "you don't have to stay. but... could you?",
            "I miss the quiet.", "what was your home like?"};
    private int talk = 200;
    private int lineIndex;

    public HomesickDragon(EntityType<? extends EnderDragon> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        noPhysics = true;
    }

    @Override
    public void aiStep() {
        // not super: the End fight's AI would circle (0, 0) and chew through the landscape
        oFlapTime = flapTime;
        flapTime += 0.045f;
        if (level().isClientSide) {
            if (lerpSteps > 0) {
                lerpPositionAndRotationStep(lerpSteps, lerpX, lerpY, lerpZ, lerpYRot, lerpXRot);
                lerpSteps--;
            }
        } else {
            fly((ServerLevel) level());
        }
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        EnderDragonAccessor acc = (EnderDragonAccessor) this;
        double[][] hist = acc.freerun$positions();
        int ptr = acc.freerun$posPointer();
        if (ptr < 0) {
            for (double[] h : hist) {
                h[0] = getYRot();
                h[1] = getY();
            }
        }
        ptr = (ptr + 1) % hist.length;
        hist[ptr][0] = getYRot();
        hist[ptr][1] = getY();
        acc.freerun$setPosPointer(ptr);
        for (EnderDragonPart part : getSubEntities()) {
            part.setPos(getX(), getY() + 2, getZ());
            part.xo = getX();
            part.yo = getY() + 2;
            part.zo = getZ();
        }
    }

    private void fly(ServerLevel level) {
        ServerPlayer guide = (ServerPlayer) level.getNearestPlayer(this, 160);
        Vec3 target;
        if (guide != null) {
            Vec3 back = guide.getLookAngle().multiply(1, 0, 1).normalize().scale(-9);
            target = guide.position().add(back).add(0, 8, 0);
        } else {
            target = position();
        }
        Vec3 d = target.subtract(position());
        double dist = d.length();
        Vec3 v = getDeltaMovement();
        Vec3 want = dist < 3 ? Vec3.ZERO : d.normalize().scale(Math.min(0.75, dist * 0.04));
        v = v.add(want.subtract(v).scale(0.06));
        setDeltaMovement(v);
        setPos(getX() + v.x, getY() + v.y + Mth.sin(tickCount * 0.05f) * 0.02, getZ() + v.z);
        if (v.horizontalDistanceSqr() > 0.001) {
            float yaw = (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) + 90f;   // the dragon model faces its tail along +yaw
            setYRot(Mth.approachDegrees(getYRot(), yaw, 3f));
        } else if (guide != null) {
            Vec3 g = guide.position().subtract(position());
            setYRot(Mth.approachDegrees(getYRot(), (float) (Mth.atan2(g.z, g.x) * Mth.RAD_TO_DEG) + 90f, 1.5f));
        }
        if (guide != null && --talk <= 0) {
            talk = 400 + random.nextInt(500);
            guide.sendSystemMessage(Component.literal("§5🐉 the dragon§7: §o" + LINES[lineIndex++ % LINES.length]));
            level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.NEUTRAL, 0.5f, 0.55f);
        }
        // home: an End portal under her
        BlockPos at = blockPosition();
        for (int dy = -12; dy <= 2; dy++) {
            var st = level.getBlockState(at.offset(0, dy, 0));
            if (st.is(Blocks.END_PORTAL) || st.is(Blocks.END_GATEWAY)) {
                goHome(level);
                return;
            }
        }
    }

    /** She's home. */
    public void goHome(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceTo(this) < 160) {
                p.sendSystemMessage(Component.literal("§5🐉 the dragon§7: §othis is it. this is home."));
                p.sendSystemMessage(Component.literal("§5🐉 the dragon§7: §othank you. I won't forget."));
                p.sendSystemMessage(Component.literal("§8She looks back at you once. It's sad and you don't know why."));
                p.giveExperiencePoints(500);
            }
        }
        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.NEUTRAL, 2.0f, 0.5f);
        ReverseMode.dragonHome(level);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }
}
