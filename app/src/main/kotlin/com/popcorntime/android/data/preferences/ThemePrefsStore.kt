package com.popcorntime.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, DARK, LIGHT }

@Singleton
class ThemePrefsStore @Inject constructor(
    @Named("themeDataStore") private val prefs: DataStore<Preferences>,
) {
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = prefs.data.map { prefs ->
        when (prefs[THEME_MODE_KEY]) {
            ThemeMode.DARK.name -> ThemeMode.DARK
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit { it[THEME_MODE_KEY] = mode.name }
    }
}
