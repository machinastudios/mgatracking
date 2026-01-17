package com.machina.gatracking.services;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.machina.shared.config.PluginConfig;

/**
 * Service for sending events to Google Analytics Measurement Protocol API.
 * Tracks metrics like server usage, player counts, and installations per mod ID.
 */
public class GATrackingService {
    private static final String GA_ENDPOINT = "https://www.google-analytics.com/mp/collect";
    private static final int MAX_EVENTS_PER_REQUEST = 25;
    private static final Gson GSON = new Gson();

    private final PluginConfig config;
    private final CloseableHttpClient httpClient;
    private final Map<String, String> modClientIds = new ConcurrentHashMap<>();
    private final AtomicInteger requestsThisMinute = new AtomicInteger(0);
    private final AtomicLong lastMinuteReset = new AtomicLong(System.currentTimeMillis());

    private final boolean enabled;
    private final String measurementId;
    private final int maxRequestsPerMinute;

    /**
     * Create a new GATrackingService instance.
     *
     * @param config The plugin configuration
     */
    public GATrackingService(@Nonnull PluginConfig config) {
        this.config = config;
        this.httpClient = HttpClients.createDefault();
        this.enabled = config.getBoolean("ga.enabled", true);
        this.measurementId = config.getString("ga.measurementId", "");
        this.maxRequestsPerMinute = config.getInteger("rateLimit.maxRequestsPerMinute", 20);

        // Validate configuration
        if (this.enabled && this.measurementId.isEmpty()) {
            System.err.println("[MGATracking] Warning: Google Analytics tracking is enabled but measurementId is not configured!");
        }
    }

    /**
     * Get the measurement ID for a specific mod, or the global one if not configured.
     *
     * @param modId The mod identifier
     * @return The measurement ID
     */
    @Nonnull
    private String getMeasurementId(@Nonnull String modId) {
        String modIdValue = this.config.getString("mods." + modId + ".measurementId", "");
        return modIdValue.isEmpty() ? this.measurementId : modIdValue;
    }

    /**
     * Check if tracking is enabled for a specific mod.
     *
     * @param modId The mod identifier
     * @return true if tracking is enabled and configured for this mod
     */
    public boolean isEnabled(@Nonnull String modId) {
        if (!this.enabled) {
            return false;
        }
        String measurementId = this.getMeasurementId(modId);
        return !measurementId.isEmpty();
    }

    /**
     * Check if tracking is enabled (global check).
     *
     * @return true if tracking is enabled and configured
     */
    public boolean isEnabled() {
        return this.enabled && !this.measurementId.isEmpty();
    }

    /**
     * Get or create a client ID for a mod.
     * Each mod gets a unique client ID that persists for tracking purposes.
     *
     * @param modId The mod identifier
     * @return The client ID for the mod
     */
    @Nonnull
    private String getClientId(@Nonnull String modId) {
        return this.modClientIds.computeIfAbsent(modId, k -> {
            // Generate a new client ID: timestamp.random
            long timestamp = System.currentTimeMillis() / 1000;
            String random = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            return timestamp + "." + random;
        });
    }

    /**
     * Check if we can send a request (rate limiting).
     *
     * @return true if we can send a request
     */
    private boolean canSendRequest() {
        long currentTime = System.currentTimeMillis();
        long lastReset = this.lastMinuteReset.get();
        long timeSinceReset = currentTime - lastReset;

        // Reset counter if a minute has passed
        if (timeSinceReset >= 60000) {
            this.requestsThisMinute.set(0);
            this.lastMinuteReset.set(currentTime);
        }

        return this.requestsThisMinute.get() < this.maxRequestsPerMinute;
    }

