package com.aghour.app.ui.components

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.aghour.app.R
import com.aghour.app.ui.theme.Cream
import com.aghour.app.ui.theme.Danger
import com.aghour.app.ui.theme.DangerSoft
import com.aghour.app.ui.theme.Forest
import com.aghour.app.ui.theme.Gold
import com.aghour.app.ui.theme.GoldDark
import com.aghour.app.ui.theme.Muted
import com.aghour.app.ui.theme.Sand

// ---------------------------------------------------------------- system bars

/** Light status-bar icons on dark (forest/photo) screens, dark icons on cream screens. */
@Composable
fun StatusBarIcons(light: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !light
    }
}

// ---------------------------------------------------------------- layouts

/**
 * The layout used by most designs: a dark header, then a cream "sheet" with rounded top corners
 * that fills the rest of the screen. Scrolls as one piece.
 */
@Composable
fun SheetLayout(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    sheetPadding: PaddingValues = PaddingValues(16.dp),
    header: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    StatusBarIcons(light = true)
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    Column(
        modifier
            .fillMaxSize()
            .background(Forest)
            .verticalScroll(scrollState),
    ) {
        header()
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = screenHeight)
                .background(Cream, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(sheetPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

/** Title area for cream screens (Explore, News, Plans, Pay). */
@Composable
fun ScreenTitle(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    StatusBarIcons(light = false)
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = if (onBack != null) 4.dp else 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Forest)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Forest)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Row(content = actions)
    }
}

/** Round back / share buttons drawn over photos. */
@Composable
fun OverlayIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Forest.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

// ---------------------------------------------------------------- brand pieces

/** Forest rounded square with the cream logo mark (placeholder for firms and news without photos). */
@Composable
fun MarkTile(size: Dp = 44.dp, corner: Dp = 12.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(corner)).background(Forest),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.mark_cream), contentDescription = null, modifier = Modifier.size(size * 0.64f))
    }
}

/** Forest rounded square with an emoji/letter (categories). */
@Composable
fun SymbolTile(symbol: String, size: Dp = 48.dp, background: Color = Forest, content: Color = Cream) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = (size.value * 0.42f).sp, color = content, fontWeight = FontWeight.Bold)
    }
}

/** Gold rounded-square avatar with the first letter (profile). */
@Composable
fun LetterAvatar(text: String, size: Dp = 56.dp, background: Color = Gold, content: Color = Forest) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.trim().take(1), color = content, fontWeight = FontWeight.Black, fontSize = (size.value * 0.42f).sp)
    }
}

enum class BadgeTone { Green, Gold, Red, Gray }

@Composable
fun Badge(text: String, tone: BadgeTone = BadgeTone.Green, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tone) {
        BadgeTone.Green -> Forest.copy(alpha = 0.10f) to Forest
        BadgeTone.Gold -> Gold.copy(alpha = 0.15f) to GoldDark
        BadgeTone.Red -> DangerSoft to Danger
        BadgeTone.Gray -> Sand.copy(alpha = 0.6f) to Forest.copy(alpha = 0.6f)
    }
    Text(
        text,
        modifier = modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 1.dp),
        color = fg,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Forest, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                color = Gold,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(6.dp),
            )
        }
    }
}

/** Filter chips row: selected = forest, others = white with a sand border. */
@Composable
fun <T> FilterChipsRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
) {
    LazyRow(contentPadding = contentPadding, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options, key = { it.first.toString() }) { (value, label) ->
            val isSelected = value == selected
            Text(
                label,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) Forest else Color.White)
                    .then(if (isSelected) Modifier else Modifier.border(1.dp, Sand, RoundedCornerShape(50)))
                    .clickable { onSelect(value) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                color = if (isSelected) Cream else Forest.copy(alpha = 0.65f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/** Fake search field that opens the Explore tab (Home header, on forest). */
@Composable
fun SearchPill(text: String, onClick: () -> Unit, onDark: Boolean = true) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (onDark) Color.White.copy(alpha = 0.10f) else Color.White)
            .then(if (onDark) Modifier else Modifier.border(1.dp, Sand, RoundedCornerShape(14.dp)))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = if (onDark) Cream.copy(alpha = 0.7f) else Gold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            color = if (onDark) Cream.copy(alpha = 0.6f) else Muted,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Gold,
            contentColor = Forest,
            disabledContainerColor = Gold.copy(alpha = 0.5f),
            disabledContentColor = Forest.copy(alpha = 0.6f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Forest, strokeWidth = 2.dp)
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

@Composable
fun ForestButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 44.dp) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Forest, contentColor = Cream),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Card container used across screens: white, sand border, rounded. */
@Composable
fun SandCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Sand, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

// ---------------------------------------------------------------- states

/** Empty or error state with the faded logo mark and an optional retry button. */
@Composable
fun MessageBox(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.mark_green),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            alpha = 0.25f,
        )
        Spacer(Modifier.height(12.dp))
        Text(message, color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            ForestButton(stringResource(R.string.retry), onRetry, Modifier.width(180.dp))
        }
    }
}

/** Moving light band over a placeholder shape while content loads. */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "shimmerX",
    )
    background(
        Brush.linearGradient(
            colors = listOf(Sand.copy(alpha = 0.55f), Cream, Sand.copy(alpha = 0.55f)),
            start = Offset(x, 0f),
            end = Offset(x + 400f, 200f),
        ),
    )
}

/** Placeholder rows shaped like the real list items. */
@Composable
fun SkeletonRows(count: Int = 5, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(count) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Sand, RoundedCornerShape(16.dp))
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

