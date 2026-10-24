package com.aghour.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Brand colors (from the logo), same as the admin dashboard.
val Green950 = Color(0xFF031F17)
val Green900 = Color(0xFF053024)
val Green800 = Color(0xFF0C3829)
val Green700 = Color(0xFF13513C)
val Green50 = Color(0xFFEEF6F1)
val Gold500 = Color(0xFFC88E38)
val Gold400 = Color(0xFFD9A654)
val Gold100 = Color(0xFFF6E8CF)
val Cream = Color(0xFFF6F1E4)
val Paper = Color(0xFFFBF9F3)
val Ink = Color(0xFF17241E)
val Muted = Color(0xFF66756D)

private val LightColors = lightColorScheme(
    primary = Green900,
    onPrimary = Cream,
    primaryContainer = Green50,
    onPrimaryContainer = Green900,
    secondary = Gold500,
    onSecondary = Green950,
    secondaryContainer = Gold100,
    onSecondaryContainer = Green950,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Green50,
    onSurfaceVariant = Muted,
    outline = Color(0xFFD6D9D2),
    outlineVariant = Color(0xFFE4E2D8),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

/** Arabic-first app: the layout is always right-to-left, whatever the phone language is. */
@Composable
fun AghourTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = LightColors, shapes = AppShapes, content = content)
    }
}
