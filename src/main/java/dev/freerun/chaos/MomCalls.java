package dev.freerun.chaos;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every so often (somewhere between fifteen and forty minutes of play) the screen goes black and
 * a phone rings: it's Mom. Answer, and she tells you to come home for dinner; get to your bed (or
 * spawn) in time and there's meatloaf. Ignore her, and every mob around turns to stare at you,
 * disappointed, for a full minute.
 */
public final class MomCalls {
    private static final class State {
        int nextCall;
        int ringing = -1;
        int dinnerLeft = -1;
        BlockPos home;
        int disapproval;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private MomCalls() {}

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(MomCalls::tick);
    }

    private static State state(ServerPlayer p) {
        return STATES.computeIfAbsent(p.getUUID(), u -> {
            State s = new State();
            s.nextCall = next(p);
            return s;
        });
    }

    private static int next(ServerPlayer p) {
        return 20 * 60 * (15 + p.getRandom().nextInt(26));
    }

    /** For testing (and for the Backwards Clock's chaos): have Mom call now. */
    public static void callNow(ServerPlayer p) {
        state(p).nextCall = 1;
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            State s = state(p);
            if (p.isSpectator() || p.isDeadOrDying()) continue;
            if (s.ringing >= 0) {
                if (++s.ringing > 20 * 20) answer(p, false);   // she gave up
            } else if (s.dinnerLeft < 0 && --s.nextCall <= 0 && !CreeperPodcast.hosting(p)) {
                s.ringing = 0;
                ChaosPayloads.send(p, ChaosPayloads.MOM_CALL, 0, p.getName().getString(), List.of());
            }
            if (s.dinnerLeft >= 0) dinner(p, s);
            if (s.disapproval > 0) disapprove(p, s);
        }
    }

    static void answer(ServerPlayer p, boolean picked) {
        State s = state(p);
        if (s.ringing < 0) return;
        s.ringing = -1;
        s.nextCall = next(p);
        if (picked) {
            s.home = home(p);
            s.dinnerLeft = 20 * 60 * 5;
            ChaosPayloads.send(p, ChaosPayloads.MOM_QUEST, s.dinnerLeft / 20, "",
                    List.of(String.valueOf(s.home.getX()), String.valueOf(s.home.getY()), String.valueOf(s.home.getZ())));
        } else {
            s.disapproval = 20 * 60;
            p.sendSystemMessage(Component.literal("§7You let it ring. §oEvery living thing nearby heard you ignore your mom."));
            p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0f, 0.8f);
        }
    }

    private static BlockPos home(ServerPlayer p) {
        BlockPos bed = p.getRespawnPosition();
        if (bed != null && p.getRespawnDimension() == p.level().dimension()) return bed;
        return p.serverLevel().getSharedSpawnPos();
    }

    private static void dinner(ServerPlayer p, State s) {
        s.dinnerLeft--;
        if (p.tickCount % 10 == 0 && p.level().dimension() == net.minecraft.world.level.Level.OVERWORLD
                && p.blockPosition().distSqr(s.home) < 7 * 7) {
            s.dinnerLeft = -1;
            p.sendSystemMessage(Component.literal("§d📞 Mom§7: §fThere you are! Sit down, it's meatloaf. Did you wash your hands?"));
            if (!p.getInventory().add(new ItemStack(ChaosRegistry.MEATLOAF, 2))) p.drop(new ItemStack(ChaosRegistry.MEATLOAF, 2), false);
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
            ChaosPayloads.send(p, ChaosPayloads.MOM_DONE, 1, "", List.of());
            return;
        }
        if (s.dinnerLeft == 0) {
            s.dinnerLeft = -1;
            p.sendSystemMessage(Component.literal("§d📞 Mom§7: §fYour dinner's cold. I'm not mad. §oI'm disappointed."));
            s.disapproval = 20 * 30;
            ChaosPayloads.send(p, ChaosPayloads.MOM_DONE, 0, "", List.of());
        }
    }

    /** Every mob nearby stops what it's doing and stares at you. Tsk. */
    private static void disapprove(ServerPlayer p, State s) {
        s.disapproval--;
        if (p.tickCount % 2 != 0) return;
        ServerLevel level = p.serverLevel();
        for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(80, 40, 80), Mob::isAlive)) {
            m.getNavigation().stop();
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 9, false, false));
            Vec3 d = p.getEyePosition().subtract(m.getEyePosition());
            float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
            float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
            m.setYRot(yaw);
            m.setYHeadRot(yaw);
            m.yBodyRot = yaw;
            m.setXRot(pitch);
            m.getLookControl().setLookAt(p, 90, 90);
            // and the odd slow, disappointed head shake
            if (m.getRandom().nextInt(400) == 0) {
                level.playSound(null, m.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.5f, 0.6f + m.getRandom().nextFloat() * 0.4f);
            }
        }
        if (s.disapproval == 0) p.displayClientMessage(Component.literal("§7Everyone's gone back to what they were doing. Call your mom."), true);
    }
}
