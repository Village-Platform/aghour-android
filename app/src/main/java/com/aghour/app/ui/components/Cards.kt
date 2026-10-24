package com.aghour.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aghour.app.R
import com.aghour.app.data.remote.ArticleDto
import com.aghour.app.data.remote.FirmDto
import com.aghour.app.ui.theme.Gold100
import com.aghour.app.ui.theme.Gold500
import com.aghour.app.ui.theme.Green50
import com.aghour.app.ui.theme.Green900

private val CardShape = RoundedCornerShape(18.dp)

@Composable
fun brandCardColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)

@Composable
fun brandCardElevation() = CardDefaults.cardElevation(defaultElevation = 0.5.dp, pressedElevation = 3.dp)

/** Firm row: letter avatar, name, category, address, hours and a one-tap call button. */
@Composable
fun FirmCard(firm: FirmDto, onClick: () -> Unit, modifier: Modifier = Modifier, showCategory: Boolean = false) {
    val context = LocalContext.current
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = brandCardColors(),
        elevation = brandCardElevation(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LetterAvatar(firm.name, size = 52.dp, background = Green50, content = Green900)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    firm.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showCategory) {
                    Pill(firm.category.name, Gold100, Green900)
                }
                firm.address?.takeIf { it.isNotBlank() }?.let { IconLine(Icons.Filled.LocationOn, it) }
                firm.workingHours?.takeIf { it.isNotBlank() }?.let { IconLine(Icons.Filled.DateRange, it) }
            }
            firm.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { context.openDialer(phone) },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Green50, contentColor = Green900),
                ) {
                    Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.call))
                }
            }
        }
    }
}

@Composable
fun IconLine(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = Gold500)
        Spacer(Modifier.width(5.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Full-width news card with a gold accent bar. */
@Composable
fun NewsCard(article: ArticleDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = brandCardColors(),
        elevation = brandCardElevation(),
    ) {
        Row(Modifier.padding(16.dp)) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(64.dp)
                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                NewsMeta(article)
                Spacer(Modifier.height(6.dp))
                Text(article.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    article.body,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Kind pill + date. */
@Composable
fun NewsMeta(article: ArticleDto, onDark: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Pill(
            kindLabel(article.kind),
            if (onDark) Gold500 else MaterialTheme.colorScheme.primaryContainer,
            if (onDark) Green900 else MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(4.dp)
                .background(if (onDark) Gold100 else MaterialTheme.colorScheme.outline, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            formatDate(article.publishedAt),
            style = MaterialTheme.typography.labelSmall,
            color = if (onDark) Gold100 else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}
