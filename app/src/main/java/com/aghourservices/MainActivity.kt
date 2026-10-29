package com.aghourservices

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
import com.aghourservices.data.settings.ThemeMode
import com.aghourservices.ui.components.LocalAppContainer
import com.aghourservices.ui.navigation.AppNavigation
import com.aghourservices.ui.theme.AghourTheme

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

    /** Each time the app comes to the front: pick up features switched on or off in the dashboard. */
    override fun onStart() {
        super.onStart()
        (application as AghourApp).container.features.refresh()
    }
}
