package dev.freerun.client;

import dev.freerun.Freerun;
import dev.freerun.FreerunConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * A fake live-stream chat in the corner of the screen. Somebody says something every 2 to 10
 * seconds, and chat reacts to what you're doing: getting hurt, dying, bailing or landing on the
 * skateboard, riding the bus, transforming, going AFK, typing in real chat. Toggle it with J;
 * size and on/off are in Video Settings.
 */
public final class StreamChat {
    private static final ResourceLocation EMOTES = Freerun.id("emotes");
    private static final String[] EMOTE_NAMES = {"skull", "sob", "fire", "moai", "clown", "heart"};
    private static final int MAX_LINES = 40;
    private static final int WIDTH = 168;

    private record Line(Component text, long born) {}

    private static final List<Line> lines = new ArrayList<>();
    private static final RandomSource R = RandomSource.create();
    private static int nextIn = 60;
    private static int viewers = 1200 + R.nextInt(900);
    private static float hype;
    private static final List<String> queue = new ArrayList<>();
    private static int queueDelay;

    // what we last saw, to notice changes
    private static int lastHurt;
    private static boolean wasLow, wasDead, wasOnBus, wasTransformed, wasInWater, wasNight;
    private static float lastFall;
    private static int still;
    private static double lastX, lastZ;

    private StreamChat() {}

    // ------------------------------------------------------------------ settings

    public static boolean shown() {
        return FreerunConfig.get().streamChat;
    }

    public static void toggle() {
        FreerunConfig.get().streamChat = !shown();
        FreerunConfig.save();
    }

    /** The Video Settings rows: chat size (50% to 200%) and on/off. */
    public static OptionInstance<?>[] options() {
        FreerunConfig cfg = FreerunConfig.get();
        OptionInstance<Integer> size = new OptionInstance<>("freerun.options.stream_chat_size", OptionInstance.noTooltip(),
                (caption, v) -> Options.genericValueLabel(caption, Component.literal(v * 5 + "%")),
                new OptionInstance.IntRange(10, 40), Mth.clamp(cfg.streamChatScale / 5, 10, 40), v -> {
                    FreerunConfig.get().streamChatScale = v * 5;
                    FreerunConfig.save();
                });
        OptionInstance<Boolean> on = OptionInstance.createBoolean("freerun.options.stream_chat", cfg.streamChat, v -> {
            FreerunConfig.get().streamChat = v;
            FreerunConfig.save();
        });
        return new OptionInstance<?>[]{on, size};
    }

    // ------------------------------------------------------------------ what chat says

