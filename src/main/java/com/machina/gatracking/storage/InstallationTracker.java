package com.machina.gatracking.storage;

import java.nio.file.Path;

import javax.annotation.Nonnull;

import com.machina.shared.config.PluginConfig;

/**
 * Tracks which plugins have been installed (first-time initialization).
 * Persists installation state to a separate configuration file.
 */
public class InstallationTracker {
    private static final String INSTALLATIONS_FILE = "installations.json5";
    private final PluginConfig config;

    /**
     * Create a new InstallationTracker.
     *
     * @param dataDirectory The data directory where the installations file will be stored
     */
    public InstallationTracker(@Nonnull Path dataDirectory) {
        this.config = new PluginConfig(dataDirectory, INSTALLATIONS_FILE);
        this.config.load();
    }

    /**
     * Check if a plugin has been installed before.
     *
     * @param modId The mod identifier
     * @return true if the plugin has been installed before, false if it's a first-time installation
     */
    public boolean isInstalled(@Nonnull String modId) {
        return this.config.getBoolean("installed." + modId, false);
    }

    /**
     * Mark a plugin as installed.
     *
     * @param modId The mod identifier
     */
    public void markAsInstalled(@Nonnull String modId) {
        this.config.setBoolean("installed." + modId, true);
        this.config.save();
    }
}
