package com.aghourservices.ui.settings

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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghourservices.R
import com.aghourservices.data.settings.AppLanguage
import com.aghourservices.data.settings.Language
import com.aghourservices.data.settings.SettingsStore
import com.aghourservices.data.settings.ThemeMode
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

class SettingsViewModel(private val settings: SettingsStore) : ViewModel() {
    val state: StateFlow<SettingsUiState> = settings.themeMode
        .map { SettingsUiState(themeMode = it ?: ThemeMode.System) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(settings.themeMode.value ?: ThemeMode.System))

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    // Not in SettingsUiState: Android owns this value and recreates the activity when it changes.
    fun setLanguage(language: Language) {
        if (language != AppLanguage.current()) AppLanguage.set(language)
    }
}

/** "الإعدادات": language (Arabic / English) and appearance (light / dark / follow the phone). */
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
                SettingTitle(Icons.Outlined.Translate, stringResource(R.string.language))
                Spacer(Modifier.height(10.dp))
                // Read from the configuration, not the ViewModel: it survives the recreation a language change causes.
                val language = if (LocalConfiguration.current.locales[0].language == Language.English.tag) Language.English else Language.Arabic
                Segmented(
                    options = listOf(
                        Language.Arabic to stringResource(R.string.language_arabic),
                        Language.English to stringResource(R.string.language_english),
                    ),
                    selected = language,
                    onSelect = vm::setLanguage,
                )
            }
            SurfaceCard(padding = 16.dp) {
                SettingTitle(Icons.Outlined.Palette, stringResource(R.string.appearance))
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
        }
    }
}

@Composable
private fun SettingTitle(icon: ImageVector, title: String) {
    val c = Aghour.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = c.goldInk, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = c.ink)
    }
}
