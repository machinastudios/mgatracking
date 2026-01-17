package com.machina.gatracking;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.machina.gatracking.services.GATrackingService;
import com.machina.gatracking.storage.InstallationTracker;
import com.machina.shared.SuperPlugin;
import com.machina.shared.config.PluginConfig;

/**
 * The main class for the Google Analytics tracking system
 */
public class Main extends SuperPlugin {
    /**
     * The singleton instance of the Main class
     */
    @SuppressWarnings("null")
    @Nonnull
    public static Main INSTANCE;

    /**
     * The plugin configuration
     */
    public PluginConfig config = new PluginConfig(this, "config");

    /**
     * The Google Analytics tracking service
     */
    private GATrackingService trackingService;

    /**
     * The installation tracker for managing plugin installations
     */
    private InstallationTracker installationTracker;

    public Main(@Nonnull JavaPluginInit init) {
        super(init);
    }

    public void init() {
        // Load the configuration
        this.loadConfig();

        // Save the instance
        INSTANCE = this;

        // Initialize tracking service
        this.trackingService = new GATrackingService(this.config);

        // Initialize installation tracker
        this.installationTracker = new InstallationTracker(this.getConfigDirectory());

        // Setup events for tracking player connections
        if (this.config.getBoolean("tracking.autoTrackPlayers", true)) {
            this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, event -> {
                String modId = this.getManifest().getName();
                int playerCount = Universe.get().getPlayerCount();
                this.trackingService.trackPlayerConnected(modId, playerCount);
            });
        }

        // Track server startup
        this.trackServerStartup();
    }

    /**
     * Get the tracking service instance
     * @return The GATrackingService instance
     */
    @Nonnull
    public GATrackingService getTrackingService() {
        return this.trackingService;
    }

    /**
     * Get the installation tracker instance
     * @return The InstallationTracker instance
     */
    @Nonnull
    public InstallationTracker getInstallationTracker() {
        return this.installationTracker;
    }

    /**
     * Track server startup (this will be called when the plugin loads)
     */
    private void trackServerStartup() {
        if (this.config.getBoolean("tracking.autoTrackServerStartup", true)) {
            // This will track that a server using the mod has started
            // We use the plugin name as the mod identifier
            String modId = this.getManifest().getName();
            this.trackingService.trackServerUsingMod(modId);
        }
    }

    /**
     * Load the configuration
     */
    private void loadConfig() {
        // Google Analytics configuration
        this.config.addDefault("ga.enabled", true, "Whether Google Analytics tracking is enabled");
        this.config.addDefault("ga.measurementId", "", "Google Analytics Measurement ID (G-XXXXXXXXXX)");

        // Tracking configuration
        this.config.addDefault("tracking.autoTrackPlayers", true, "Whether to automatically track player connections");
        this.config.addDefault("tracking.autoTrackServerStartup", true, "Whether to automatically track server startup");

        // Rate limiting configuration
        this.config.addDefault("rateLimit.maxRequestsPerMinute", 20, "Maximum number of GA requests per minute");

        // Load the configuration
        this.config.load();
    }
}
