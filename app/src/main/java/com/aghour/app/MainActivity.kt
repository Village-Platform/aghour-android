package com.aghour.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aghour.app.ui.components.LocalAppContainer
import com.aghour.app.ui.navigation.AppNavigation
import com.aghour.app.ui.theme.AghourTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen() // must be called before super.onCreate
        super.onCreate(savedInstanceState)
        val container = (application as AghourApp).container
        // Keep the splash until we know whether to show the welcome screen (read from DataStore).
        splash.setKeepOnScreenCondition { container.session.welcomeDone.value == null }
        // Screens switch the status-bar icon color themselves (light on forest, dark on cream).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                AghourTheme { AppNavigation() }
            }
        }
    }
}
