package com.popcorntime.android.data.repository

import com.popcorntime.android.data.db.dao.BookmarkedDao
import com.popcorntime.android.data.db.dao.LibraryItemDao
import com.popcorntime.android.data.db.dao.WatchedDao
import com.popcorntime.android.data.db.dao.WatchlistDao
import com.popcorntime.android.data.db.entity.BookmarkedEntity
import com.popcorntime.android.data.db.entity.LibraryItemEntity
import com.popcorntime.android.data.db.entity.WatchedEntity
import com.popcorntime.android.data.db.entity.WatchlistEntity
import com.popcorntime.android.data.trakt.TraktSyncService
import com.popcorntime.android.data.trakt.TraktTokenStore
import com.popcorntime.android.domain.model.LibraryContentType
import com.popcorntime.android.domain.model.LibraryItem
import com.popcorntime.android.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val bookmarkedDao: BookmarkedDao,
    private val watchedDao: WatchedDao,
    private val watchlistDao: WatchlistDao,
    private val libraryItemDao: LibraryItemDao,
    private val traktSyncService: TraktSyncService,
    private val traktTokenStore: TraktTokenStore,
) : LibraryRepository {

    // ── Favourites ────────────────────────────────────────────────────────────

    override fun observeFavourites(): Flow<List<LibraryItem>> =
        bookmarkedDao.observeAll().flatMapLatest { imdbIds ->
            if (imdbIds.isEmpty()) flowOf(emptyList())
            else libraryItemDao.getByIds(imdbIds).map { list -> list.map { it.toDomain() } }
        }

    override suspend fun isFavourited(imdbId: String): Boolean =
        bookmarkedDao.isBookmarked(imdbId)

    override suspend fun toggleFavourite(imdbId: String, metadata: LibraryItem) {
        libraryItemDao.upsert(metadata.toEntity())
        if (bookmarkedDao.isBookmarked(imdbId)) {
            bookmarkedDao.delete(imdbId)
            traktSyncService.removeFavourite(imdbId, metadata.contentType)
        } else {
            bookmarkedDao.insert(BookmarkedEntity(imdbId))
            traktSyncService.pushFavourite(imdbId, metadata.contentType)
        }
    }

    // ── Watchlist ─────────────────────────────────────────────────────────────

    override fun observeWatchlist(): Flow<List<LibraryItem>> =
        watchlistDao.observeAll().flatMapLatest { entities ->
            val ids = entities.map { it.imdbId }
            if (ids.isEmpty()) flowOf(emptyList())
            else libraryItemDao.getByIds(ids).map { list -> list.map { it.toDomain() } }
        }

    override suspend fun isInWatchlist(imdbId: String): Boolean =
        watchlistDao.isInWatchlist(imdbId)

    override suspend fun addToWatchlist(imdbId: String, metadata: LibraryItem) {
        libraryItemDao.upsert(metadata.toEntity())
        watchlistDao.insert(WatchlistEntity(imdbId))
        traktSyncService.pushToWatchlist(imdbId, metadata.contentType)
    }

    override suspend fun removeFromWatchlist(imdbId: String) {
        watchlistDao.delete(imdbId)
        val entity = libraryItemDao.getById(imdbId)
        val contentType = entity?.let {
            when (it.contentType) {
                "movie" -> LibraryContentType.MOVIE
                "anime" -> LibraryContentType.ANIME
                else -> LibraryContentType.SHOW
            }
        } ?: LibraryContentType.MOVIE
        traktSyncService.removeFromWatchlist(imdbId, contentType)
    }

    // ── Watched ───────────────────────────────────────────────────────────────

    override fun observeWatched(): Flow<List<LibraryItem>> =
        watchedDao.observeAll().flatMapLatest { imdbIds ->
            if (imdbIds.isEmpty()) flowOf(emptyList())
            else libraryItemDao.getByIds(imdbIds).map { list -> list.map { it.toDomain() } }
        }

    override suspend fun isWatched(imdbId: String): Boolean =
        watchedDao.isWatched(imdbId)

    override suspend fun markWatched(imdbId: String, metadata: LibraryItem) {
        libraryItemDao.upsert(metadata.toEntity())
        watchedDao.insert(WatchedEntity(imdbId))
        traktSyncService.pushWatched(imdbId, System.currentTimeMillis(), metadata.contentType)
    }

    override suspend fun unmarkWatched(imdbId: String) {
        watchedDao.delete(imdbId)
        val entity = libraryItemDao.getById(imdbId)
        val contentType = entity?.let {
            when (it.contentType) {
                "movie" -> LibraryContentType.MOVIE
                "anime" -> LibraryContentType.ANIME
                else -> LibraryContentType.SHOW
            }
        } ?: LibraryContentType.MOVIE
        try {
            traktSyncService.removeWatched(imdbId, contentType)
        } catch (_: Exception) {
            // Trakt failure does not prevent local deletion
        }
    }

    // ── Trakt sync ────────────────────────────────────────────────────────────

    override suspend fun syncFromTrakt() {
        if (!traktTokenStore.isLoggedIn()) return
        // Pull watch history — insert any imdbIds not already watched locally
        val remoteWatched = traktSyncService.pullWatchHistory()
        remoteWatched.forEach { imdbId ->
            if (!watchedDao.isWatched(imdbId)) {
                watchedDao.insert(WatchedEntity(imdbId, System.currentTimeMillis()))
                // Upsert a minimal LibraryItemEntity so the Library UI can display this item
                libraryItemDao.upsert(LibraryItemEntity(imdbId = imdbId, title = "", posterUrl = "", year = "", contentType = "movie"))
            }
        }
        // Pull watchlist
        val remoteWatchlist = traktSyncService.pullWatchlist()
        remoteWatchlist.forEach { imdbId ->
            if (!watchlistDao.isInWatchlist(imdbId)) {
                watchlistDao.insert(WatchlistEntity(imdbId))
                libraryItemDao.upsert(LibraryItemEntity(imdbId = imdbId, title = "", posterUrl = "", year = "", contentType = "movie"))
            }
        }
        // Pull favourites
        val remoteFavourites = traktSyncService.pullFavourites()
        remoteFavourites.forEach { imdbId ->
            if (!bookmarkedDao.isBookmarked(imdbId)) {
                bookmarkedDao.insert(BookmarkedEntity(imdbId))
                libraryItemDao.upsert(LibraryItemEntity(imdbId = imdbId, title = "", posterUrl = "", year = "", contentType = "movie"))
            }
        }
    }

    override fun isTraktConnected(): Flow<Boolean> = traktTokenStore.isTraktConnected()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun LibraryItem.toEntity() = LibraryItemEntity(
        imdbId = imdbId,
        title = title,
        posterUrl = posterUrl,
        year = year,
        contentType = when (contentType) {
            LibraryContentType.MOVIE -> "movie"
            LibraryContentType.SHOW -> "show"
            LibraryContentType.ANIME -> "anime"
        },
        addedAt = addedAt,
    )

    private fun LibraryItemEntity.toDomain() = LibraryItem(
        imdbId = imdbId,
        title = title,
        posterUrl = posterUrl,
        year = year,
        contentType = when (contentType) {
            "movie" -> LibraryContentType.MOVIE
            "anime" -> LibraryContentType.ANIME
            else -> LibraryContentType.SHOW
        },
        addedAt = addedAt,
    )
}
