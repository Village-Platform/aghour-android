package com.aghourservices.ui.welcome

import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aghourservices.R
import com.aghourservices.ui.components.StatusBarIcons
import com.aghourservices.ui.theme.Aghour

/**
 * Continues the system splash: the mark is drawn exactly where Android drew it (288dp splash icon,
 * centred), then the name and "تطبيق القرية" fade in below it, reproducing the full logo.
 * Offsets come from the logo artwork (mark and name cut from the same 2000px source, same scale).
 */
@Composable
fun LaunchScreen() {
    val c = Aghour.colors
    StatusBarIcons()
    val name = remember { Animatable(0f) }
    LaunchedEffect(Unit) { name.animateTo(1f, tween(durationMillis = 450, delayMillis = 120)) }

    Box(Modifier.fillMaxSize().background(c.page)) {
        Image(themedPainter(R.drawable.splash_mark, c.isDark), contentDescription = null, modifier = Modifier.align(Alignment.Center).size(288.dp))
        Image(
            themedPainter(R.drawable.splash_name, c.isDark),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                // absolute: the artwork is not mirrored by the RTL layout
                .absoluteOffset(x = (-11).dp, y = 89.dp)
                .size(width = 190.dp, height = 65.dp)
                .graphicsLayer {
                    alpha = name.value
                    translationY = (1f - name.value) * 8.dp.toPx()
                },
        )
    }
}

/**
 * The light or dark (`drawable-night`) version of an image for the app's own theme choice, which can
 * differ from the phone's dark mode (e.g. "Dark" chosen in Settings on a phone in light mode).
 */
@Composable
private fun themedPainter(@DrawableRes id: Int, dark: Boolean): Painter {
    val context = LocalContext.current
    return remember(id, dark) {
        val config = Configuration(context.resources.configuration).apply {
            val night = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
        }
        val drawable = context.createConfigurationContext(config).getDrawable(id) as BitmapDrawable
        BitmapPainter(drawable.bitmap.asImageBitmap())
    }
}
