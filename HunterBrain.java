package dev.freerun.evil.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.freerun.Freerun;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Talks to the Claude Messages API on a background thread and keeps the latest {@link HunterDecision}.
 * Nothing here blocks the game loop: {@link #requestAsync} fires and returns immediately; the result
 * is stored in {@link #latest()} when it arrives. The server thread reads latest() each tick.
 *
 * Prompt caching: the system prompt is sent as a cacheable block, so repeated calls bill the
 * instructions at the cheaper cached rate.
 */
public final class HunterBrain {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final String SYSTEM = """
            You are the strategist for a predator entity hunting a player in Minecraft.
            You are given the current situation and must choose a short-term plan.
            Respond with ONLY a compact JSON object, no prose, in this exact shape:
            {"strategy":"chase|cutoff|flank|ambush","targetX":<num|null>,"targetY":<num|null>,"targetZ":<num|null>,"aggression":<0..1>,"taunt":"<short menacing line or empty>"}
            Use "cutoff" with a target coordinate to intercept where the player is heading.
            Keep taunts under 12 words. Never add anything outside the JSON.""";

    private final AtomicReference<HunterDecision> latest = new AtomicReference<>(new HunterDecision());
    private final Deque<Long> callTimes = new ArrayDeque<>();
    private volatile boolean inFlight;

    public HunterDecision latest() {
        return latest.get();
    }

    /** True if we're allowed to make a call right now (rate limit + not already waiting on one). */
    public boolean canCall(AiConfig cfg) {
        if (inFlight) return false;
        long now = System.currentTimeMillis();
        while (!callTimes.isEmpty() && now - callTimes.peekFirst() > 60_000) callTimes.pollFirst();
        return callTimes.size() < cfg.maxCallsPerMinute;
    }

    /** Fire a request for a new plan. Returns immediately; {@link #latest()} updates later. */
    public void requestAsync(AiConfig cfg, String situationJson) {
        String key = cfg.resolveKey();
        if (key.isBlank() || inFlight) return;
        inFlight = true;
        callTimes.addLast(System.currentTimeMillis());

        JsonObject body = new JsonObject();
        body.addProperty("model", cfg.model);
        body.addProperty("max_tokens", cfg.maxOutputTokens);

        // System prompt as a cacheable block.
        JsonArray system = new JsonArray();
        JsonObject sysBlock = new JsonObject();
        sysBlock.addProperty("type", "text");
        sysBlock.addProperty("text", SYSTEM);
        JsonObject cache = new JsonObject();
        cache.addProperty("type", "ephemeral");
        sysBlock.add("cache_control", cache);
        system.add(sysBlock);
        body.add("system", system);

        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", situationJson);
        messages.add(userMsg);
        body.add("messages", messages);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(cfg.endpoint))
                .timeout(Duration.ofSeconds(10))
                .header("content-type", "application/json")
                .header("x-api-key", key)
                .header("anthropic-version", cfg.apiVersion)
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

        HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .whenComplete((resp, err) -> {
                    try {
                        if (err != null) {
                            Freerun.LOGGER.warn("Hunter brain request failed: {}", err.toString());
                            return;
                        }
                        if (resp.statusCode() / 100 != 2) {
                            Freerun.LOGGER.warn("Hunter brain HTTP {}: {}", resp.statusCode(), trim(resp.body()));
                            return;
                        }
                        HunterDecision d = parse(resp.body());
                        if (d != null) latest.set(d);
                    } finally {
                        inFlight = false;
                    }
                });
    }

    /** Pull the model's text out of the Messages response and parse our decision JSON from it. */
    private static HunterDecision parse(String responseBody) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray content = root.getAsJsonArray("content");
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < content.size(); i++) {
                JsonObject block = content.get(i).getAsJsonObject();
                if ("text".equals(block.get("type").getAsString())) text.append(block.get("text").getAsString());
            }
            String json = extractJson(text.toString());
            if (json == null) return null;
            return GSON.fromJson(json, HunterDecision.class);
        } catch (Exception e) {
            Freerun.LOGGER.warn("Could not parse hunter decision", e);
            return null;
        }
    }

    /** Grab the first {...} block in case the model wrapped it in anything. */
    private static String extractJson(String s) {
        int a = s.indexOf('{');
        int b = s.lastIndexOf('}');
        return (a >= 0 && b > a) ? s.substring(a, b + 1) : null;
    }

    private static String trim(String s) {
        return s == null ? "" : s.substring(0, Math.min(s.length(), 300));
    }
}
