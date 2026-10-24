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
        installSplashScreen() // must be called before super.onCreate
        super.onCreate(savedInstanceState)
        // Every screen starts with the green brand header, so status bar icons are light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        val container = (application as AghourApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                AghourTheme { AppNavigation() }
            }
        }
    }
}
