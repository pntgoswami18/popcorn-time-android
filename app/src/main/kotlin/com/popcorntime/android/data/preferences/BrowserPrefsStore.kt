package com.popcorntime.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class BrowserPrefsStore @Inject constructor(
    @Named("browserPrefsDataStore") private val prefs: DataStore<Preferences>,
) {
    private val HIDE_WATCHED_MOVIES = booleanPreferencesKey("hide_watched_movies")
    private val HIDE_WATCHED_SHOWS = booleanPreferencesKey("hide_watched_shows")
    private val MAX_CONTENT_RATING = stringPreferencesKey("max_content_rating")

    val hideWatchedMovies: Flow<Boolean> = prefs.data.map { it[HIDE_WATCHED_MOVIES] ?: false }
    val hideWatchedShows: Flow<Boolean> = prefs.data.map { it[HIDE_WATCHED_SHOWS] ?: false }
    val maxContentRating: Flow<String> = prefs.data.map { it[MAX_CONTENT_RATING] ?: "" }

    suspend fun setHideWatchedMovies(v: Boolean) { prefs.edit { it[HIDE_WATCHED_MOVIES] = v } }
    suspend fun setHideWatchedShows(v: Boolean) { prefs.edit { it[HIDE_WATCHED_SHOWS] = v } }
    suspend fun setMaxContentRating(rating: String) { prefs.edit { it[MAX_CONTENT_RATING] = rating } }
}
