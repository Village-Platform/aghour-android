package com.aghour.app.data.settings

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ThemeMode { Light, Dark, System }

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Device preferences that are not tied to the signed-in user (they survive logout).
 * Kept apart from SessionStore so clearing the session can never touch them.
 */
class SettingsStore(context: Context, scope: CoroutineScope) {

    private val appContext = context.applicationContext
    private val store = appContext.settingsDataStore

    /** null while loading from disk (the splash stays up), then the saved mode; System by default. */
    val themeMode: StateFlow<ThemeMode?> = store.data
        .map { prefs -> prefs[THEME]?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } } ?: ThemeMode.System }
        .stateIn(scope, SharingStarted.Eagerly, null)

    init {
        scope.launch { themeMode.filterNotNull().distinctUntilChanged().collect(::applyToSystem) }
    }

    /**
     * Android 12+: tell the system the app's own day/night choice, so the system splash at the next
     * launch (drawn before any app code runs) uses the same light or dark colours as the app.
     * Older versions can only follow the phone's dark mode for that first splash frame.
     */
    private fun applyToSystem(mode: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val nightMode = when (mode) {
            ThemeMode.Light -> UiModeManager.MODE_NIGHT_NO
            ThemeMode.Dark -> UiModeManager.MODE_NIGHT_YES
            ThemeMode.System -> UiModeManager.MODE_NIGHT_AUTO // no app override: follow the phone
        }
        appContext.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(nightMode)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME] = mode.name }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
    }
}
