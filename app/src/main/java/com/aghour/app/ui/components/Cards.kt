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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aghour.app.R
import com.aghour.app.data.remote.ArticleDto
import com.aghour.app.data.remote.FirmDto
import com.aghour.app.ui.theme.Cream
import com.aghour.app.ui.theme.Forest
import com.aghour.app.ui.theme.Muted
import com.aghour.app.ui.theme.Sand

/** Firm row (Explore / Home): forest mark tile, name, "category · address", hours, quick call. */
@Composable
fun FirmRow(firm: FirmDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Sand, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MarkTile(size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(firm.name, style = MaterialTheme.typography.titleSmall, color = Forest, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(firm.category.name, firm.address?.takeIf { it.isNotBlank() }).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            firm.workingHours?.takeIf { it.isNotBlank() }?.let {
                Text("🕐 $it", style = MaterialTheme.typography.labelSmall, color = Forest.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        firm.phone?.takeIf { it.isNotBlank() }?.let { phone ->
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Forest.copy(alpha = 0.08f))
                    .clickable { context.openDialer(phone) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.call), tint = Forest, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** News row: mark tile, title, kind badge and relative time. */
@Composable
fun NewsRow(article: ArticleDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Sand, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MarkTile(size = 52.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(article.title, style = MaterialTheme.typography.titleSmall, color = Forest, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(kindLabel(article.kind), kindTone(article.kind))
                Spacer(Modifier.width(6.dp))
                Text(relativeTime(article.publishedAt), style = MaterialTheme.typography.labelSmall, color = Muted)
            }
        }
    }
}

/** Big card for the newest item: village photo, forest gradient, badge and title. */
@Composable
fun FeaturedNewsCard(article: ArticleDto, badge: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(R.drawable.brand_scene),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Forest.copy(alpha = 0.95f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
            Badge("📌 $badge", BadgeTone.Gold, Modifier.background(Cream, RoundedCornerShape(50)))
            Spacer(Modifier.height(6.dp))
            Text(article.title, color = Cream, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(relativeTime(article.publishedAt), color = Cream.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        }
    }
}
