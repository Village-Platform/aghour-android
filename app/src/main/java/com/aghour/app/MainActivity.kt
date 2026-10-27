package com.aghour.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aghour.app.data.settings.ThemeMode
import com.aghour.app.ui.components.LocalAppContainer
import com.aghour.app.ui.navigation.AppNavigation
import com.aghour.app.ui.theme.AghourTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen() // must be called before super.onCreate
        super.onCreate(savedInstanceState)
        val container = (application as AghourApp).container
        // Keep the system splash until the theme is read from DataStore, so the launch logo that
        // follows (LaunchScreen) is already in the right theme and takes over seamlessly.
        splash.setKeepOnScreenCondition { container.settings.themeMode.value == null }
        // Screens switch the status-bar icon colour themselves; AghourTheme sets the navigation bar.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        setContent {
            val themeMode by container.settings.themeMode.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalAppContainer provides container) {
                AghourTheme(themeMode ?: ThemeMode.System) { AppNavigation() }
            }
        }
    }
}
