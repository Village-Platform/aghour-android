package com.aghour.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aghour.app.R
import com.aghour.app.data.remote.ArticleDto
import com.aghour.app.data.remote.FirmDto
import com.aghour.app.ui.theme.Aghour

/**
 * Firm row (Explore / Home / My firms): category icon tile, name, "category · address", hours, quick call.
 * [icon] is the category icon (see firmIcon).
 */
@Composable
fun FirmRow(firm: FirmDto, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val c = Aghour.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(firm.name, style = MaterialTheme.typography.titleSmall, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.asHeading())
            Text(
                categoryLine(firm.category.name, firm.address),
                style = MaterialTheme.typography.bodySmall,
                color = c.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            firm.workingHours?.takeIf { it.isNotBlank() }?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = c.muted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(it, style = MaterialTheme.typography.labelSmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        firm.phone?.takeIf { it.isNotBlank() }?.let { phone ->
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(c.surface2)
                    .clickable { context.openDialer(phone) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.call), tint = c.goldInk, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Marks a title for TalkBack, so screen-reader users can jump between headings. */
fun Modifier.asHeading(): Modifier = semantics { heading() }

/** "category · address" with the category name bold and in the text colour (the address stays muted). */
@Composable
fun categoryLine(category: String, address: String?): AnnotatedString {
    val c = Aghour.colors
    return buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.ink)) { append(category) }
        address?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
    }
}

/** News row: mark tile, title, kind badge and relative time. */
@Composable
fun NewsRow(article: ArticleDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Aghour.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MarkTile(size = 50.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(article.title, style = MaterialTheme.typography.titleSmall, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.asHeading())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(kindLabel(article.kind), kindTone(article.kind))
                Spacer(Modifier.width(6.dp))
                Text(relativeTime(article.publishedAt), style = MaterialTheme.typography.labelSmall, color = c.muted)
            }
        }
    }
}

/** Big card for the newest item: village photo, dark gradient, gold badge and title. */
@Composable
fun FeaturedNewsCard(article: ArticleDto, badge: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Aghour.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(R.drawable.brand_scene),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
            // Solid gold over the photo: the soft badge tint is unreadable on a dark image.
            Row(
                Modifier.background(c.gold, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.PushPin, contentDescription = null, tint = c.onGold, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(3.dp))
                Text(badge, color = c.onGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(article.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(relativeTime(article.publishedAt), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        }
    }
}
