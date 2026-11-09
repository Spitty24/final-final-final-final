package dev.freerun.chaos;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The real Ender Dragon, in the End, is going through a divorce. She's tired. She talks about her
 * ex, the Wither. Halfway through the fight she asks if you'd rather just grab a drink instead;
 * say yes and the fight's over (portal, egg and all), followed by a very awkward credits scene.
 */
public final class DragonDivorce {
    private static final String[] LINES = {
            "I'm so tired.",
            "you know my ex? the Wither? three heads and not one of them ever listened.",
            "he got the Nether in the divorce. the whole Nether. I got... an island.",
            "sorry. you didn't come all this way to hear about my divorce.",
            "the crystals were a wedding present. I can't even look at them.",
            "he used to blow things up when he was upset. I used to think that was passion.",
            "my therapist says I should stop circling.",
            "the endermen aren't my kids. they just... live here. like me.",
            "is it weird that I sort of like the company?",
            "do you know what it's like to be the final boss of someone else's story?",
            "he's seeing a Warden now. a WARDEN. he can't even SEE.",
            "ow. okay. fair. I'd hit me too.",
            "fifteen years and he still spelled my name with one 'n'.",
    };
    private static final String[] NO = {"...right. of course. nobody ever says yes.", "fine. FINE. let's just do this then."};

    private static final class Talk {
        int timer = 200;
        int line;
        boolean asked;
        int waiting = -1;
    }

    private static final Map<UUID, Talk> TALKS = new HashMap<>();

    private DragonDivorce() {}

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DragonDivorce::tick);
    }

    private static List<ServerPlayer> near(ServerLevel end, EnderDragon d) {
        return end.getPlayers(p -> p.distanceTo(d) < 200 && !p.isSpectator());
    }

    private static void say(ServerLevel end, EnderDragon d, String line) {
        for (ServerPlayer p : near(end, d)) p.sendSystemMessage(Component.literal("§5🐉 Ender Dragon§7: §d§o" + line));
    }

    private static void tick(MinecraftServer server) {
        ServerLevel end = server.getLevel(Level.END);
        if (end == null) return;
        for (EnderDragon d : end.getDragons()) {
            if (d instanceof HomesickDragon || !d.isAlive() || d.getDragonFight() == null) continue;
            Talk t = TALKS.computeIfAbsent(d.getUUID(), u -> new Talk());
            if (near(end, d).isEmpty()) continue;
            if (t.waiting >= 0) {
                if (++t.waiting > 20 * 25) {   // nobody answered
                    t.waiting = -1;
                    say(end, d, NO[0]);
                }
                continue;
            }
            if (!t.asked && d.getHealth() < d.getMaxHealth() * 0.5f) {
                t.asked = true;
                t.waiting = 0;
                say(end, d, "...hey. can I ask you something?");
                say(end, d, "do you want to just... grab a drink instead? I know a place.");
                for (ServerPlayer p : near(end, d)) ChaosPayloads.send(p, ChaosPayloads.DRAGON_ASK, d.getId(), "", List.of());
                continue;
            }
            if (--t.timer <= 0) {
                t.timer = 20 * (16 + d.getRandom().nextInt(14));
                say(end, d, LINES[t.line++ % LINES.length]);
            }
        }
    }

    static void answer(ServerPlayer p, boolean yes) {
        ServerLevel end = p.server.getLevel(Level.END);
        if (end == null) return;
        for (EnderDragon d : end.getDragons()) {
            Talk t = TALKS.get(d.getUUID());
            if (t == null || t.waiting < 0) continue;
            t.waiting = -1;
            if (!yes) {
                say(end, d, NO[0]);
                say(end, d, NO[1]);
                return;
            }
            say(end, d, "oh. oh! okay. yeah. let me just... land.");
            end.playSound(null, d.blockPosition(), SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.HOSTILE, 3.0f, 1.4f);
            d.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
            EndDragonFight fight = d.getDragonFight();
            List<ServerPlayer> watchers = near(end, d);
            if (fight != null) fight.setDragonKilled(d);
            d.discard();
            TALKS.remove(d.getUUID());
            for (ServerPlayer w : watchers) {
                w.giveExperiencePoints(1200);
                ChaosPayloads.send(w, ChaosPayloads.DRAGON_CREDITS, 0, w.getName().getString(), List.of());
            }
            return;
        }
    }
}