    private static final String[] GENERAL = {
            "W stream", "L stream fr", "chat is this real", "first", "who else watching at 3am", "bro is cooked :skull:", "this is peak",
            "aura +1000", "-1000 aura :sob:", "no cap this is bussin", "lowkey fire", "highkey mid", "skibidi", "only in ohio :moai:",
            "bro is NOT the main character :skull:", "NPC behavior", "sigma grindset", "mewing rn", "*mewing intensifies*",
            "fanum tax incoming", "he's locked in", "LOCK IN", "the crashout is coming :sob:", "bro is aura farming", "6 7", "SIX SEVEN",
            "67 67 67 67", "chat say W", "W", "W W W W", "L", "ratio", "chopped :skull:", "unc behavior :skull:", "tuff :fire:",
            "that's tuff", "it's giving main character", "it's giving NPC", "delulu is the solulu", "real", "fr fr", "on god",
            "deadass", "bet", "say less", "slay", "ate and left no crumbs", "vibe check", "emotional damage", "bro thinks he's in a movie",
            "goofy ahh gameplay :sob:", "based", "touch grass", "mom said it's my turn on the minecraft", "can I get a hi", "hi chat",
            "hi streamer :heart:", "notice me pls", "say my name pls :sob:", "how is bro this bad :sob:", "mods ban this guy",
            "mods are asleep post skibidi", "stop glazing", "the glaze is crazy", "rent free", "brainrot detected",
            "my brain is rotting :sob:", "this goes hard", "core memory unlocked", "SHEEEESH", "bruh", "bro :skull:",
            ":skull: :skull: :skull:", ":sob: :sob: :sob:", ":fire: :fire: :fire:", ":moai:", ":clown:", "bro forgot he's live",
            "zero rizz :skull:", "unspoken rizz", "the rizzler is live", "pov: you're watching this", "nah I'd win", "clip that",
            "CLIP IT", "bro is speedrunning being cooked", "skill issue", "gg", "ez", "built different", "galaxy brain play",
            "low IQ play :skull:", "we're so back", "it's so over", "the lore is crazy", "this arc :fire:", "villain arc loading",
            "redemption arc", "side quest unlocked", "the grind never stops", "creeper behind you :skull:", "where diamonds",
            "build a dirt house", "go to sleep the phantoms are coming", "what mods is this", "this mod is insane",
            "minecraft 2 looking crazy", "is this real life or ohio", "bro has the attention span of a goldfish",
            "this is my roman empire", "not me watching this in class :sob:", "teacher is coming hide the tab", "W content",
            "pause", "that's crazy", "nah bc why is this so funny :sob:", "im crying :sob:", "LMAOOO", "help :sob:", "the vibes",
            "chat we're witnessing history", "goated", "GOAT", "certified hood classic", "he's him", "she's her", "they're them",
            "rate this play 1-10", "-10/10", "11/10 :fire:", "i'd rate it 6 7", "real ones know", "no thoughts head empty",
            "brain.exe has stopped working", "loading...", "buffering :sob:", "my wifi is crying", "lag diff",
            "the music in my head rn :fire:", "streamer plz say hi to my cat", "my dog is watching too", "aura check",
            "he did NOT just do that :skull:", "the audacity", "be so fr", "ur so real for that", "type 1 if ur here", "1", "1", "2",
            "chat spam W", "this stream > school", "skipping homework for this", "i have a test tomorrow and im here"
    };
    private static final String[] HURT = {"bro took damage :skull:", "OUCH", "that's gonna leave a mark", "health bar crying :sob:",
            "skill issue", "-100 aura", "bro got jumped", "dodge??", "why'd you stand there :sob:", "pain"};
    private static final String[] LOW = {"bro is NOT gonna make it", "eat something :sob:", "HEAL HEAL HEAL", "one heart gameplay",
            "chat he's cooked", "low hp arc", "this is where it ends :skull:"};
    private static final String[] DEATH = {"F", "F", "F", "F in chat", "RIP :sob:", "LMAOOO :skull:", "he's done :skull:",
            "respawn arc", "that was so avoidable", "bro died to that??", "-10000 aura :skull:", "uninstall"};
    private static final String[] LANDED = {"SHEEESH", "that trick was tuff :fire:", "W skater", "skate 4 looking crazy", "aura +500",
            "clean :fire:", "pro skater arc", "bro is cracked", "CLIP THAT", "how did he land that :sob:", "board control diff"};
    private static final String[] BAIL = {"BAILED :skull:", "the bail :sob:", "ouch the board", "skill issue", "bro ate concrete :skull:",
            "-1000 aura", "that looked painful", "replay that LMAO", "gravity said no", "the floor is undefeated"};
    private static final String[] BUS = {"bro takes the bus :sob:", "public transport arc", "no car :skull:", "W bus driver",
            "the bus seats are crazy", "where are we going chat", "bus lore", "next stop: ohio :moai:"};
    private static final String[] TRANSFORM = {"WHAT IS THAT :skull:", "bro turned into WHAT", "transformation arc :fire:",
            "the glow up is crazy", "swamp creature era", "ok that's actually tuff", "i'm scared :sob:"};
    private static final String[] WATER = {"swimming arc", "he's in the water :sob:", "drowning speedrun", "fish mode"};
    private static final String[] NIGHT = {"night arc", "monsters out chat", "go to bed", "it's getting dark :moai:"};
    private static final String[] FALL = {"THE FALL :skull:", "gravity diff", "bro thought he could fly", "AAAAAAA", "fall damage moment"};
    private static final String[] AFK = {"is he AFK", "streamer fell asleep", "bro paused his life", "hello??", "the silence is loud",
            "did he leave :sob:", "AFK farming viewers", "wake up"};
    private static final String[] TYPED = {"who asked :skull:", "ratio", "real", "W take", "L take", "bro is talking to us :sob:",
            "streamer noticed me!!", "he read chat :heart:", "ok but who asked", "agreed", "based take"};