    /**
     * Send an event to Google Analytics.
     *
     * @param modId The mod identifier
     * @param eventName The event name
     * @param eventParams Optional event parameters
     */
    public void sendEvent(@Nonnull String modId, @Nonnull String eventName, @Nullable Map<String, String> eventParams) {
        if (!this.isEnabled(modId)) {
            return;
        }

        if (!this.canSendRequest()) {
            System.out.println("[MGATracking] Rate limit reached, skipping event: " + eventName);
            return;
        }

        try {
            String clientId = this.getClientId(modId);
            String measurementId = this.getMeasurementId(modId);

            // Build the request URL (using only measurement_id, no api_secret)
            String url = GA_ENDPOINT + "?measurement_id=" + measurementId;

            // Build the request body
            JsonObject body = new JsonObject();
            body.addProperty("client_id", clientId);

            JsonArray eventsArray = new JsonArray();
            JsonObject event = new JsonObject();
            event.addProperty("name", eventName);

            if (eventParams != null && !eventParams.isEmpty()) {
                JsonObject params = new JsonObject();
                for (Map.Entry<String, String> entry : eventParams.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    // Ensure parameter names and values meet GA requirements
                    if (key.length() <= 40 && value != null && value.length() <= 100) {
                        params.addProperty(key, value);
                    }
                }
                event.add("params", params);
            }

            eventsArray.add(event);
            body.add("events", eventsArray);

            // Send the request
            HttpPost request = new HttpPost(URI.create(url));
            request.setEntity(new StringEntity(GSON.toJson(body), StandardCharsets.UTF_8));
            request.setHeader("Content-Type", "application/json");

            this.requestsThisMinute.incrementAndGet();

            try (CloseableHttpResponse response = this.httpClient.execute(request)) {
                int statusCode = response.getCode();
                if (statusCode >= 200 && statusCode < 300) {
                    // Success
                } else {
                    System.err.println("[MGATracking] Failed to send event: " + eventName + " (Status: " + statusCode + ")");
                }
            }
        } catch (IOException e) {
            System.err.println("[MGATracking] Error sending event to Google Analytics: " + e.getMessage());
        }
    }

    /**
     * Track a new installation of a mod.
     * This should be called when a mod is first installed on a server.
     *
     * @param modId The mod identifier
     */
    public void trackInstallation(@Nonnull String modId) {
        Map<String, String> params = new HashMap<>();
        params.put("mod_id", modId);
        params.put("event_category", "mod_installation");
        this.sendEvent(modId, "mod_installation", params);
    }

    /**
     * Track that a server is using the mod.
     * This should be called periodically or on server startup.
     *
     * @param modId The mod identifier
     */
    public void trackServerUsingMod(@Nonnull String modId) {
        Map<String, String> params = new HashMap<>();
        params.put("mod_id", modId);
        params.put("event_category", "server_usage");
        this.sendEvent(modId, "server_using_mod", params);
    }

    /**
     * Track server startup.
     *
     * @param serverIdentifier The server identifier
     */
    public void trackServerStartup(@Nonnull String serverIdentifier) {
        // Use server identifier as mod ID for default tracking
        this.trackServerUsingMod(serverIdentifier);
    }

    /**
     * Track the number of players connected to a server.
     *
     * @param modId The mod identifier
     * @param playerCount The current number of players connected
     */
    public void trackPlayerConnected(@Nonnull String modId, int playerCount) {
        Map<String, String> params = new HashMap<>();
        params.put("mod_id", modId);
        params.put("player_count", String.valueOf(playerCount));
        params.put("event_category", "player_metrics");
        this.sendEvent(modId, "player_connected", params);
    }

    /**
     * Track a custom event.
     *
     * @param modId The mod identifier
     * @param eventName The name of the event
     * @param eventParameters Optional parameters for the event
     */
    public void trackCustomEvent(@Nonnull String modId, @Nonnull String eventName, @Nullable Map<String, String> eventParameters) {
        Map<String, String> params = new HashMap<>();
        if (eventParameters != null) {
            params.putAll(eventParameters);
        }
        params.put("mod_id", modId);
        this.sendEvent(modId, eventName, params);
    }

    /**
     * Close the HTTP client and clean up resources.
     */
    public void shutdown() {
        try {
            if (this.httpClient != null) {
                this.httpClient.close();
            }
        } catch (IOException e) {
            System.err.println("[MGATracking] Error closing HTTP client: " + e.getMessage());
        }
    }
}
