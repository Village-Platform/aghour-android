package com.aghour.app.ui.components

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.aghour.app.R
import com.aghour.app.ui.theme.Aghour

// ---------------------------------------------------------------- system bars

/** Light status-bar icons over photos and in the dark theme, dark icons on light pages. */
@Composable
fun StatusBarIcons(overPhoto: Boolean = false) {
    val view = LocalView.current
    val light = overPhoto || Aghour.colors.isDark
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !light
    }
}

// ---------------------------------------------------------------- layouts

/**
 * Detail layout (firm, article): a photo header, then a page-coloured "sheet" with rounded top
 * corners that slides up over the photo. Scrolls as one piece.
 */
@Composable
fun SheetLayout(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    sheetPadding: PaddingValues = PaddingValues(16.dp),
    header: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    StatusBarIcons(overPhoto = true)
    val c = Aghour.colors
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Column(
        modifier
            .fillMaxSize()
            .background(c.page)
            .verticalScroll(scrollState),
    ) {
        header()
        Column(
            Modifier
                .fillMaxWidth()
                .offset(y = (-20).dp)
                .heightIn(min = screenHeight)
                .background(c.page, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(sheetPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

/** Title area for plain screens (Explore, News, Plans, Pay, Settings…). */
@Composable
fun ScreenTitle(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    StatusBarIcons()
    val c = Aghour.colors
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = if (onBack != null) 4.dp else 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = c.ink)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = c.ink, modifier = Modifier.asHeading())
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = c.muted)
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Round back / share buttons drawn over photos. */
@Composable
fun OverlayIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

// ---------------------------------------------------------------- brand pieces

/** The logo mark: green on light pages, cream on dark ones. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, alpha: Float = 1f) {
    val res = if (Aghour.colors.isDark) R.drawable.mark_cream else R.drawable.mark_green
    Image(painterResource(res), contentDescription = null, modifier = modifier, alpha = alpha)
}

/** The full logo (mark + name): green on light pages, cream on dark ones. */
@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    val res = if (Aghour.colors.isDark) R.drawable.logo_cream else R.drawable.logo_green
    Image(painterResource(res), contentDescription = null, modifier = modifier)
}

/** Tinted rounded square with the logo mark (placeholder for firms and news without photos). */
@Composable
fun MarkTile(size: Dp = 44.dp, corner: Dp = 12.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(corner)).background(Aghour.colors.brand.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        BrandMark(Modifier.size(size * 0.66f))
    }
}

/**
 * Rounded square with a category icon in the category's own colour on a light tint of it;
 * the app logo mark on the neutral tint when the category has no icon.
 */
@Composable
fun IconTile(icon: CategoryIcon?, size: Dp = 48.dp) {
    val c = Aghour.colors
    val tint = icon?.color
    val background = tint?.copy(alpha = if (c.isDark) 0.18f else 0.12f) ?: c.brand.copy(alpha = 0.10f)
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null && tint != null) {
            Icon(icon.vector, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
        } else {
            BrandMark(Modifier.size(size * 0.66f))
        }
    }
}

/** Gold rounded-square avatar with the first letter (profile). */
@Composable
fun LetterAvatar(text: String, size: Dp = 56.dp) {
    val c = Aghour.colors
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(c.gold),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.trim().take(1), color = c.onGold, fontWeight = FontWeight.Black, fontSize = (size.value * 0.42f).sp)
    }
}

enum class BadgeTone { Green, Gold, Red, Gray }

@Composable
fun Badge(text: String, tone: BadgeTone = BadgeTone.Green, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = Aghour.colors
    val (bg, fg) = when (tone) {
        BadgeTone.Green -> c.ok.copy(alpha = 0.12f) to c.ok
        BadgeTone.Gold -> c.gold.copy(alpha = 0.22f) to c.goldInk
        BadgeTone.Red -> c.bad.copy(alpha = 0.12f) to c.bad
        BadgeTone.Gray -> c.surface2 to c.muted
    }
    Row(
        modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    val c = Aghour.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.ink, modifier = Modifier.weight(1f).asHeading())
        if (action != null && onAction != null) {
            Text(
                action,
                color = c.goldInk,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(6.dp),
            )
        }
    }
}

