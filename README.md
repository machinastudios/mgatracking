<div align="center">

# MStatsTracking

> Google Analytics tracking API for Hytale mods

**Plugin API for custom GA4 events: server usage, player counts, installations, and per-mod measurement IDs.**

[![Java](https://img.shields.io/badge/Language-Java-orange.svg)](#)
[![Hytale](https://img.shields.io/badge/Platform-Hytale-purple.svg)](#)

[Overview](#overview) • [Configuration](#configuration) • [API usage](#usage-in-other-mods) • [Troubleshooting](#troubleshooting)

</div>

---

## Overview

MStatsTracking lets Hytale mods send custom events to Google Analytics 4. Track server adoption, connected players, first-time installs, and arbitrary feature events with built-in rate limiting and per-mod measurement ID overrides.

Other mods depend on `mgatracking` and call `GATrackingAPI` at runtime.

---

## Installation

1. Place `mgatracking.jar` in the server's `mods` directory.
2. Configure Google Analytics credentials (see [Configuration](#configuration)).
3. Depend on the API from your mod's `pom.xml`.

---

## Configuration

Edit `config/com.machina/mgatracking/config.json5`:

```json5
{
  ga: {
    enabled: true,
    measurementId: "G-XXXXXXXXXX"
  },
  mods: {
    "mauth": {
      measurementId: "G-MOD_SPECIFIC_ID"
    }
  },
  tracking: {
    autoTrackPlayers: true,
    autoTrackServerStartup: true
  },
  rateLimit: {
    maxRequestsPerMinute: 20
  }
}
```

### Measurement ID

1. Google Analytics - Admin - Data Streams
2. Select or create a stream
3. Copy Measurement ID (`G-XXXXXXXXXX`)

---

## Usage in other mods

### Maven dependency

```xml
<dependency>
    <groupId>com.machina</groupId>
    <artifactId>mgatracking</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Plugin helper (recommended)

```java
import com.machina.gatracking.GATrackingAPI;

public class Main extends SuperPlugin {
    private GATrackingAPI.PluginTracker tracker;

    public void init() {
        this.tracker = GATrackingAPI.forPlugin(this);
        this.tracker.init();

        int currentPlayers = Universe.get().getPlayerCount();
        this.tracker.trackPlayerCount(currentPlayers);

        Map<String, String> params = new HashMap<>();
        params.put("feature", "two_factor_auth");
        params.put("action", "enabled");
        this.tracker.trackCustomEvent("feature_used", params);
    }
}
```

### Static API

```java
GATrackingAPI.trackInstallation("mauth");
GATrackingAPI.trackServerUsingMod("mauth");
GATrackingAPI.trackPlayerCount("mauth", playerCount);
GATrackingAPI.trackCustomEvent("mauth", "feature_used", params);

if (GATrackingAPI.isTrackingAvailable()) {
    // ready
}
```

---

## Tracked metrics

| Event | Description |
| ----- | ----------- |
| `server_using_mod` | Server started with the mod (`mod_id`) |
| `player_connected` | Player count snapshot (`mod_id`, `player_count`) |
| `mod_installation` | First install detected (`mod_id`) |
| Custom | Via `trackCustomEvent()` with extra parameters |

All events include `mod_id` and `event_category`.

Default rate limit: 20 requests/minute (configurable).

---

## Troubleshooting

| Issue | Check |
| ----- | ----- |
| Events missing in GA | `ga.enabled`, correct `measurementId`, server logs |
| Rate limit messages | Increase `rateLimit.maxRequestsPerMinute` |

---

## License

Part of the Machina Studios mod collection for Hytale.
