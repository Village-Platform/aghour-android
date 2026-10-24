package com.aghour.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aghour.app.R
import com.aghour.app.ui.theme.Cream
import com.aghour.app.ui.theme.Gold100
import com.aghour.app.ui.theme.Gold500
import com.aghour.app.ui.theme.Green50
import com.aghour.app.ui.theme.Green900
import com.aghour.app.ui.theme.Green950

/**
 * Green brand header used at the top of the main screens: optional village photo,
 * dark green gradient and a faint logo mark in the corner. Rounded at the bottom.
 */
@Composable
fun BrandHeader(
    modifier: Modifier = Modifier,
    showPhoto: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Green900),
    ) {
        if (showPhoto) {
            Image(
                painter = painterResource(R.drawable.brand_scene),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Green900.copy(alpha = if (showPhoto) 0.70f else 1f), Green950.copy(alpha = 0.97f)),
                    ),
                ),
        )
        // Watermark: the logo symbol, large and faint, half outside the left edge.
        Image(
            painter = painterResource(R.drawable.brand_mark_light),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd) // BottomEnd = bottom-left in right-to-left
                .size(150.dp)
                .absoluteOffset(x = (-36).dp, y = 30.dp)
                .alpha(0.08f),
        )
        content()
    }
}

/** White pill that looks like a search field; opens the search screen. */
@Composable
fun SearchPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = Gold500)
        Spacer(Modifier.width(10.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Round avatar with the first letter (firms and users have no photos yet). */
@Composable
fun LetterAvatar(
    text: String,
    size: Dp = 48.dp,
    background: Color = Gold100,
    content: Color = Green900,
    bordered: Boolean = false,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(if (bordered) Modifier.border(3.dp, Cream, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.trim().take(1),
            color = content,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

/** Alternating soft backgrounds so lists of tiles are easier to scan. */
fun tileBackground(index: Int): Color = if (index % 2 == 0) Green50 else Gold100

// ---- Loading skeletons ----

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
            colors = listOf(Color(0xFFEDEAE0), Color(0xFFF8F6F0), Color(0xFFEDEAE0)),
            start = Offset(x, 0f),
            end = Offset(x + 400f, 200f),
        ),
    )
}

@Composable
fun SkeletonBlock(width: Dp? = null, height: Dp = 14.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .shimmer(),
    )
}

/** Placeholder cards shaped like the real list items. */
@Composable
fun SkeletonCards(count: Int = 5, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(48.dp).clip(CircleShape).shimmer())
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBlock(width = 160.dp, height = 16.dp)
                    SkeletonBlock(height = 12.dp)
                    SkeletonBlock(width = 100.dp, height = 12.dp)
                }
            }
        }
    }
}
