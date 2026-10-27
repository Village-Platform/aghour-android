package com.aghour.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class ThemeMode { Light, Dark, System }

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Device preferences that are not tied to the signed-in user (they survive logout).
 * Kept apart from SessionStore so clearing the session can never touch them.
 */
class SettingsStore(context: Context, scope: CoroutineScope) {

    private val store = context.applicationContext.settingsDataStore

    /** null while loading from disk (the splash stays up), then the saved mode; System by default. */
    val themeMode: StateFlow<ThemeMode?> = store.data
        .map { prefs -> prefs[THEME]?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } } ?: ThemeMode.System }
        .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME] = mode.name }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
    }
}
