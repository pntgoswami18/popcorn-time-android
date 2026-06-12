# Popcorn Time Android

A native Android client for Popcorn Time — stream movies, TV series and anime via BitTorrent, with Trakt.tv sync, subtitles, and a personal library.

> **Status:** Phases 1–6 complete. Active development.

---

## Features

### Movies
- Browse movies powered by the [YTS API](https://yts.mx/api) with multi-server fallback
- Filter by genre, sort by rating/year/seeds/peers
- Search by title
- Detail page: backdrop, poster, rating, runtime, genres, synopsis
- Quality selector (4K / 1080p / 720p / 480p) with seed/peer counts
- Stream via BitTorrent — playback starts at 0.5% buffer, no full download required

### TV Series & Anime
- Browse series and anime via the TVMaze API (show/episode metadata) + EZTV (torrent magnets)
- Season/episode navigation with air dates and overviews
- Per-episode quality picker

### Player
- Full-screen ExoPlayer with play/pause, seek bar, volume
- Subtitles via [OpenSubtitles REST API v3](https://opensubtitles.stoplight.io/docs/opensubtitles-api) — searchable CC picker in-player
- Auto-mark watched on playback completion
- Background torrent download with foreground service notification
- **Cast to TV** — Chromecast (Google Cast SDK), Kodi JSON-RPC, DLNA renderers, or any external video player
- **Resume playback** — position saved every 10 s, restored on next open (skips resume if under 30 s)
- **Auto-play next episode** — 10-second countdown overlay; cancel at any time
- **Picture-in-Picture** — enters PiP automatically on home button; full overlay suppressed in PiP
- **Media Session** — lock-screen and notification transport controls via Media3
- **Aspect ratio / zoom** — cycle between Fit, Fill, Zoom, and Fixed-width modes
- **Brightness control** — in-player slider; restored to system brightness on exit
- **Audio track picker** — bottom sheet listing all tracks from the media container
- **Custom subtitle import** — load any local `.srt` file via the file picker
- **Subtitle styling** — font size, color, background opacity, and edge type
- **Local file playback** — open `.mp4`, `.mkv`, `.avi`, and other video files directly from device storage

### Personal Library
- **Favourites** — heart icon on any movie or show
- **Watchlist** — playlist icon to queue content for later
- **Watched history** — auto-populated from player, or mark manually
- **Bulk mark watched** — mark an entire series as watched in one tap from the show detail screen
- **Parental controls** — age-rating filter (G / PG / PG-13 / R) applied across movie and show browsers
- **Hide watched** — option to hide or fade already-watched titles in the browser
- All three lists persisted locally in Room

### Trakt.tv Sync
- Connect via device authorization flow (no browser redirect needed)
- Push/pull watch history, watchlist and favourites
- Manual sync via Library → Settings gear
- **Real-time scrobbling** — playback position pushed to Trakt while watching (start / pause / stop)
- Token persisted in DataStore; disconnect any time

### Downloads & Torrent
- **Offline download mode** — save any title to device storage for later playback
- **Download manager** — track progress, resume, cancel, and delete downloads from Settings
- **Speed limits** — configurable max download / upload speeds in Torrent Settings
- **Seeding ratio** — stop seeding automatically when the configured ratio is reached
- **Cache management** — clear torrent temp files from Settings

### Discovery
- **Ratings overlay** — IMDb rating badge on every movie and show thumbnail card
- **Random picker** — shuffle button to open a random title from the current browse list

### UI & Platform
- **Themes** — System default, Light, and Dark modes
- **Android TV / Fire TV layout** — permanent navigation drawer and D-pad friendly focus management; LEANBACK_LAUNCHER intent filter

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
| Casting | Google Cast SDK v21.5.0, NsdManager (DLNA), Kodi JSON-RPC |
| Minimum SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

---

## Architecture

```
app/
└── src/main/kotlin/com/popcorntime/android/
    ├── data/
    │   ├── api/          # Ktor API services (YTS, TVMaze, EZTV, OpenSubtitles)
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
        ├── files/        # Local file picker screen
        ├── library/      # Library tab (Favourites / Watchlist / Watched)
        ├── main/         # MainScreen, bottom navigation, NavHost
        ├── movies/       # Movie browser + detail
        ├── player/       # ExoPlayer screen (resume, PiP, Media Session, subtitles, audio)
        ├── settings/     # Appearance, Trakt, Torrent, Downloads settings screens
        ├── shows/        # Series/Anime browser + detail
        └── theme/        # Material 3 theme (System / Light / Dark)
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

### TV Series & Anime
- **TVMaze** (`https://api.tvmaze.com`) — free, no auth required; show and episode metadata including air dates, descriptions and artwork
- **EZTV** (`https://eztv.re/api`) — episode torrent magnet links indexed by IMDB ID; up to 300 torrents fetched per show

> The original Butter/api-fetch.sh servers (`tv-v2.api-fetch.sh`, `shows.api-fetch.website`) went offline in 2025 and have been fully replaced by the above.

### Subtitles
- [OpenSubtitles REST API v3](https://opensubtitles.stoplight.io/docs/opensubtitles-api)
- Subtitle search by IMDB ID; download via authenticated POST `/download`
- **Optional auth** — sign in with an OpenSubtitles account (Library → CC icon) to unlock higher download quotas and per-language preferences

### Additional Torrent Sources (Jackett / Prowlarr)
- Configure a self-hosted [Jackett](https://github.com/Jackett/Jackett) or [Prowlarr](https://github.com/Prowlarr/Prowlarr) instance under **Library → Source icon**
- When configured, movies use Jackett indexers (category 2000) and shows use TV indexers (category 5000) instead of YTS/EZTV
- Falls back to YTS / EZTV automatically if Jackett returns no results or times out

---

## Remote Control API (HTTPS)

The app exposes a local REST API on port **8889** when enabled (**Library → Remote icon**). Control playback from any script, dashboard, or automation tool on your LAN.

The feature is **off by default** and the toggle is persisted: the server runs if and only if remote control is enabled in settings, including across app restarts. Starting a torrent stream never starts the server on its own.

### TLS: self-signed certificate, trust-on-first-use

The server is **HTTPS-only** — there is no cleartext HTTP listener when the feature is enabled. On first start the app generates an EC key pair in the Android Keystore (the private key never leaves it) with a self-signed certificate valid for 10 years. The certificate is stable across app restarts and token regenerations, so a client that trusted it once keeps working.

Because the certificate is self-signed, clients can't validate it against a CA. Instead, the QR payload carries the certificate's SHA-256 fingerprint (`fp` fragment parameter, format `sha256:<64 hex chars>`) for **trust-on-first-use (TOFU)** pinning. The same fingerprint is shown in the Remote settings screen under **Advanced → Certificate fingerprint**.

- **Browsers** show a one-time "connection not private" warning. Verify the certificate's SHA-256 fingerprint against the one on the settings screen, then accept. Browsers cannot pin a fingerprint from the page itself, so this manual check *is* the TOFU step.
- **Verify from a terminal:**
  ```bash
  openssl s_client -connect 192.168.1.x:8889 </dev/null 2>/dev/null \
    | openssl x509 -noout -fingerprint -sha256
  ```
- **curl / scripts:** the certificate names the device `CN=PopcornTime Remote` with no IP SAN, so `--cacert` alone fails hostname verification. Pin the public key instead — curl enforces `--pinnedpubkey` even together with `-k`:
  ```bash
  # one-time: compute the SPKI pin from the live server
  PIN=$(openssl s_client -connect 192.168.1.x:8889 </dev/null 2>/dev/null \
    | openssl x509 -pubkey -noout \
    | openssl pkey -pubin -outform der \
    | openssl dgst -sha256 -binary | base64)
  curl -k --pinnedpubkey "sha256//$PIN" https://192.168.1.x:8889/status ...
  ```
  Plain `curl -k` without a pin skips verification entirely — it protects against passive eavesdropping only, not an active man-in-the-middle.

Security notes: TLS protects tokens and pairing codes from passive sniffing on the LAN. Protection against an *active* MITM depends on the client actually verifying the fingerprint (TOFU); a client that blindly accepts any certificate gets encryption but not server authentication. After an app uninstall/reinstall the keystore entry is recreated, so clients must re-verify and re-trust the new certificate.

### Remote control web page

The server also serves a built-in remote control web UI at `GET /` (and `/index.html`) — these two routes are **unauthenticated** so a browser can load the page; every API route still requires the bearer token.

The QR code shown while pairing encodes a plain URL carrying a short-lived pairing code and the TLS certificate fingerprint:

```
https://<ip>:8889/#code=<pairing-code>&fp=sha256:<hex>
```

Scan it with a phone camera to open the remote page in a browser (accept the self-signed-certificate warning on first connect — ideally after checking the fingerprint). The code and fingerprint ride in the URL fragment (never sent over the network to the server); the page exchanges the code for a session token via `POST /pair` after you approve the device on the phone, stores the token in `sessionStorage`, and sends it as `Authorization: Bearer <token>` on every API call. The manual/advanced form `https://<ip>:8889/#token=<token>` is still supported for persistent bearer tokens.

> **Breaking change (TLS):** the API moved from `http://` to `https://` — cleartext connections are refused. Re-scan the QR code (it now uses the `https` scheme and includes the `fp` fingerprint parameter) and accept the browser warning once. Scripts must point at `https://` and pin or trust the device certificate as described above.
>
> **Breaking change (older):** the QR code previously encoded a JSON object (`{"ip":...,"port":...,"token":...}`). Clients that parsed that JSON must now parse the URL above instead.

### API

Authentication: `Authorization: Bearer <token>` — token is shown in the Remote settings screen.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | Remote control web page (no auth) |
| `GET` | `/status` | Playback state, position, duration, queue |
| `POST` | `/play` | Resume playback |
| `POST` | `/pause` | Pause playback |
| `POST` | `/seek?position=<ms>` | Seek to position in milliseconds |
| `GET` | `/queue` | List queued items |
| `POST` | `/queue/add` | Enqueue a movie or episode (JSON body: `QueueItem`) |
| `DELETE` | `/queue/clear` | Clear the playback queue |

Example:
```bash
TOKEN="your-token-here"
# PIN computed once via openssl (see TLS section above)
curl -k --pinnedpubkey "sha256//$PIN" -H "Authorization: Bearer $TOKEN" https://192.168.1.x:8889/status
curl -k --pinnedpubkey "sha256//$PIN" -X POST -H "Authorization: Bearer $TOKEN" https://192.168.1.x:8889/pause
```

---

## Permissions

| Permission | Reason |
|-----------|--------|
| `INTERNET` | Stream torrents and fetch metadata |
| `FOREGROUND_SERVICE` | Keep torrent engine alive while playing |
| `FOREGROUND_SERVICE_DATA_SYNC` | Android 14+ classification for background data transfer |
| `WAKE_LOCK` | Prevent CPU sleep during active streaming |
| `CHANGE_NETWORK_STATE` | Required by Google Cast SDK |
| `ACCESS_WIFI_STATE` | Detect LAN IP for Kodi/DLNA stream URLs |

---

## Roadmap

| Phase | Status | Features |
|-------|--------|---------|
| 1 | ✅ Done | Movies browser, YTS API, TorrentEngine, ExoPlayer, subtitles |
| 2 | ✅ Done | Series + Anime tabs, episode navigation, filter/search, bottom nav |
| 3 | ✅ Done | Library tab, Favourites, Watchlist, Watched history, Trakt.tv sync |
| 4 | ✅ Done | Casting — Chromecast, External Player, Kodi/XBMC, DLNA |
| 5 | ✅ Done | Additional torrent sources (Jackett/Prowlarr), HTTP remote control API, full OpenSubtitles auth |
| 6 | ✅ Done | **Player UX:** Resume playback, auto-play next episode with countdown, aspect ratio / zoom, brightness control, audio track picker<br>**Subtitles:** Custom .srt import, subtitle styling (font, size, color, background)<br>**Content Discovery:** Ratings overlay on cards, random / shuffle picker, hide/fade watched in browser<br>**Library:** Bulk "mark series as watched", parental controls (age-rating filter)<br>**Download & Torrent:** Offline download mode, local file playback, speed limits, seeding ratio, cache management<br>**Trakt:** Real-time scrobbling (start / pause / stop)<br>**UI & Platform:** Themes (System / Light / Dark), Android TV / Fire TV layout, PiP, Media Session |

---

## Known Limitations

- **No code signing** — sideloaded APKs may require enabling "Install from unknown sources"
- **Trakt Client ID** — must be supplied by the developer before Trakt sync works (see setup above)
- **Room migration** — migrations 1→2 and 2→3 are in place; upgrading preserves all library data and adds the `downloads` table
- **YTS movie detail** — fetched via IMDB ID lookup; availability depends on YTS mirror uptime
- **Download mode** — a single torrent stream is active at a time; background downloads pause if a stream is started

---

## License

This project is for personal/educational use. It does not host or distribute any copyrighted content — all media data is fetched from third-party public APIs.
