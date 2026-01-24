package com.machina.mstatstracking.services;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import com.machina.shared.config.ConfigurationFile;

import main.java.com.machina.mstatstracking.services.TelemetryClient;

/**
 * Service for sending events to PostHog API.
 * Tracks metrics like server usage, player counts, and installations per mod ID.
 */
public class MStatsTrackingService {
    private final ConfigurationFile config;
    private final CloseableHttpClient httpClient;
    private final Map<String, String> modClientIds = new ConcurrentHashMap<>();
    private final AtomicInteger requestsThisMinute = new AtomicInteger(0);
    private final AtomicLong lastMinuteReset = new AtomicLong(System.currentTimeMillis());

    private final boolean enabled;
    private final String apiKey;

    private final TelemetryClient client;

    /**
     * Create a new MStatsTrackingService instance.
     *
     * @param config The plugin configuration
     */
    public MStatsTrackingService(@Nonnull ConfigurationFile config) {
        this.config = config;
        this.httpClient = HttpClients.createDefault();
        this.enabled = config.getBoolean("posthog.enabled", true);
        this.apiKey = config.getString("posthog.apiKey", "");

        // Validate configuration
        if (this.enabled && this.apiKey.isEmpty()) {
            System.err.println("[MStatsTracking] Warning: PostHog tracking is enabled but API key is not configured!");
        }

        // Create the telemetry client
        this.client = new TelemetryClient(this.apiKey, this.enabled);
    }

    /**
     * Get the API key for a specific mod, or the global one if not configured.
     *
     * @param modId The mod identifier
     * @return The API key
     */
    @Nonnull
    private String getApiKey(@Nonnull String modId) {
        String modIdValue = this.config.getString("mods." + modId + ".apiKey", "");
        return modIdValue.isEmpty() ? this.apiKey : modIdValue;
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

        String apiKey = this.getApiKey(modId);
        return !apiKey.isEmpty();
    }

    /**
     * Check if tracking is enabled (global check).
     *
     * @return true if tracking is enabled and configured
     */
    public boolean isEnabled() {
        return this.enabled && !this.apiKey.isEmpty();
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
     * Track a new installation of a mod.
     * This should be called when a mod is first installed on a server.
     *
     * @param modId The mod identifier
     */
    public void trackInstallation(@Nonnull String modId) {
        client.sendInstall(modId);
    }

    /**
     * Track that a server is using the mod.
     * This should be called periodically or on server startup.
     *
     * @param modId The mod identifier
     */
    public void trackServerUsingMod(@Nonnull String modId) {
        client.sendHeartbeat(modId);
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
        client.sendPlayersOnline(modId, playerCount);
    }

    /**
     * Track a custom event.
     *
     * @param modId The mod identifier
     * @param eventName The name of the event
     * @param eventParameters Optional parameters for the event
     */
    public void trackCustomEvent(@Nonnull String modId, @Nonnull String eventName, @Nullable Map<String, String> eventParameters) {
        client.sendCustomEvent(modId, eventName, eventParameters);
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
            System.err.println("[MStatsTracking] Error closing HTTP client: " + e.getMessage());
        }
    }
}
