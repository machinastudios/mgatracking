package com.machina.mstatstracking.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public final class TelemetryClient {
    /**
     * The endpoint for the PostHog API.
     */
    private static final String POSTHOG_ENDPOINT = "https://app.posthog.com/capture/";

    /**
     * The HTTP client for the PostHog API.
     */
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /**
     * The Gson instance for the PostHog API.
     */
    private static final Gson GSON = new Gson();

    private final String apiKey;
    private final String installId;
    private final boolean enabled;

    public TelemetryClient(@Nonnull String apiKey, boolean enabled) {
        this.apiKey = apiKey;
        this.enabled = enabled;

        this.installId = loadOrCreateInstallId();
    }

    /**
     * Send an install event to the PostHog API.
     */
    public void sendInstall(String modName, String modVersion) {
        send("mod_install", Map.of(
            "mod_name", modName,
            "mod_version", modVersion,
            "java_version", System.getProperty("java.version"),
            "os", System.getProperty("os.name"),
            "arch", System.getProperty("os.arch")
        ));
    }

    /**
     * Send a heartbeat event to the PostHog API.
     */
    public void sendHeartbeat(String modName, String modVersion, long uptimeSeconds) {
        send("mod_heartbeat", Map.of(
            "mod_version", modVersion,
            "uptime_seconds", uptimeSeconds
        ));
    }

    /**
     * Send a players online event to the PostHog API.
     */
    public void sendPlayersOnline(String modName, String modVersion, int count) {
        send("players_online", Map.of(
            "count", count,
            "mod_version", modVersion
        ));
    }

    /**
     * Send a player seen event to the PostHog API.
     */
    public void sendPlayerSeen(String modName, String modVersion, String playerUuid, String salt) {
        send("player_seen", Map.of(
            "player_id", sha256(playerUuid + salt),
            "mod_version", modVersion
        ));
    }

    /**
     * Send an error event to the PostHog API.
     */
    public void sendError(String modName, String modVersion, Throwable t) {
        send("mod_error", Map.of(
            "exception", t.getClass().getSimpleName(),
            "message", t.getMessage(),
            "mod_version", modVersion,
            "severity", "error"
        ));
    }

    /**
     * Send an event to the PostHog API.
     */
    private void send(String event, Map<String, Object> properties) {
        // If tracking is not enabled, do not send the event
        if (!enabled) {
            return;
        }

        try {
            // Build the JSON payload
            String json = GSON.toJson(Map.of(
                "api_key", apiKey,
                "event", event,
                "install_id", installId,
                "properties", properties
            ));

            // Build the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(POSTHOG_ENDPOINT))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            // Send the request asynchronously
            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding());

        } catch (Exception e) {
            // Telemetry must NEVER crash the mod
            System.err.println("[MStatsTracking] Error sending event: " + e.getMessage());
        }
    }

    /**
     * Load or create an install ID.
     */
    private static String loadOrCreateInstallId() {
        // idealmente: salvar em arquivo dentro do data folder do mod
        return UUID.randomUUID().toString();
    }

    /**
     * Calculate the SHA-256 hash of a string.
     */
    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (Exception e) {
            return "hash_error";
        }
    }
}