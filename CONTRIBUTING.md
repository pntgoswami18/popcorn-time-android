# Contributing

## Branch naming

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feature/<short-description>` | `feature/chromecast-casting` |
| Bug fix | `fix/<short-description>` | `fix/subtitle-download-crash` |
| Phase | `phase/<n>-<description>` | `phase/4-casting` |

## Commit style

Use the conventional format: `type: short description`

```
feat: add Chromecast support to player screen
fix: prevent ExoPlayer release on subtitle URL change
refactor: extract TorrentMonitor from TorrentEngine
docs: update README with Phase 4 roadmap
```

## Code style

- Kotlin only
- Jetpack Compose for all UI — no XML layouts
- MVVM + Clean Architecture: domain models stay pure Kotlin, no Android imports
- Hilt for all dependency injection — no manual `getInstance()` singletons
- One `@Singleton` binding per type (either `@Inject constructor` OR `@Provides`, never both)
- `flatMapLatest` for chaining Flows — never call suspending functions inside `Flow.map {}`
- `ConcurrentHashMap` for any cache accessed from multiple threads

## Adding a new API server

Movie servers live in `AppModule.provideMovieServers()`.  
Show servers live in `AppModule.provideShowServers()`.  
Both are `ArrayDeque<String>` — add the new URL with a trailing slash.

## Room migrations

The database is currently at version **2**. When adding new entities or columns:
1. Bump `version` in `AppDatabase.kt`
2. Write a `Migration(old, new)` object
3. Add it to the builder in `DatabaseModule` **before** removing `fallbackToDestructiveMigration()` from development builds
