package com.aghourservices

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aghourservices.data.remote.parseNotificationAction
import com.aghourservices.data.settings.ThemeMode
import com.aghourservices.di.AppContainer
import com.aghourservices.ui.components.LocalAppContainer
import com.aghourservices.ui.navigation.AppNavigation
import com.aghourservices.ui.theme.AghourTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen() // must be called before super.onCreate
        super.onCreate(savedInstanceState)
        checkAndRequestNotificationPermission()

        val container = (application as AghourApp).container
        handleNotificationIntent(intent, container)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val container = (application as AghourApp).container
        handleNotificationIntent(intent, container)
    }

    @Suppress("DEPRECATION")
    private fun handleNotificationIntent(intent: Intent?, container: AppContainer) {
        val extras = intent?.extras ?: return
        val screen = extras.get("screen")?.toString()
        val rawId = extras.get("id")?.toString()
        val kind = extras.get("kind")?.toString()

        val action = parseNotificationAction(screen, rawId, kind)
        if (action != null) {
            container.pendingNotificationAction.value = action
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /** Each time the app comes to the front: pick up features switched on or off in the dashboard. */
    override fun onStart() {
        super.onStart()
        (application as AghourApp).container.features.refresh()
    }
}
