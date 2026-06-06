# Popcorn Time Android

A native Android client for Popcorn Time — stream movies, TV series and anime via BitTorrent, with Trakt.tv sync, subtitles, and a personal library.

> **Status:** Phases 1–3 complete. Active development.

---

## Features

### Movies
- Browse movies powered by the [YTS API](https://yts.mx/api) with multi-server fallback
- Filter by genre, sort by rating/year/seeds/peers
- Search by title
- Detail page: backdrop, poster, rating, runtime, genres, synopsis
- Quality selector (4K / 1080p / 720p / 480p) with seed/peer counts
- Stream via BitTorrent — playback starts at 3% buffer, no full download required

### TV Series & Anime
- Browse series and anime via the Popcorn Time TV API
- Season/episode navigation with air dates and overviews
- Per-episode quality picker

### Player
- Full-screen ExoPlayer with play/pause, seek bar, volume
- Subtitles via [OpenSubtitles REST API v3](https://opensubtitles.stoplight.io/docs/opensubtitles-api) — searchable CC picker in-player
- Auto-mark watched on playback completion
- Background torrent download with foreground service notification

### Personal Library
- **Favourites** — heart icon on any movie or show
- **Watchlist** — playlist icon to queue content for later
- **Watched history** — auto-populated from player, or mark manually
- All three lists persisted locally in Room

### Trakt.tv Sync
- Connect via device authorization flow (no browser redirect needed)
- Push/pull watch history, watchlist and favourites
- Manual sync via Library → Settings gear
- Token persisted in DataStore; disconnect any time

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture (domain / data / ui layers) |
| DI | Hilt |
| Navigation | Jetpack Navigation Compose |
| Local DB | Room |
| Networking | Ktor Client + kotlinx.serialization |
| Image loading | Coil |
| Video playback | ExoPlayer / Media3 |
| Torrent engine | libtorrent4j (native JNI) |
| Local HTTP server | NanoHTTPD (byte-range, serves torrent to ExoPlayer) |
| Token storage | DataStore Preferences |
| Minimum SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

---

## Architecture

```
app/
└── src/main/kotlin/com/popcorntime/android/
    ├── data/
    │   ├── api/          # Ktor API services (YTS, Popcorn Time TV, OpenSubtitles)
    │   │   └── dto/      # Serializable response DTOs
    │   ├── db/           # Room database, DAOs, entities
    │   ├── repository/   # Repository implementations
    │   ├── subtitles/    # OpenSubtitles service
    │   ├── torrent/      # TorrentEngine, TorrentStreamServer, TorrentService
    │   └── trakt/        # Trakt auth, sync, token store
    ├── di/               # Hilt modules (AppModule, DatabaseModule, TraktModule, ...)
    ├── domain/
    │   ├── model/        # Pure Kotlin domain models (Movie, Show, LibraryItem, ...)
    │   └── repository/   # Repository interfaces
    └── ui/
        ├── library/      # Library tab (Favourites / Watchlist / Watched)
        ├── main/         # MainScreen, bottom navigation, NavHost
        ├── movies/       # Movie browser + detail
        ├── player/       # ExoPlayer screen
        ├── settings/     # Trakt settings screen
        ├── shows/        # Series/Anime browser + detail
        └── theme/        # Material 3 theme
```

### Key data flows

**Streaming pipeline**
```
Magnet/torrent URL
  → libtorrent4j SessionManager (sequential download, prioritise largest file)
  → NanoHTTPD localhost:8888 (byte-range HTTP)
  → ExoPlayer MediaItem
  → PlayerScreen
```

**Multi-server fallback (movies & shows)**
```
ArrayDeque<String> of shuffled servers
  → try first server
  → on success: promote server to front
  → on failure: rotate to back, try next
```

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK with API 34

### Clone & build

```bash
git clone https://github.com/pntgoswami18/popcorn-time-android.git
cd popcorn-time-android
```

Open in Android Studio and let Gradle sync. Then run on a device or emulator (API 26+).

### Trakt.tv setup (optional)

1. Create a free app at [trakt.tv/oauth/applications](https://trakt.tv/oauth/applications)
2. Copy your **Client ID**
3. Open `app/build.gradle.kts` and replace the placeholder:
   ```kotlin
   buildConfigField("String", "TRAKT_CLIENT_ID", "\"YOUR_TRAKT_CLIENT_ID_HERE\"")
   ```
4. Rebuild. You can now connect via **Library → Settings gear → Connect to Trakt.tv**

> Without a Client ID the app works fully offline — Trakt sync features are silently skipped.

---

## API Servers

### Movies (YTS)
The app ships with five pre-configured movie API mirrors. They are shuffled on startup and rotated automatically on failure:

```
https://fusme.link/
https://jfper.link/
https://uxert.link/
https://yrkde.link/
https://yts.bz/
```

### TV Series & Anime (Popcorn Time API)
```
https://tv-v2.api-fetch.sh/
https://shows.api-fetch.website/
```

### Subtitles
- [OpenSubtitles REST API v3](https://opensubtitles.stoplight.io/docs/opensubtitles-api)
- Subtitle search by IMDB ID; download via authenticated POST `/download`

---

## Permissions

| Permission | Reason |
|-----------|--------|
| `INTERNET` | Stream torrents and fetch metadata |
| `FOREGROUND_SERVICE` | Keep torrent engine alive while playing |
| `FOREGROUND_SERVICE_DATA_TRANSFER` | Android 14+ classification for network data transfer |
| `WAKE_LOCK` | Prevent CPU sleep during active streaming |

---

## Roadmap

| Phase | Status | Features |
|-------|--------|---------|
| 1 | ✅ Done | Movies browser, YTS API, TorrentEngine, ExoPlayer, subtitles |
| 2 | ✅ Done | Series + Anime tabs, episode navigation, filter/search, bottom nav |
| 3 | ✅ Done | Library tab, Favourites, Watchlist, Watched history, Trakt.tv sync |
| 4 | Planned | Casting — Chromecast, DLNA, AirPlay, Kodi/XBMC |
| 5 | Planned | Additional torrent sources, HTTP remote control API, full OpenSubtitles auth |
| 6 | Planned | Themes, Android TV / Fire TV layout, PiP mode, Media Session |

---

## Known Limitations

- **No code signing** — sideloaded APKs may require enabling "Install from unknown sources"
- **Trakt Client ID** — must be supplied by the developer before Trakt sync works (see setup above)
- **Room migration** — upgrading from an older build destroys local library data (destructive migration in development builds). A proper `Migration(1, 2)` will be added before any public release.
- **YTS movie detail** — fetched via IMDB ID lookup; availability depends on YTS mirror uptime

---

## License

This project is for personal/educational use. It does not host or distribute any copyrighted content — all media data is fetched from third-party public APIs.
