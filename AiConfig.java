package dev.freerun.evil.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.freerun.Freerun;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings for the Claude-driven hunter brain, in config/freerun-ai.json.
 *
 * SECURITY: your API key lives only in this file on your PC. Never commit freerun-ai.json to
 * GitHub and never put the key in the mod jar — anyone who gets it can spend your credits.
 * If apiKeyEnv is set, the key is read from that environment variable instead (preferred).
 */
public final class AiConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("freerun-ai.json");
    private static AiConfig instance;

    /** Master switch. Even when true, the brain stays off until a key is found. */
    public boolean enabled = false;

    /** Paste your Anthropic API key here, OR leave blank and set apiKeyEnv instead. */
    public String apiKey = "";
    /** If set, the key is read from this environment variable (e.g. "ANTHROPIC_API_KEY"). Takes priority over apiKey. */
    public String apiKeyEnv = "ANTHROPIC_API_KEY";

    /** Model id. Small = cheapest; see the console for current ids/prices. */
    public String model = "claude-haiku-4-5";
    public String endpoint = "https://api.anthropic.com/v1/messages";
    public String apiVersion = "2023-06-01";
    public int maxOutputTokens = 200;

    /** How often (in ticks, 20 = 1s) the hunter asks Claude for a new plan. */
    public int thinkIntervalTicks = 100;      // 5 seconds
    /** Hard safety cap so a bug can never spike your bill. */
    public int maxCallsPerMinute = 15;
    /** Only think while the hunter is within this many blocks of you (saves calls). */
    public double thinkRange = 48.0;
    /** Print the model's taunts in chat. */
    public boolean showTaunts = true;

    public String resolveKey() {
        if (apiKeyEnv != null && !apiKeyEnv.isBlank()) {
            String env = System.getenv(apiKeyEnv);
            if (env != null && !env.isBlank()) return env.trim();
        }
        return apiKey == null ? "" : apiKey.trim();
    }

    public boolean usable() {
        return enabled && !resolveKey().isBlank();
    }

    public static AiConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        AiConfig cfg = null;
        if (Files.exists(FILE)) {
            try (Reader r = Files.newBufferedReader(FILE)) {
                cfg = GSON.fromJson(r, AiConfig.class);
            } catch (Exception e) {
                Freerun.LOGGER.warn("Could not read {}, using defaults", FILE, e);
            }
        }
        instance = cfg != null ? cfg : new AiConfig();
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
