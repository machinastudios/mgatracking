# MGATracking

Google Analytics tracking API for Hytale mods. This plugin-API allows other mods to send custom events to Google Analytics, tracking metrics like server usage, player counts, and installations.

## Features

- **Server Usage Tracking**: Track how many servers are using your mod
- **Player Metrics**: Track the number of players connected to servers
- **Installation Tracking**: Track when your mod is first installed
- **Custom Events**: Send custom events with custom parameters
- **Rate Limiting**: Built-in rate limiting to prevent API abuse
- **Per-Mod Configuration**: Each mod can have its own Google Analytics ID

## Installation

1. Place `mgatracking.jar` in your Hytale server's mods directory
2. Configure your Google Analytics credentials (see Configuration)
3. Other mods can now use the API by adding mgatracking as a dependency

## Configuration

Edit `config/com.machina/mgatracking/config.json5`:

```json5
{
  // Global Google Analytics configuration (default for all mods)
  ga: {
    enabled: true,
    measurementId: "G-XXXXXXXXXX"
  },

  // Per-mod configuration (optional - mods can override global settings)
  mods: {
    "mauth": {
      measurementId: "G-MOD_SPECIFIC_ID"
    }
  },

  // Tracking configuration
  tracking: {
    autoTrackPlayers: true,
    autoTrackServerStartup: true
  },

  // Rate limiting
  rateLimit: {
    maxRequestsPerMinute: 20
  }
}
```

### Getting Google Analytics Measurement ID

1. Go to Google Analytics → Admin → Data Streams
2. Select your stream (or create one)
3. Copy the Measurement ID (format: G-XXXXXXXXXX)

## Usage in Other Mods

### As a Maven Dependency

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.machina</groupId>
    <artifactId>mgatracking</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### API Usage Examples

#### Using the Plugin Helper (Recommended)

```java
import com.machina.gatracking.GATrackingAPI;

public class Main extends SuperPlugin {
    private GATrackingAPI.PluginTracker tracker;

    public void init() {
        // Get tracker for this plugin (automatically uses plugin name as mod ID)
        this.tracker = GATrackingAPI.forPlugin(this);

        // Initialize tracking - automatically handles installation and server usage tracking
        // This will:
        // 1. Track installation if it's the first time (saved automatically)
        // 2. Track that server is using this mod
        this.tracker.init();

        // Track player count (call when player count changes)
        int currentPlayers = Universe.get().getPlayerCount();
        this.tracker.trackPlayerCount(currentPlayers);

        // Track a custom event
        Map<String, String> params = new HashMap<>();
        params.put("feature", "two_factor_auth");
        params.put("action", "enabled");
        this.tracker.trackCustomEvent("feature_used", params);
    }
}
```

#### Using Static Methods

```java
import com.machina.gatracking.GATrackingAPI;

// Track a new installation (manual - prefer using forPlugin().init() instead)
GATrackingAPI.trackInstallation("mauth");

// Track that a server is using your mod
GATrackingAPI.trackServerUsingMod("mauth");

// Track player count (call when player count changes)
int currentPlayers = Universe.get().getPlayerCount();
GATrackingAPI.trackPlayerCount("mauth", currentPlayers);

// Track a custom event
Map<String, String> params = new HashMap<>();
params.put("feature", "two_factor_auth");
params.put("action", "enabled");
GATrackingAPI.trackCustomEvent("mauth", "feature_used", params);

// Check if tracking is available
if (GATrackingAPI.isTrackingAvailable()) {
    // Tracking is enabled and ready
}
```

## Tracked Metrics

The plugin tracks the following metrics:

1. **Server Usage** (`server_using_mod` event)
   - Tracks when a server starts using the mod
   - Includes `mod_id` parameter

2. **Player Connections** (`player_connected` event)
   - Tracks when players connect to the server
   - Includes `mod_id` and `player_count` parameters

3. **Installations** (`mod_installation` event)
   - Tracks when a mod is first installed
   - Includes `mod_id` parameter

4. **Custom Events**
   - Any custom event sent via `trackCustomEvent()`
   - Always includes `mod_id` parameter

## Event Structure

All events sent to Google Analytics include:
- `mod_id`: The identifier of the mod sending the event
- `event_category`: Category grouping (e.g., "mod_installation", "server_usage", "player_metrics")

Additional parameters are included based on the event type.

## Rate Limiting

The plugin implements rate limiting to prevent API abuse. By default, it allows up to 20 requests per minute. This can be configured in `config.json5`.

## Troubleshooting

### Events not appearing in Google Analytics

1. Check that `ga.enabled` is `true` in config
2. Verify `measurementId` is correct (format: G-XXXXXXXXXX)
3. Check server logs for error messages
4. Ensure you're looking at the correct GA property

### Rate limit errors

If you see "Rate limit reached" messages, increase `rateLimit.maxRequestsPerMinute` in config.

## License

This project is part of the Machina Studios mod collection for Hytale.
