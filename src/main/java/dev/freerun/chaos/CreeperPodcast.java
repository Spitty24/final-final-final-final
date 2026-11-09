package dev.freerun.chaos;

import dev.freerun.mixin.CreeperAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Creepers don't explode any more. When one would, it sits you down for an episode of its
 * podcast instead: three invasive questions, three answers each. Answer well and it thanks you
 * and wanders off. Answer badly, ghost it, or walk away, and it explodes anyway, emotionally first.
 * (A creeper you light with flint and steel still just explodes.)
 */
public final class CreeperPodcast {
    public static final String DONE_TAG = "freerun_podcasted";
    private static final int ROUNDS = 3;

    record Q(String q, String[] a, int[] score, String[] reply) {}

    private static final Q[] QUESTIONS = {
            q("So. Why'd you punch that tree?", "I needed wood for tools.", 1, "valid. very relatable grind.",
                    "It looked at me funny.", -1, "...wow. violent. we'll edit that out.", "Everyone does it on day one.", 0, "the sheep mentality. noted."),
            q("Be honest. How many dirt houses have you built?", "Zero. I'm an architect.", -1, "lying on a podcast. bold.",
                    "Like... three.", 1, "growth. we love honesty on this show.", "I live in one right now.", 0, "humble. moving on."),
            q("Why do you hit sheep when shears exist?", "Shears are for cowards.", -1, "the sheep can hear you, you know.",
                    "I didn't know shears existed.", 0, "ignorance. classic.", "I use shears, actually.", 1, "a gentleman. rare."),
            q("Do you think about me when you're mining?", "Every second. You haunt me.", 1, "that's... that's actually sweet.",
                    "Not really.", 0, "oof. okay. cool. cool cool cool.", "I think about diamonds.", -1, "always the diamonds with you people."),
            q("What's in the chest you labelled 'misc'?", "Everything.", 1, "most honest thing anyone's said on this show.",
                    "Important stuff.", 0, "vague. very vague.", "None of your business.", -1, "defensive. we'll circle back."),
            q("Why'd you leave your dog in the house for three in-game years?", "He likes it in there.", -1, "that's what they all say.",
                    "I forgot about him.", 0, "he didn't forget about you.", "I visit him every day.", 1, "a good owner. respect."),
            q("When did you last call your mom?", "Yesterday.", 1, "love that for you. she misses you.",
                    "She keeps calling during fights.", 0, "she worries. let her worry.", "What's a mom?", -1, "...we need to talk about that."),
            q("Have you ever thought that maybe YOU'RE the hostile mob?", "...oh no.", 1, "and THAT is growth.",
                    "No. I'm the hero.", -1, "the hero of what, exactly.", "Every day.", 0, "heavy. let's take a short break."),
            q("Why do you have 47 stacks of cobblestone?", "You never know.", 0, "...you never know. deep.",
                    "For my mega base.", 1, "a dreamer. i see you.", "I can't stop picking it up.", -1, "that's a hoarding problem, buddy."),
            q("Hot take: is lava a liquid or a lifestyle?", "A lifestyle.", 1, "finally someone gets it.",
                    "A liquid. It's a liquid.", 0, "boring. correct, but boring.", "I don't go near lava.", -1, "coward energy. noted."),
            q("Do you even have a plan, or do you just wander until it's dark?", "I wander.", 1, "real. the realest.",
                    "I have a detailed plan.", -1, "show me the plan. ...that's what i thought.", "What time is it?", 0, "about creeper o'clock."),
            q("Why do you always run when you hear me?", "You're kind of scary.", 0, "i get that a lot. it hurts every time.",
                    "Because you explode!", -1, "ONE time. okay, many times.", "I'm working on it.", 1, "we're all working on something."),
            q("Rate your own building skills. Out of ten.", "Ten.", -1, "the confidence is louder than the builds.",
                    "Maybe a four.", 1, "self-aware. refreshing.", "Six seven.", 0, "...what does that even mean."),
            q("Last question, and be honest: is this the best podcast you've ever been on?", "Absolutely.", 1, "smash that subscribe.",
                    "It's my first podcast.", 0, "so... yes?", "It's mid.", -1, "wow. WOW."),
    };

