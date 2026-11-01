package com.aghourservices.ui.settings

import com.aghourservices.data.analytics.AnalyticsEvent
import com.aghourservices.data.analytics.AnalyticsService
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghourservices.BuildConfig
import com.aghourservices.R
import com.aghourservices.data.settings.SettingsStore
import com.aghourservices.data.settings.ThemeMode
import com.aghourservices.ui.components.GhostButton
import com.aghourservices.ui.components.MutedText
import com.aghourservices.ui.components.ScreenTitle
import com.aghourservices.ui.components.Segmented
import com.aghourservices.ui.components.SurfaceCard
import com.aghourservices.ui.components.appContainer
import com.aghourservices.ui.theme.Aghour
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(val themeMode: ThemeMode = ThemeMode.System)

class SettingsViewModel(private val settings: SettingsStore, private val analytics: AnalyticsService) : ViewModel() {
    val state: StateFlow<SettingsUiState> = settings.themeMode
        .map { SettingsUiState(themeMode = it ?: ThemeMode.System) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(settings.themeMode.value ?: ThemeMode.System))

    fun setThemeMode(mode: ThemeMode) {
        if (mode != state.value.themeMode) analytics.log(AnalyticsEvent.ChangeTheme(mode.name.lowercase()))
        viewModelScope.launch { settings.setThemeMode(mode) }
    }
}

/** "الإعدادات": appearance (light / dark / follow the phone). */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = appContainer()
    val vm: SettingsViewModel = viewModel { SettingsViewModel(container.settings, container.analytics) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Aghour.colors

    Column(Modifier.fillMaxSize().background(c.page)) {
        ScreenTitle(stringResource(R.string.settings), onBack = onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SurfaceCard(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Palette, contentDescription = null, tint = c.goldInk, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = c.ink)
                }
                Spacer(Modifier.height(10.dp))
                Segmented(
                    options = listOf(
                        ThemeMode.Light to stringResource(R.string.theme_light),
                        ThemeMode.Dark to stringResource(R.string.theme_dark),
                        ThemeMode.System to stringResource(R.string.theme_system),
                    ),
                    selected = state.themeMode,
                    onSelect = vm::setThemeMode,
                    icons = mapOf(
                        ThemeMode.Light to Icons.Outlined.LightMode,
                        ThemeMode.Dark to Icons.Outlined.DarkMode,
                        ThemeMode.System to Icons.Outlined.BrightnessAuto,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                MutedText(stringResource(R.string.theme_system_hint))
            }
            // Compile-time constant: the whole card is dropped from release builds.
            if (BuildConfig.DEBUG) DebugToolsCard()
        }
    }
}

/** Debug builds only: checks the Crashlytics pipeline end to end (aghour-dev project). */
@Composable
private fun DebugToolsCard() {
    val c = Aghour.colors
    SurfaceCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.BugReport, contentDescription = null, tint = c.bad, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.debug_tools), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = c.ink)
        }
        Spacer(Modifier.height(10.dp))
        GhostButton(
            text = stringResource(R.string.debug_test_crash),
            onClick = { throw RuntimeException("Test crash from debug settings") },
        )
        Spacer(Modifier.height(8.dp))
        MutedText(stringResource(R.string.debug_test_crash_hint))
    }
}