    private static final String[] NAME_A = {"xX", "", "", "", "the", "lil", "big", "sir", "not", "real", "its"};
    private static final String[] NAME_B = {"Sigma", "Skibidi", "Rizz", "Aura", "Gamer", "Toast", "Frog", "Goblin", "Mewing", "Fanum",
            "Npc", "Bussin", "Ohio", "Noob", "Pixel", "Creeper", "Bean", "Nugget", "Potato", "Shrimp", "Moai", "Gremlin", "Cookie",
            "Taco", "Ninja", "Dino", "Pickle", "Sloth", "Waffle", "Chunky"};
    private static final String[] NAME_C = {"King", "Lord", "Master", "Fan", "Enjoyer", "Lover", "Hater", "God", "Main", "Andy", "Kid",
            "Gamer", "Boy", "Girl", "Fr", "Tv", "Ttv", "Yt", "", ""};
    private static final int[] COLORS = {0xFF5F5F, 0x5FA8FF, 0x60E060, 0xFFB040, 0xC060FF, 0xFF70C0, 0x40E0D0, 0xFFE040, 0xA0A0FF, 0xFF8060};

    private static String name() {
        String n = NAME_A[R.nextInt(NAME_A.length)] + NAME_B[R.nextInt(NAME_B.length)] + NAME_C[R.nextInt(NAME_C.length)];
        if (R.nextInt(3) == 0) n += R.nextInt(1000);
        if (n.startsWith("xX")) n += "Xx";
        return n;
    }

    /** Turns ":skull:" and friends into emote glyphs from our font. */
    private static MutableComponent parse(String s) {
        MutableComponent out = Component.empty();
        int i = 0;
        while (i < s.length()) {
            int a = s.indexOf(':', i);
            int b = a < 0 ? -1 : s.indexOf(':', a + 1);
            int emote = -1;
            if (b > a) for (int k = 0; k < EMOTE_NAMES.length; k++) if (s.substring(a + 1, b).equals(EMOTE_NAMES[k])) emote = k;
            if (emote < 0) {
                out.append(Component.literal(a < 0 ? s.substring(i) : s.substring(i, b < 0 ? s.length() : b)));
                i = a < 0 || b < 0 ? s.length() : b;
                continue;
            }
            if (a > i) out.append(Component.literal(s.substring(i, a)));
            out.append(Component.literal(String.valueOf((char) (0xE000 + emote))).withStyle(Style.EMPTY.withFont(EMOTES)));
            i = b + 1;
        }
        return out;
    }

    private static void say(String text) {
        int color = COLORS[R.nextInt(COLORS.length)];
        MutableComponent line = Component.empty();
        if (R.nextInt(9) == 0) line.append(Component.literal("◆ ").withStyle(st -> st.withColor(0xB070FF)));   // a subscriber badge
        line.append(Component.literal(name()).withStyle(st -> st.withColor(color).withBold(true)));
        line.append(Component.literal(": ").withStyle(st -> st.withColor(0xBBBBBB)));
        line.append(parse(text).withStyle(st -> st.withColor(0xEEEEEE)));
        add(line);
    }

    private static void announce(String text) {
        add(Component.literal("★ " + text).withStyle(st -> st.withColor(0xC9A0FF).withItalic(true)));
    }

    private static void add(Component c) {
        lines.add(new Line(c, System.currentTimeMillis()));
        while (lines.size() > MAX_LINES) lines.remove(0);
    }

    private static String pick(String[] pool) {
        return pool[R.nextInt(pool.length)];
    }

    /** Chat reacts: a few messages over the next second or two. */
    public static void react(String[] pool, int howMany) {
        for (int i = 0; i < howMany; i++) queue.add(pick(pool));
        hype = Math.min(1, hype + 0.3f);
    }

    public static void skateLanded(int points) {
        react(LANDED, points > 3000 ? 4 : points > 800 ? 2 : 1);
    }

    public static void skateBailed() {
        react(BAIL, 3);
    }

    public static void playerTyped() {
        react(TYPED, 2);
    }

    // ------------------------------------------------------------------ ticking

    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        if (mc.isPaused()) return;
        // the steady trickle: someone every 2 to 10 seconds (sooner when chat's hyped)
        if (--nextIn <= 0) {
            float roll = R.nextFloat();
            if (roll < 0.04f) announce(name() + " just followed!");
            else if (roll < 0.06f) announce(name() + " subscribed for " + (2 + R.nextInt(30)) + " months!");
            else if (roll < 0.07f) announce(name() + " gifted " + (1 + R.nextInt(10)) + " subs to the chat!");
            else if (roll < 0.08f) announce(name() + " tipped $" + (1 + R.nextInt(20)) + ": \"" + pick(GENERAL).replaceAll(":[a-z]+:", "").trim() + "\"");
            else say(pick(GENERAL));
            int lo = 40, hi = 200;
            nextIn = (int) (lo + R.nextInt(hi - lo + 1) * (1 - hype * 0.6f));
        }
        if (!queue.isEmpty() && --queueDelay <= 0) {
            say(queue.remove(0));
            queueDelay = 4 + R.nextInt(10);
        }
        hype *= 0.995f;
        if (p.tickCount % 40 == 0) viewers = Math.max(150, viewers + R.nextInt(41) - 20 + (int) (hype * 30));

