package com.aghour.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aghour.app.R

/**
 * Cairo (SIL Open Font License, see licenses/Cairo-OFL.txt) — one variable font file,
 * each weight below picks its point on the font's weight axis.
 */
val Cairo = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.SemiBold),
    Font(R.font.cairo, FontWeight.Bold),
    Font(R.font.cairo, FontWeight.ExtraBold),
)

/**
 * Tajawal (SIL Open Font License, see licenses/Tajawal-OFL.txt) — a rounder display face used for
 * the welcome screen call-to-action, to stand apart from the Cairo body text.
 */
val Tajawal = FontFamily(
    Font(R.font.tajawal_bold, FontWeight.Bold),
    Font(R.font.tajawal_extrabold, FontWeight.ExtraBold),
)

private val base = Typography()
private fun TextStyle.cairo(weight: FontWeight? = null) =
    copy(fontFamily = Cairo, fontWeight = weight ?: fontWeight)

val AppTypography = Typography(
    displayLarge = base.displayLarge.cairo(FontWeight.Bold),
    displayMedium = base.displayMedium.cairo(FontWeight.Bold),
    displaySmall = base.displaySmall.cairo(FontWeight.Bold),
    headlineLarge = base.headlineLarge.cairo(FontWeight.Bold),
    headlineMedium = base.headlineMedium.cairo(FontWeight.Bold),
    headlineSmall = base.headlineSmall.cairo(FontWeight.Bold),
    titleLarge = base.titleLarge.cairo(FontWeight.Bold),
    titleMedium = base.titleMedium.cairo(FontWeight.Bold),
    titleSmall = base.titleSmall.cairo(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.cairo().copy(lineHeight = 28.sp),
    bodyMedium = base.bodyMedium.cairo().copy(lineHeight = 22.sp),
    bodySmall = base.bodySmall.cairo(),
    labelLarge = base.labelLarge.cairo(FontWeight.Bold),
    labelMedium = base.labelMedium.cairo(FontWeight.SemiBold),
    labelSmall = base.labelSmall.cairo(FontWeight.Medium),
)
