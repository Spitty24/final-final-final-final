package dev.freerun.client.chaos;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.freerun.Freerun;
import dev.freerun.chaos.ChaosPayloads;
import dev.freerun.chaos.ChaosRegistry;
import dev.freerun.chaos.HomesickDragon;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** The client side of the weird stuff: screens, overlays, music, and the creepers' headphones. */
public final class ChaosClient {
    private static final ResourceLocation VIGNETTE = Freerun.id("textures/gui/chaos/vignette.png");
    /** Creepers currently recording an episode (they get headphones and a mic). */
    public static final Set<Integer> PODCASTERS = new HashSet<>();

    static int dinnerTicks = -1;
    static BlockPos home;
    private static boolean lofiOn, sadOn;
    private static int fogThought;

    private ChaosClient() {}

    public static void init() {
        EntityRendererRegistry.register(ChaosRegistry.HOMESICK_DRAGON, EnderDragonRenderer::new);
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (renderer instanceof CreeperRenderer cr) helper.register(new PodcastGearLayer(cr));
        });
        ClientPlayNetworking.registerGlobalReceiver(ChaosPayloads.ToClient.TYPE, (payload, ctx) -> ctx.client().execute(() -> receive(payload)));
        ClientTickEvents.END_CLIENT_TICK.register(ChaosClient::tick);
        HudRenderCallback.EVENT.register((g, delta) -> hud(g));
    }

    static void send(int kind, int id, int value) {
        if (ClientPlayNetworking.canSend(ChaosPayloads.ToServer.TYPE)) ClientPlayNetworking.send(new ChaosPayloads.ToServer(kind, id, value));
    }

    private static void receive(ChaosPayloads.ToClient p) {
        Minecraft mc = Minecraft.getInstance();
        switch (p.kind()) {
            case ChaosPayloads.PODCAST_QUESTION -> {
                PODCASTERS.add(p.id());
                if (mc.screen instanceof PodcastScreen ps && ps.creeperId == p.id()) ps.question(p.text(), p.lines());
                else {
                    mc.setScreen(new PodcastScreen(p.id(), p.text(), p.lines()));
                    dev.freerun.client.StreamChat.react(new String[]{"the creeper has a PODCAST??", "creeper podcast arc :fire:",
                            "W guest", "answer carefully bro :skull:", "subscribed"}, 2);
                }
            }
            case ChaosPayloads.PODCAST_LINE -> {
                if (mc.screen instanceof PodcastScreen ps) ps.reply(p.text());
            }
            case ChaosPayloads.PODCAST_END -> {
                PODCASTERS.remove(p.id());
                if (mc.screen instanceof PodcastScreen ps) ps.ended(p.text());
            }
            case ChaosPayloads.MOM_CALL -> {
                mc.setScreen(new MomCallScreen(p.text()));
                dev.freerun.client.StreamChat.react(new String[]{"MOM IS CALLING :sob:", "ANSWER IT", "pick up bro", "not the mom call :skull:",
                        "he's getting called home :sob:", "mom said dinner"}, 3);
            }
            case ChaosPayloads.MOM_QUEST -> {
                dinnerTicks = p.id() * 20;
                home = new BlockPos(Integer.parseInt(p.lines().get(0)), Integer.parseInt(p.lines().get(1)), Integer.parseInt(p.lines().get(2)));
            }
            case ChaosPayloads.MOM_DONE -> dinnerTicks = -1;
            case ChaosPayloads.DRAGON_ASK -> {
                mc.setScreen(new DragonAskScreen());
                dev.freerun.client.StreamChat.react(new String[]{"SAY YES", "the dragon has rizz??", "this is so awkward :sob:",
                        "she's going through it", "say no bro :skull:"}, 4);
            }
            case ChaosPayloads.DRAGON_CREDITS -> mc.setScreen(new AwkwardCreditsScreen(p.text()));
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ music

    static boolean lockedIn(LocalPlayer p) {
        return p != null && p.hasEffect(ChaosRegistry.LOCKED_IN);
    }

    static boolean fogged(LocalPlayer p) {
        return p != null && p.hasEffect(ChaosRegistry.BRAIN_FOG);
    }

    static boolean homesickNear(LocalPlayer p) {
        return p != null && !p.level().getEntitiesOfClass(HomesickDragon.class, p.getBoundingBox().inflate(96)).isEmpty();
    }

    /** A looping, non-positional track that fades in, and fades out once its condition stops. */
    static final class Loop extends AbstractTickableSoundInstance {
        private final BooleanSupplier keep;
        private final float max;
        private boolean fading;

        Loop(SoundEvent sound, float max, BooleanSupplier keep) {
            super(sound, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.keep = keep;
            this.max = max;
            looping = true;
            delay = 0;
            volume = 0.01f;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            if (!keep.getAsBoolean()) fading = true;
            volume = fading ? volume - 0.03f : Math.min(max, volume + 0.03f);
            if (volume <= 0) stop();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        boolean done() {
            return isStopped();
        }

        void kill() {
            stop();
        }
    }

    private static Loop lofi, sad;

    private static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (dinnerTicks > 0) dinnerTicks--;
        boolean wantLofi = lockedIn(p);
        if (wantLofi && (lofi == null || lofi.done())) {
            lofi = new Loop(ChaosRegistry.LOFI, 0.8f, () -> lockedIn(Minecraft.getInstance().player));
            mc.getSoundManager().play(lofi);
            mc.getMusicManager().stopPlaying();
        }
        if (p.tickCount % 20 == 0) {
            boolean wantSad = homesickNear(p);
            if (wantSad && (sad == null || sad.done())) {
                sad = new Loop(ChaosRegistry.SAD_PIANO, 0.7f, () -> homesickNear(Minecraft.getInstance().player));
                mc.getSoundManager().play(sad);
                mc.getMusicManager().stopPlaying();
            }
            sadOn = wantSad;
        }
        if (wantLofi && !lofiOn) dev.freerun.client.StreamChat.react(new String[]{"HE'S LOCKED IN", "lock in arc :fire:", "the focus is crazy",
                "aura +5000", "he's in the zone fr"}, 3);
        if (!wantLofi && lofiOn) dev.freerun.client.StreamChat.react(new String[]{"he crashed :skull:", "the crash is real", "brain fog arc",
                "bro is buffering", "unlocked :sob:"}, 2);
        lofiOn = wantLofi;
        if (fogged(p)) fogThought++;
    }

    // ------------------------------------------------------------------ overlays

    private static final String[] FOG_THOUGHTS = {"...what was I doing", "why did I come in here", "is it tuesday",
            "need. another. can.", "my eyes are open but nobody's home", "brain.exe not responding", "wait what"};

    private static void hud(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui) return;
        int w = g.guiWidth(), h = g.guiHeight();
        float t = p.tickCount + mc.getTimer().getGameTimeDeltaPartialTick(false);
        if (lockedIn(p)) {
            // tunnel vision, pulsing on the beat (84 bpm)
            float beat = (float) Math.pow(Math.max(0, Mth.cos(t / 20f * 84f / 60f * Mth.TWO_PI)), 8);
            vignette(g, w, h, 0.05f, 0.09f, 0.03f, 0.88f + 0.1f * beat);
            g.drawCenteredString(mc.font, "§a§lLOCKED IN", w / 2, 6, 0xFFFFFFFF);
        }
        if (fogged(p)) {
            float k = 0.28f + 0.1f * Mth.sin(t * 0.05f);
            g.fill(0, 0, w, h, ((int) (k * 255) << 24) | 0xD8D8E0);
            vignette(g, w, h, 0.85f, 0.85f, 0.9f, 0.55f);
            int phase = (fogThought / 120) % FOG_THOUGHTS.length;
            int a = (int) (Mth.clamp(Mth.sin((fogThought % 120) / 120f * Mth.PI), 0, 1) * 200);
            if (a > 8) g.drawCenteredString(mc.font, "§o" + FOG_THOUGHTS[phase], w / 2, h / 2 + 30, (a << 24) | 0x404050);
        }
        if (sadOn) g.fill(0, 0, w, h, 0x182A3A60);
        if (dinnerTicks > 0 && home != null) {
            int s = dinnerTicks / 20;
            int dist = (int) Math.sqrt(p.blockPosition().distSqr(home));
            String txt = String.format("§dMom: dinner's ready §7— §f%d:%02d §7· %d blocks home", s / 60, s % 60, dist);
            int tw = mc.font.width(txt);
            g.fill(w / 2 - tw / 2 - 5, h - 72, w / 2 + tw / 2 + 5, h - 59, 0x90000000);
            g.drawCenteredString(mc.font, txt, w / 2, h - 69, 0xFFFFFFFF);
        }
    }

    private static void vignette(GuiGraphics g, int w, int h, float r, float gr, float b, float a) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(r, gr, b, a);
        g.blit(VIGNETTE, 0, 0, 0, 0, w, h, w, h);
        g.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    /** Narrower field of view while Locked In (from the FOV mixin). */
    public static float fov(float fov) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (lockedIn(p)) return fov * 0.85f;
        if (fogged(p)) return fov * 1.04f;
        return fov;
    }
}
