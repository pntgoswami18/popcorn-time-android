package com.popcorntime.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TorrentPrefsStore @Inject constructor(
    @Named("torrentPrefsDataStore") private val prefs: DataStore<Preferences>,
) {
    private val MAX_DOWNLOAD_KBPS = intPreferencesKey("max_download_kbps")
    private val MAX_UPLOAD_KBPS = intPreferencesKey("max_upload_kbps")
    private val SEEDING_RATIO_LIMIT = floatPreferencesKey("seeding_ratio_limit")

    val maxDownloadKbps: Flow<Int> = prefs.data.map { it[MAX_DOWNLOAD_KBPS] ?: 0 }
    val maxUploadKbps: Flow<Int> = prefs.data.map { it[MAX_UPLOAD_KBPS] ?: 0 }
    val seedingRatioLimit: Flow<Float> = prefs.data.map { it[SEEDING_RATIO_LIMIT] ?: 0f }

    suspend fun setMaxDownloadKbps(v: Int) { prefs.edit { it[MAX_DOWNLOAD_KBPS] = v } }
    suspend fun setMaxUploadKbps(v: Int) { prefs.edit { it[MAX_UPLOAD_KBPS] = v } }
    suspend fun setSeedingRatioLimit(v: Float) { prefs.edit { it[SEEDING_RATIO_LIMIT] = v } }
}