    private static final String[] MELTDOWN = {"you know what? your builds have no soul.", "and nobody likes your redstone.",
            "your skin is the default one and EVERYONE noticed.", "you leave furnaces running and walk away.",
            "this episode is cancelled. YOU are cancelled."};

    private static Q q(String q, String a, int sa, String ra, String b, int sb, String rb, String c, int sc, String rc) {
        return new Q(q, new String[]{a, b, c}, new int[]{sa, sb, sc}, new String[]{ra, rb, rc});
    }

    private static final class Episode {
        final UUID creeper, player;
        final List<Integer> order = new ArrayList<>();
        int round, score, age, waitingSince, meltdown = -1;

        Episode(UUID creeper, UUID player) {
            this.creeper = creeper;
            this.player = player;
        }
    }

    private static final Map<UUID, Episode> EPISODES = new HashMap<>();

    private CreeperPodcast() {}

    /** From the creeper mixin, instead of exploding. True = don't explode. */
    public static boolean intercept(Creeper c) {
        if (c.level().isClientSide || c.isIgnited()) return false;
        if (EPISODES.containsKey(c.getUUID())) return true;
        Episode ep = null;
        if (c.getTags().contains(DONE_TAG)) return true;
        ServerPlayer host = c.getTarget() instanceof ServerPlayer sp ? sp : null;
        if (host == null && c.level().getNearestPlayer(c, 6) instanceof ServerPlayer sp) host = sp;
        if (host == null || host.isSpectator()) return false;
        for (Episode e : EPISODES.values()) if (e.player.equals(host.getUUID())) return true;   // one show at a time
        ep = new Episode(c.getUUID(), host.getUUID());
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < QUESTIONS.length - 1; i++) all.add(i);
        java.util.Collections.shuffle(all, new java.util.Random(c.getRandom().nextLong()));
        for (int i = 0; i < ROUNDS - 1; i++) ep.order.add(all.get(i));
        ep.order.add(QUESTIONS.length - 1);
        EPISODES.put(c.getUUID(), ep);
        ((CreeperAccessor) c).freerun$setSwell(0);
        c.setSwellDir(-1);
        c.level().playSound(null, c.blockPosition(), ChaosRegistry.PODCAST_JINGLE, SoundSource.HOSTILE, 1.0f, 1.0f);
        say(host, c, "§lWELCOME BACK§r to the Creeper Cast. today's guest: " + host.getName().getString() + ". let's get into it.");
        ask(host, c, ep);
        return true;
    }

    private static void ask(ServerPlayer p, Creeper c, Episode ep) {
        Q q = QUESTIONS[ep.order.get(ep.round)];
        ep.waitingSince = ep.age;
        ChaosPayloads.send(p, ChaosPayloads.PODCAST_QUESTION, c.getId(), "Question " + (ep.round + 1) + " of " + ROUNDS,
                List.of(q.q(), q.a()[0], q.a()[1], q.a()[2]));
    }

    private static void say(ServerPlayer p, Creeper c, String line) {
        p.sendSystemMessage(Component.literal("§a🎙 Creeper§7: §f" + line));
        ChaosPayloads.send(p, ChaosPayloads.PODCAST_LINE, c.getId(), line, List.of());
    }

    static void answer(ServerPlayer p, int creeperId, int choice) {
        if (!(p.serverLevel().getEntity(creeperId) instanceof Creeper c)) return;
        Episode ep = EPISODES.get(c.getUUID());
        if (ep == null || !ep.player.equals(p.getUUID()) || ep.meltdown >= 0) return;
        Q q = QUESTIONS[ep.order.get(ep.round)];
        if (choice < 0 || choice > 2) {
            ep.score -= 2;
            say(p, c, "...you're just gonna stare at me? on MY podcast?");
        } else {
            ep.score += q.score()[choice];
            say(p, c, q.reply()[choice]);
        }
        ep.round++;
        if (ep.round < ROUNDS && ep.score > -2) {
            ask(p, c, ep);
            return;
        }
        finish(p, c, ep);
    }

    private static void finish(ServerPlayer p, Creeper c, Episode ep) {
        if (ep.score < 0) {
            ep.meltdown = 0;   // emotional first, then the real thing
            ChaosPayloads.send(p, ChaosPayloads.PODCAST_END, c.getId(), "meltdown", List.of());
            return;
        }
        ChaosPayloads.send(p, ChaosPayloads.PODCAST_END, c.getId(), "ok", List.of());
        if (ep.score >= 2) {
            say(p, c, "honestly? best episode we've ever done. like, comment, subscribe. take some merch.");
            c.spawnAtLocation(new ItemStack(Items.GUNPOWDER, 3));
            c.spawnAtLocation(new ItemStack(Items.MUSIC_DISC_CAT));
            ExperienceOrb.award(p.serverLevel(), c.position(), 25);
        } else {
            say(p, c, "eh. mid episode. we'll fix it in post. you're free to go.");
            c.spawnAtLocation(new ItemStack(Items.GUNPOWDER, 1));
        }
        c.addTag(DONE_TAG);
        c.setTarget(null);
        EPISODES.remove(c.getUUID());
    }

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(CreeperPodcast::tick);
    }

    private static void tick(MinecraftServer server) {
        if (EPISODES.isEmpty()) return;
        for (Episode ep : List.copyOf(EPISODES.values())) {
            ep.age++;
            ServerPlayer p = server.getPlayerList().getPlayer(ep.player);
            Creeper c = null;
            if (p != null && p.serverLevel().getEntity(ep.creeper) instanceof Creeper cc) c = cc;
            if (p == null || c == null || !c.isAlive()) {
                if (p != null) ChaosPayloads.send(p, ChaosPayloads.PODCAST_END, c == null ? -1 : c.getId(), "gone", List.of());
                EPISODES.remove(ep.creeper);
                continue;
            }
            // the guest sits still and the host looks at them
            c.getNavigation().stop();
            c.setSwellDir(-1);
            ((CreeperAccessor) c).freerun$setSwell(0);
            c.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 10, false, false));
            c.getLookControl().setLookAt(p, 30, 30);
            if (ep.meltdown >= 0) {
                if (ep.meltdown % 22 == 0) {
                    int k = ep.meltdown / 22;
                    if (k < 3) say(p, c, MELTDOWN[(k + ep.score * -1 + c.getId()) % (MELTDOWN.length - 1)]);
                    if (k == 3) say(p, c, MELTDOWN[MELTDOWN.length - 1]);
                    p.level().playSound(null, c.blockPosition(), SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.0f, 0.8f + k * 0.1f);
                }
                if (++ep.meltdown > 22 * 4 + 10) {
                    EPISODES.remove(ep.creeper);
                    c.addTag(DONE_TAG + "_boom");
                    ((CreeperAccessor) c).freerun$explode();
                }
                continue;
            }
            // walked off, or sat there for a minute: ghosting the podcast
            boolean away = p.distanceTo(c) > 12 || p.level() != c.level();
            if (away || ep.age - ep.waitingSince > 20 * 60) {
                say(p, c, away ? "oh you're just gonna LEAVE? during the interview??" : "hello?? we're live??");
                ChaosPayloads.send(p, ChaosPayloads.PODCAST_END, c.getId(), "meltdown", List.of());
                ep.score = -9;
                ep.meltdown = 0;
            }
        }
    }

    /** The creeper mixin asks this before letting a real explosion through. */
    public static boolean mayExplode(Creeper c) {
        return c.isIgnited() || c.getTags().contains(DONE_TAG + "_boom") || !(c.level() instanceof ServerLevel);
    }

    /** Is this creeper mid-interview (the client draws headphones on it from its own list). */
    public static boolean interviewing(Creeper c) {
        return EPISODES.containsKey(c.getUUID());
    }

    static boolean hosting(Player p) {
        for (Episode e : EPISODES.values()) if (e.player.equals(p.getUUID())) return true;
        return false;
    }
}
