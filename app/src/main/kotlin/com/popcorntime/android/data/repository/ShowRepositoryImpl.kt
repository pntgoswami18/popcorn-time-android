package com.popcorntime.android.data.repository

import com.popcorntime.android.data.api.ShowApiService
import com.popcorntime.android.data.api.dto.EpisodeDto
import com.popcorntime.android.data.api.dto.ShowDto
import com.popcorntime.android.data.db.dao.BookmarkedDao
import com.popcorntime.android.data.db.dao.WatchedDao
import com.popcorntime.android.data.db.entity.BookmarkedEntity
import com.popcorntime.android.data.db.entity.WatchedEntity
import com.popcorntime.android.domain.model.ContentType
import com.popcorntime.android.domain.model.Episode
import com.popcorntime.android.domain.model.EpisodeTorrent
import com.popcorntime.android.domain.model.Show
import com.popcorntime.android.domain.model.ShowFilter
import com.popcorntime.android.domain.repository.ShowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ShowRepositoryImpl @Inject constructor(
    private val api: ShowApiService,
    private val watchedDao: WatchedDao,
    private val bookmarkedDao: BookmarkedDao,
) : ShowRepository {

    override suspend fun getShows(filter: ShowFilter): Result<List<Show>> = runCatching {
        api.listShows(filter).map { it.toDomain() }
    }

    override suspend fun getShowDetail(imdbId: String, type: ContentType): Result<Show> =
        runCatching { api.getShowDetail(imdbId, type).toDomain() }

    override fun observeWatched(): Flow<Set<String>> = watchedDao.observeAll().map { it.toSet() }
    override fun observeBookmarked(): Flow<Set<String>> = bookmarkedDao.observeAll().map { it.toSet() }

    override suspend fun toggleWatched(imdbId: String) {
        if (watchedDao.isWatched(imdbId)) watchedDao.delete(imdbId)
        else watchedDao.insert(WatchedEntity(imdbId))
    }

    override suspend fun toggleBookmarked(imdbId: String) {
        if (bookmarkedDao.isBookmarked(imdbId)) bookmarkedDao.delete(imdbId)
        else bookmarkedDao.insert(BookmarkedEntity(imdbId))
    }
}

// ── Mappers ──────────────────────────────────────────────────────────────────

private fun ShowDto.toDomain() = Show(
    imdbId = imdbId,
    tvdbId = tvdbId,
    title = title,
    year = year,
    slug = slug,
    synopsis = synopsis,
    runtime = runtime,
    country = country,
    network = network,
    airDay = airDay,
    airTime = airTime,
    status = status,
    numSeasons = numSeasons,
    rating = rating.percentage / 10.0,
    genres = genres,
    posterUrl = images.poster,
    backdropUrl = images.fanart,
    bannerUrl = images.banner,
    episodes = episodes.map { it.toDomain() },
)

private fun EpisodeDto.toDomain() = Episode(
    tvdbId = tvdbId,
    season = season,
    episode = episode,
    title = title,
    overview = overview,
    firstAired = firstAired,
    torrents = torrents.mapValues { (_, t) ->
        EpisodeTorrent(url = t.url, seeds = t.seeds, peers = t.peers, provider = t.provider)
    },
)
