package dev.freerun;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Settings, stored in config/freerun.json. Edit the file and restart the game to apply. */
public final class FreerunConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("freerun.json");
    private static FreerunConfig instance;

    // --- moves ---
    public boolean enabled = true;
    public boolean wallRun = true;
    public boolean wallClimb = true;
    public boolean ledgeGrab = true;
    public boolean vault = true;
    /** Vault automatically when sprinting into a waist-high obstacle (otherwise press jump). */
    public boolean autoVault = true;
    public boolean slide = true;
    public boolean roll = true;
    /** Sprinting builds extra speed over ~2 seconds. */
    public boolean momentum = true;
    public double momentumBonus = 0.20;

    // --- tuning ---
    public int wallRunTicks = 30;
    public int wallClimbTicks = 14;
    /** Blocks of fall distance a landing roll absorbs (server side). */
    public float rollFallReduction = 12.0f;

    // --- visuals ---
    public boolean firstPersonArms = true;
    /** Pump both arms while sprinting with empty hands. */
    public boolean sprintArmPump = true;
    /** Camera roll in degrees while wall running (negative flips the direction). */
    public float cameraTilt = 8.0f;
    /** Camera does a forward flip during landing rolls. */
    public boolean cameraRollFlip = true;

    public static FreerunConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        FreerunConfig cfg = null;
        if (Files.exists(FILE)) {
            try (Reader r = Files.newBufferedReader(FILE)) {
                cfg = GSON.fromJson(r, FreerunConfig.class);
            } catch (Exception e) {
                Freerun.LOGGER.warn("Could not read {}, using defaults", FILE, e);
            }
        }
        instance = cfg != null ? cfg : new FreerunConfig();
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
