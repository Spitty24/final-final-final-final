package dev.freerun.evil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.freerun.Freerun;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Settings for the "evil" layer, stored in config/freerun-evil.json. */
public final class EvilConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("freerun-evil.json");
    private static EvilConfig instance;

    /** Master switch for everything in this file. Off by default so the base mod stays friendly. */
    public boolean evilMode = false;

    // --- cursed parkour ---
    public boolean cursedParkour = true;
    /** Chance per tick a wall run / climb randomly gives out. */
    public double failChance = 0.035;
    /** Grabbed ledges crumble this many ticks after you grab them (0 = never). */
    public int ledgeCrumbleTicks = 22;
    /** After a hard landing you limp (slowness) for this many ticks. */
    public int limpTicks = 60;

    // --- the pursuer ---
    public boolean pursuer = true;
    /** Ticks of continuous running before the Pursuer spawns. */
    public int pursuerSpawnDelay = 1200;
    /** Base move speed; it ramps up the longer you survive. */
    public double pursuerBaseSpeed = 0.30;
    public double pursuerMaxSpeed = 0.62;
    /** Ticks to go from base to max speed. */
    public int pursuerRampTicks = 6000;
    public double pursuerSpawnDistance = 40.0;
    /** The Pursuer breaks blocks in its way, bridges gaps and pillars up walls to reach you. */
    public boolean pursuerGriefs = true;
    /** Blocks it will reach out to break/place from its own position (keeps terraforming local). */
    public double pursuerGriefRange = 6.0;
    /** On a catch, it steals this many random item stacks from your inventory (0 = none). */
    public boolean pursuerStealsItems = true;
    public int pursuerStealCount = 3;
    /** On a catch, it takes this many hearts off you (1 heart = 2 health). */
    public double pursuerCatchHearts = 5.0;

    // --- gravity's grudge ---
    public boolean gravityGrudge = true;
    /** Extra downward pull (blocks/tick^2) that grows while you stand still. */
    public double grudgePull = 0.03;
    public int grudgeGraceTicks = 40;

    // --- caught behaviour ---
    /**
     * What happens when the Pursuer catches you:
     * FAKE_OUT, HARDCORE_SCATTER, CURSED_RESPAWN, or DELETE_WORLD.
     * WARNING: DELETE_WORLD permanently deletes the current single-player save. There is no undo.
     * It is ignored on multiplayer servers (the caught player just gets HARDCORE_SCATTER instead).
     */
    public String onCaught = "FAKE_OUT";
    /** Extra gate so DELETE_WORLD can never fire by accident: must be true AND onCaught must be DELETE_WORLD. */
    public boolean allowWorldDeletion = false;

    // --- visuals ---
    public boolean evilSkin = true;
    public boolean darkTrails = true;

    // --- grumbus (independent of evilMode; he's harmless) ---
    public boolean grumbus = true;
    /** The Wingus: appears 2 minutes in, hunts Grumbus, eats him, flies north. */
    public boolean wingus = true;

    // --- nuke launcher (independent of evilMode) ---
    public float nukeRadius = 14.0f;         // TNT is 4; this is a big one
    public boolean nukeFire = true;
    public boolean nukeDestroysBlocks = true;

    public CaughtMode caughtMode() {
        try {
            return CaughtMode.valueOf(onCaught);
        } catch (IllegalArgumentException e) {
            return CaughtMode.FAKE_OUT;
        }
    }

    public enum CaughtMode { FAKE_OUT, HARDCORE_SCATTER, CURSED_RESPAWN, DELETE_WORLD }

    public static EvilConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        EvilConfig cfg = null;
        if (Files.exists(FILE)) {
            try (Reader r = Files.newBufferedReader(FILE)) {
                cfg = GSON.fromJson(r, EvilConfig.class);
            } catch (Exception e) {
                Freerun.LOGGER.warn("Could not read {}, using defaults", FILE, e);
            }
        }
        instance = cfg != null ? cfg : new EvilConfig();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(instance, w);
            }
        } catch (Exception e) {
            Freerun.LOGGER.warn("Could not write {}", FILE, e);
        }
    }
}
