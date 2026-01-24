package com.machina.mstatstracking;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.machina.mstatstracking.services.MStatsTrackingService;
import com.machina.mstatstracking.storage.InstallationTracker;
import com.machina.shared.SuperPlugin;
import com.machina.shared.config.ConfigurationFile;

/**
 * The main class for the PostHog tracking system
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
    public ConfigurationFile config = new ConfigurationFile(this, "config");

    /**
     * The PostHog tracking service
     */
    private MStatsTrackingService trackingService;

    /**
     * The installation tracker for managing mod installations
     */
    private InstallationTracker installationTracker;

    public Main(@Nonnull JavaPluginInit init) {
        super(init);
    }

    public void start() {
        // Load the configuration
        this.loadConfig();

        // Save the instance
        INSTANCE = this;

        // Initialize tracking service
        this.trackingService = new MStatsTrackingService(this.config);

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
    public MStatsTrackingService getTrackingService() {
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
        // PostHog configuration
        this.config.addDefault("posthog.enabled", true, "Whether PostHog tracking is enabled");
        this.config.addDefault("posthog.apiKey", "", "PostHog API Key");

        // Tracking configuration
        this.config.addDefault("tracking.features.playerConnections", true, "Whether to automatically track player connections");
        this.config.addDefault("tracking.features.serverLifecyle", true, "Whether to automatically track server lifecycle (startup and shutdown)");
        this.config.addDefault("tracking.features.vmStats", true, "Whether to automatically track VM stats (memory, CPU, etc.)");

        // Load the configuration
        this.config.load();
    }
}