/**
 * Filter chips row: selected = gold, others = soft grey (surface2). [icons] adds an icon per option;
 * an option mapped to null shows the app logo mark (category without an icon). [iconTints] colours
 * an icon while its chip is not selected (category colours); a selected chip's icon is black on gold.
 */
@Composable
fun <T> FilterChipsRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    icons: Map<T, ImageVector?> = emptyMap(),
    iconTints: Map<out T, Color> = emptyMap(),
) {
    val c = Aghour.colors
    LazyRow(contentPadding = contentPadding, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options, key = { it.first.toString() }) { (value, label) ->
            val isSelected = value == selected
            val fg = if (isSelected) c.onGold else c.muted
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) c.gold else c.surface2)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (value in icons) {
                    val icon = icons[value]
                    val tint = if (isSelected) fg else iconTints[value] ?: fg
                    if (icon != null) Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                    else BrandMark(Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(label, color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Segmented control (theme picker): selected = gold. A label may be empty when [icons] has an icon for it. */
@Composable
fun <T> Segmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    icons: Map<T, ImageVector> = emptyMap(),
) {
    val c = Aghour.colors
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.surface2)
            .border(1.dp, c.line, RoundedCornerShape(10.dp))
            .padding(2.dp),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val fg = if (isSelected) c.onGold else c.muted
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) c.gold else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icons[value]?.let { Icon(it, contentDescription = label.ifEmpty { null }, tint = fg, modifier = Modifier.size(16.dp)) }
                if (icons[value] != null && label.isNotEmpty()) Spacer(Modifier.width(4.dp))
                if (label.isNotEmpty()) Text(label, color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Fake search field that opens the Explore tab (Home). */
@Composable
fun SearchPill(text: String, onClick: () -> Unit) {
    val c = Aghour.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface2)
            .border(1.dp, c.line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = c.muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = c.muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------- buttons

@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    fontFamily: FontFamily? = null,
) {
    val c = Aghour.colors
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.gold,
            contentColor = c.onGold,
            disabledContainerColor = c.gold.copy(alpha = 0.5f),
            disabledContentColor = c.onGold.copy(alpha = 0.6f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = c.onGold, strokeWidth = 2.dp)
        } else {
            Text(
                text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily ?: MaterialTheme.typography.titleSmall.fontFamily,
            )
        }
    }
}

/** Secondary button: soft grey with a thin border (the designs' "ghost" button). */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 44.dp) {
    val c = Aghour.colors
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, c.line),
        colors = ButtonDefaults.buttonColors(containerColor = c.surface2, contentColor = c.ink),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Card container used across screens: surface colour, thin border, rounded. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Aghour.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** A feature line with a check icon (plan features, guest benefits). */
@Composable
fun CheckLine(text: String, color: Color = Aghour.colors.muted) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = Aghour.colors.ok, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, style = MaterialTheme.typography.bodySmall)
    }
}

/** Arrow pointing to the reading end (left in RTL), for menu rows. */
@Composable
fun Chevron() = Icon(
    Icons.AutoMirrored.Filled.KeyboardArrowRight,
    contentDescription = null,
    tint = Aghour.colors.muted,
    modifier = Modifier.size(20.dp),
)

// ---------------------------------------------------------------- states

/** Empty or error state with the faded logo mark and an optional retry button. */
@Composable
fun MessageBox(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandMark(Modifier.size(72.dp), alpha = 0.25f)
        Spacer(Modifier.height(12.dp))
        Text(message, color = Aghour.colors.muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            GhostButton(stringResource(R.string.retry), onRetry, Modifier.width(180.dp))
        }
    }
}

/** Moving light band over a placeholder shape while content loads. */
fun Modifier.shimmer(): Modifier = composed {
    val c = Aghour.colors
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "shimmerX",
    )
    background(
        Brush.linearGradient(
            colors = listOf(c.surface2, c.line, c.surface2),
            start = Offset(x, 0f),
            end = Offset(x + 400f, 200f),
        ),
    )
}

/** Placeholder rows shaped like the real list items. */
@Composable
fun SkeletonRows(count: Int = 5, modifier: Modifier = Modifier) {
    val c = Aghour.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(count) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surface)
                    .border(1.dp, c.line, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).shimmer())
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.width(150.dp).height(14.dp).clip(RoundedCornerShape(6.dp)).shimmer())
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(6.dp)).shimmer())
                }
            }
        }
    }
}
