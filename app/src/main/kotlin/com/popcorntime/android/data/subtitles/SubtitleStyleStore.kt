package com.popcorntime.android.data.subtitles

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

data class SubtitleStyle(
    val fontSizeMultiplier: Float = 1f,
    val fontColor: Int = android.graphics.Color.WHITE,
    val backgroundColor: Int = 0x80000000.toInt(),
)

@Singleton
class SubtitleStyleStore @Inject constructor(
    @Named("subtitleStyleDataStore") private val prefs: DataStore<Preferences>,
) {
    private val FONT_SIZE_MULTIPLIER = floatPreferencesKey("font_size_multiplier")
    private val FONT_COLOR = intPreferencesKey("font_color")
    private val BACKGROUND_COLOR = intPreferencesKey("background_color")

    fun observeStyle(): Flow<SubtitleStyle> = prefs.data.map { p ->
        SubtitleStyle(
            fontSizeMultiplier = p[FONT_SIZE_MULTIPLIER] ?: 1f,
            fontColor = p[FONT_COLOR] ?: android.graphics.Color.WHITE,
            backgroundColor = p[BACKGROUND_COLOR] ?: 0x80000000.toInt(),
        )
    }

    suspend fun saveStyle(style: SubtitleStyle) {
        prefs.edit { p ->
            p[FONT_SIZE_MULTIPLIER] = style.fontSizeMultiplier
            p[FONT_COLOR] = style.fontColor
            p[BACKGROUND_COLOR] = style.backgroundColor
        }
    }
}
