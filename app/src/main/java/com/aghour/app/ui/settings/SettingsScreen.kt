package com.aghour.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghour.app.R
import com.aghour.app.data.settings.SettingsStore
import com.aghour.app.data.settings.ThemeMode
import com.aghour.app.ui.components.MutedText
import com.aghour.app.ui.components.ScreenTitle
import com.aghour.app.ui.components.Segmented
import com.aghour.app.ui.components.SurfaceCard
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.theme.Aghour
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(val themeMode: ThemeMode = ThemeMode.System)

class SettingsViewModel(private val settings: SettingsStore) : ViewModel() {
    val state: StateFlow<SettingsUiState> = settings.themeMode
        .map { SettingsUiState(themeMode = it ?: ThemeMode.System) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(settings.themeMode.value ?: ThemeMode.System))

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }
}

/** "الإعدادات": appearance (light / dark / follow the phone). */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = appContainer()
    val vm: SettingsViewModel = viewModel { SettingsViewModel(container.settings) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Aghour.colors

    Column(Modifier.fillMaxSize().background(c.page)) {
        ScreenTitle(stringResource(R.string.settings), onBack = onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SurfaceCard(padding = 16.dp) {
                Text("🎨 ${stringResource(R.string.appearance)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = c.ink)
                Spacer(Modifier.height(10.dp))
                Segmented(
                    options = listOf(
                        ThemeMode.Light to stringResource(R.string.theme_light),
                        ThemeMode.Dark to stringResource(R.string.theme_dark),
                        ThemeMode.System to stringResource(R.string.theme_system),
                    ),
                    selected = state.themeMode,
                    onSelect = vm::setThemeMode,
                )
                Spacer(Modifier.height(8.dp))
                MutedText(stringResource(R.string.theme_system_hint))
            }
        }
    }
}
