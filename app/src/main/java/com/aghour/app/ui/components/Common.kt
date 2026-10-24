package com.aghour.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aghour.app.R
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
    }
}

/** Empty or error state with the faded logo mark, optional retry button. */
@Composable
fun MessageBox(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.brand_mark),
            contentDescription = null,
            modifier = Modifier.size(72.dp).alpha(0.25f),
        )
        Spacer(Modifier.height(12.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun Pill(text: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 2.dp),
        color = content,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
}

private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

/** Rails sends ISO-8601 with offset, e.g. 2026-10-24T19:00:00.000+03:00 */
fun formatDate(iso: String?): String =
    iso?.let { runCatching { OffsetDateTime.parse(it).format(DateFormat) }.getOrNull() } ?: ""

@Composable
fun kindLabel(kind: String): String = when (kind) {
    "article" -> stringResource(R.string.kind_article)
    "announcement" -> stringResource(R.string.kind_announcement)
    else -> stringResource(R.string.kind_news)
}

@Composable
fun roleLabel(role: String): String = when (role) {
    "firm_admin" -> stringResource(R.string.role_firm_admin)
    "super_admin" -> stringResource(R.string.role_super_admin)
    else -> stringResource(R.string.role_user)
}
