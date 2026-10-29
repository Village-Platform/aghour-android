package com.aghourservices.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.aghourservices.data.settings.ThemeMode

// Fixed brand colours: used where the colour must not follow the theme
// (welcome artwork, text and buttons drawn over photos).
val Forest = Color(0xFF0B3727)
val Gold = Color(0xFFC28D40)
val Cream = Color(0xFFF4ECD9)

/**
 * Design tokens from the screen designs (light / dark). Screens use these names,
 * never raw colours, so both themes stay correct:
 * page = screen background, surface = cards, surface2 = fields/chips/tiles,
 * ink = text, muted = secondary text, line = borders, gold = call to action,
 * goldInk = gold text, brand = tinted icon tiles, ok / bad = status.
 */
@Immutable
data class AghourColors(
    val page: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val gold: Color,
    val goldInk: Color,
    val brand: Color,
    val ok: Color,
    val bad: Color,
    val isDark: Boolean,
) {
    /** Text on a gold button (black in both themes). */
    val onGold: Color get() = Color.Black
}

val LightPalette = AghourColors(
    page = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF5F5F5),
    ink = Color(0xFF000000),
    muted = Color(0xFF525252),
    line = Color(0xFFE2E2E2),
    gold = Gold,
    goldInk = Color(0xFF7A5516),
    brand = Forest,
    ok = Color(0xFF1F7A4D),
    bad = Color(0xFFB3402F),
    isDark = false,
)

/** GitHub Dark greys (canvas #0D1117, subtle #161B22, border #30363D…) with the brand gold for actions. */
val DarkPalette = AghourColors(
    page = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    surface2 = Color(0xFF21262D),
    ink = Color(0xFFE6EDF3),
    muted = Color(0xFF8B949E),
    line = Color(0xFF30363D),
    gold = Color(0xFFD9A458),
    goldInk = Color(0xFFE8C07E),
    brand = Color(0xFFE6EDF3),
    ok = Color(0xFF3FB950),
    bad = Color(0xFFF85149),
    isDark = true,
)

private val LocalAghourColors = staticCompositionLocalOf { LightPalette }

/** Read the current tokens: `Aghour.colors.ink`. */
object Aghour {
    val colors: AghourColors
        @Composable @ReadOnlyComposable get() = LocalAghourColors.current
}

/** Material components (dialogs, radio buttons, pull-to-refresh…) take their colours from the same tokens. */
private fun AghourColors.toMaterial() = if (isDark) {
    darkColorScheme(
        primary = gold, onPrimary = onGold, secondary = gold, onSecondary = onGold,
        background = page, onBackground = ink, surface = surface, onSurface = ink,
        surfaceVariant = surface2, onSurfaceVariant = muted,
        surfaceContainerLowest = page, surfaceContainerLow = surface, surfaceContainer = surface,
        surfaceContainerHigh = surface2, surfaceContainerHighest = surface2,
        outline = line, outlineVariant = line, error = bad,
    )
} else {
    lightColorScheme(
        primary = gold, onPrimary = onGold, secondary = gold, onSecondary = onGold,
        background = page, onBackground = ink, surface = surface, onSurface = ink,
        surfaceVariant = surface2, onSurfaceVariant = muted,
        surfaceContainerLowest = page, surfaceContainerLow = surface, surfaceContainer = surface,
        surfaceContainerHigh = surface, surfaceContainerHighest = surface2,
        outline = line, outlineVariant = line, error = bad,
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Arabic-first app: the layout is always right-to-left, whatever the phone language is.
 * [mode] is the user's choice from Settings; System follows Android's dark mode.
 */
@Composable
fun AghourTheme(mode: ThemeMode = ThemeMode.System, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val colors = if (dark) DarkPalette else LightPalette

    // Window background (seen during transitions) and navigation-bar icons follow the theme.
    // Status-bar icons are set per screen by StatusBarIcons.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.setBackgroundDrawable(ColorDrawable(colors.page.toArgb()))
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalAghourColors provides colors,
        // Text without an explicit colour must not fall back to black on the dark page.
        LocalContentColor provides colors.ink,
    ) {
        MaterialTheme(colorScheme = colors.toMaterial(), typography = AppTypography, shapes = AppShapes, content = content)
    }
}