        // things chat notices
        if (p.hurtTime > 0 && lastHurt == 0 && !p.isDeadOrDying() && R.nextInt(5) < 3) react(HURT, 1 + R.nextInt(2));
        lastHurt = p.hurtTime;
        boolean low = p.getHealth() <= 6 && !p.isDeadOrDying();
        if (low && !wasLow) react(LOW, 2);
        wasLow = low;
        boolean dead = p.isDeadOrDying();
        if (dead && !wasDead) react(DEATH, 6);
        wasDead = dead;
        boolean bus = p.getVehicle() instanceof dev.freerun.city.BusEntity;
        if (bus && !wasOnBus) react(BUS, 2);
        wasOnBus = bus;
        boolean tf = dev.freerun.alien.Transformations.form(p).transformed();
        if (tf && !wasTransformed) react(TRANSFORM, 3);
        wasTransformed = tf;
        boolean water = p.isInWater();
        if (water && !wasInWater && R.nextInt(3) == 0) react(WATER, 1);
        wasInWater = water;
        long day = mc.level.getDayTime() % 24000L;
        boolean night = day > 13000 && day < 23000;
        if (night && !wasNight && p.tickCount > 200) react(NIGHT, 2);
        wasNight = night;
        if (p.onGround() && lastFall > 8) react(FALL, 2);
        lastFall = p.onGround() ? 0 : Math.max(lastFall, p.fallDistance);
        // standing still for half a minute
        if (Math.abs(p.getX() - lastX) < 0.01 && Math.abs(p.getZ() - lastZ) < 0.01 && mc.screen == null) {
            if (++still == 600) react(AFK, 3);
        } else {
            still = 0;
        }
        lastX = p.getX();
        lastZ = p.getZ();
    }

    // ------------------------------------------------------------------ drawing

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (!shown() || mc.options.hideGui || mc.player == null) return;
        Font f = mc.font;
        float scale = Mth.clamp(FreerunConfig.get().streamChatScale, 50, 200) / 100f;
        int w = g.guiWidth(), h = g.guiHeight();
        // laid out unscaled, then scaled from the bottom-right corner, above the hotbar side
        int panelW = WIDTH, pad = 4;
        List<FormattedCharSequence> rows = new ArrayList<>();
        List<Float> alphas = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Line l : lines) {
            float a = Mth.clamp((now - l.born()) / 250f, 0, 1);
            for (FormattedCharSequence seq : f.split(l.text(), panelW - pad * 2)) {
                rows.add(seq);
                alphas.add(a);
            }
        }
        int maxRows = 11;
        int from = Math.max(0, rows.size() - maxRows);
        int shownRows = rows.size() - from;
        int lineH = 10, headerH = 13;
        int panelH = headerH + shownRows * lineH + pad * 2;

        var pose = g.pose();
        pose.pushPose();
        float right = w - 6, bottom = h - 70;
        pose.translate(right, bottom, 300);
        pose.scale(scale, scale, 1);
        pose.translate(-panelW, -panelH, 0);
        g.fill(0, 0, panelW, panelH, 0x9A0E0E10);
        g.fill(0, 0, panelW, headerH, 0xCC18141F);
        g.fill(0, headerH - 1, panelW, headerH, 0xFF9146FF);
        // header: the live dot, the title and the viewers
        boolean blink = (now / 600) % 2 == 0;
        g.fill(5, 4, 10, 9, blink ? 0xFFFF3030 : 0xFFAA2020);
        g.drawString(f, "LIVE", 13, 3, 0xFFFF5050, false);
        g.drawString(f, "STREAM CHAT", 40, 3, 0xFFE8E0FF, false);
        String vs = String.format("%,d", viewers);
        g.drawString(f, vs, panelW - 4 - f.width(vs), 3, 0xFFB8B8C8, false);
        int y = headerH + pad;
        for (int i = from; i < rows.size(); i++) {
            int alpha = (int) (alphas.get(i) * 255);
            if (alpha < 8) alpha = 8;
            int x = pad + (int) ((1 - alphas.get(i)) * 12);
            g.drawString(f, rows.get(i), x, y, (alpha << 24) | 0xFFFFFF, false);
            y += lineH;
        }
        pose.popPose();
    }
}
