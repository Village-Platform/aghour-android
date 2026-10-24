package com.aghour.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// Palette from the screen designs (ajhour_screens.html).
val Forest = Color(0xFF0B3727)   // headers, primary buttons, icon tiles
val Moss = Color(0xFF244638)     // secondary dark surface
val Gold = Color(0xFFC28D40)     // call-to-action buttons, highlights
val GoldDark = Color(0xFF8A6A2F) // gold text on light gold
val Cream = Color(0xFFFAF5EC)    // screen background, text on dark
val Sand = Color(0xFFE6DCC6)     // borders, dividers, inactive
val Ink = Color(0xFF163226)      // body text
val Muted = Color(0xFF6B7F75)    // secondary text (forest at ~55%)
val Danger = Color(0xFFDC2626)
val DangerSoft = Color(0xFFFEF2F2)

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Cream,
    primaryContainer = Forest.copy(alpha = 0.10f),
    onPrimaryContainer = Forest,
    secondary = Gold,
    onSecondary = Forest,
    secondaryContainer = Gold.copy(alpha = 0.15f),
    onSecondaryContainer = GoldDark,
    tertiary = Moss,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Cream,
    onSurfaceVariant = Muted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Cream,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    outline = Sand,
    outlineVariant = Sand.copy(alpha = 0.6f),
    error = Danger,
    errorContainer = DangerSoft,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Arabic-first app: the layout is always right-to-left, whatever the phone language is. */
@Composable
fun AghourTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = LightColors, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
