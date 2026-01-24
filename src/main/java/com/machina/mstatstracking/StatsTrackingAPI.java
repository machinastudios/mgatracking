package com.machina.mstatstracking;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.machina.mstatstracking.storage.InstallationTracker;

/**
 * Public API for PostHog tracking.
 * This class provides static methods for other mods to use for tracking events.
 */
public class StatsTrackingAPI {
    /**
     * Get a tracking helper instance for a specific mod.
     * This helper automatically uses the plugin's name as the mod ID.
     *
     * @param mod The mod instance
     * @return A ModTracker instance for the mod
     */
    @Nonnull
    public static PluginTracker forPlugin(@Nonnull JavaPlugin plugin) {
        return new PluginTracker(plugin);
    }

    /**
     * Helper class for tracking events for a specific plugin.
     * Automatically uses the plugin's name as the mod ID.
     */
    public static class PluginTracker {
        private final String modId;

        private PluginTracker(@Nonnull JavaPlugin plugin) {
            this.modId = plugin.getManifest().getName();
        }

        /**
         * Track a new installation of the plugin.
         */
        public void trackInstallation() {
            GATrackingAPI.trackInstallation(this.modId);
        }

        /**
         * Track that a server is using the plugin.
         */
        public void trackServerUsingMod() {
            GATrackingAPI.trackServerUsingMod(this.modId);
        }

        /**
         * Track the number of players connected to a server.
         *
         * @param playerCount The current number of players connected
         */
        public void trackPlayerCount(int playerCount) {
            GATrackingAPI.trackPlayerCount(this.modId, playerCount);
        }

        /**
         * Track a custom event.
         *
         * @param eventName The name of the event
         * @param eventParameters Optional parameters for the event (can be null)
         */
        public void trackCustomEvent(@Nonnull String eventName, @Nullable Map<String, String> eventParameters) {
            GATrackingAPI.trackCustomEvent(this.modId, eventName, eventParameters);
        }

        /**
         * Get the mod ID used by this tracker.
         *
         * @return The mod ID
         */
        @Nonnull
        public String getModId() {
            return this.modId;
        }

        /**
         * Initialize tracking for this plugin.
         * This method automatically handles:
         * - First-time installation tracking
         * - Server usage tracking
         * 
         * Call this once in your plugin's init() method.
         */
        public void init() {
            if (Main.INSTANCE == null || Main.INSTANCE.getTrackingService() == null) {
                return;
            }

            InstallationTracker installationTracker = Main.INSTANCE.getInstallationTracker();

            // Check if this is a first-time installation
            if (!installationTracker.isInstalled(this.modId)) {
                // Track the installation
                GATrackingAPI.trackInstallation(this.modId);
                // Mark as installed
                installationTracker.markAsInstalled(this.modId);
            }

            // Track that the server is using this mod
            this.trackServerUsingMod();
        }
    }
    /**
     * Track a new installation of a mod.
     * This should be called when a mod is first installed on a server.
     *
     * @param modId The unique identifier for the mod (e.g., "mauth", "meconomy")
     */
    public static void trackInstallation(@Nonnull String modId) {
        if (Main.INSTANCE != null && Main.INSTANCE.getTrackingService() != null) {
            Main.INSTANCE.getTrackingService().trackInstallation(modId);
        }
    }

    /**
     * Track that a server is using the mod.
     * This should be called periodically or on server startup.
     *
     * @param modId The unique identifier for the mod
     */
    public static void trackServerUsingMod(@Nonnull String modId) {
        if (Main.INSTANCE != null && Main.INSTANCE.getTrackingService() != null) {
            Main.INSTANCE.getTrackingService().trackServerUsingMod(modId);
        }
    }

    /**
     * Track the number of players connected to a server.
     *
     * @param modId The unique identifier for the mod
     * @param playerCount The current number of players connected
     */
    public static void trackPlayerCount(@Nonnull String modId, int playerCount) {
        if (Main.INSTANCE != null && Main.INSTANCE.getTrackingService() != null) {
            Main.INSTANCE.getTrackingService().trackPlayerConnected(modId, playerCount);
        }
    }

    /**
     * Track a custom event.
     *
     * @param modId The unique identifier for the mod
     * @param eventName The name of the event
     * @param eventParameters Optional parameters for the event (can be null)
     */
    public static void trackCustomEvent(@Nonnull String modId, @Nonnull String eventName, @Nullable java.util.Map<String, String> eventParameters) {
        if (Main.INSTANCE != null && Main.INSTANCE.getTrackingService() != null) {
            Main.INSTANCE.getTrackingService().trackCustomEvent(modId, eventName, eventParameters);
        }
    }

    /**
     * Check if tracking is enabled and available.
     *
     * @return true if tracking is enabled and the service is initialized
     */
    public static boolean isTrackingAvailable() {
        return Main.INSTANCE != null 
            && Main.INSTANCE.getTrackingService() != null 
            && Main.INSTANCE.getTrackingService().isEnabled();
    }

    /**
     * Get the tracking service instance (for advanced usage).
     * Prefer using static methods instead when possible.
     *
     * @return The GATrackingService instance, or null if not initialized
     */
    @Nullable
    public static GATrackingService getService() {
        if (Main.INSTANCE != null) {
            return Main.INSTANCE.getTrackingService();
        }
        return null;
    }
}
