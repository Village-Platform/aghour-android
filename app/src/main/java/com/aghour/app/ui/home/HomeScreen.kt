package com.aghour.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghour.app.R
import com.aghour.app.data.remote.ArticleDto
import com.aghour.app.data.remote.CategoryDto
import com.aghour.app.ui.components.LoadingBox
import com.aghour.app.ui.components.MessageBox
import com.aghour.app.ui.components.Pill
import com.aghour.app.ui.components.SectionTitle
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.components.formatDate
import com.aghour.app.ui.components.kindLabel
import com.aghour.app.ui.theme.Cream
import com.aghour.app.ui.theme.Gold100
import com.aghour.app.ui.theme.Gold400
import com.aghour.app.ui.theme.Green900
import com.aghour.app.ui.theme.Green950

@Composable
fun HomeScreen(
    onCategoryClick: (CategoryDto) -> Unit,
    onArticleClick: (ArticleDto) -> Unit,
    onSeeAllNews: () -> Unit,
) {
    val container = appContainer()
    val vm: HomeViewModel = viewModel { HomeViewModel(container.catalog) }
    val state by vm.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { HomeBanner() }

        item { SectionTitle(stringResource(R.string.section_categories), Modifier.padding(horizontal = 16.dp)) }

        when {
            state.loading && state.categories.isEmpty() -> item { LoadingBox(Modifier.height(160.dp)) }

            state.error != null && state.categories.isEmpty() -> item { MessageBox(state.error!!, onRetry = vm::load) }

            state.categories.isEmpty() -> item { MessageBox(stringResource(R.string.no_categories)) }

            // Rows of three tiles (simpler than nesting a grid inside a scrolling list).
            else -> items(state.categories.chunked(3), key = { row -> row.first().id }) { row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { category ->
                        CategoryTile(category, Modifier.weight(1f), onClick = { onCategoryClick(category) })
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        if (state.latestNews.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SectionTitle(stringResource(R.string.section_latest_news))
                    TextButton(onClick = onSeeAllNews) { Text(stringResource(R.string.see_all)) }
                }
            }
            items(state.latestNews, key = { "news-${it.id}" }) { article ->
                NewsRow(article, onClick = { onArticleClick(article) })
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun HomeBanner() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.brand_scene),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        // Green gradient so the text stays readable on the photo.
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(Green900.copy(alpha = 0.55f), Green950.copy(alpha = 0.95f)))),
        )
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.brand_mark_light),
                contentDescription = null,
                modifier = Modifier.size(84.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.app_name),
                color = Cream,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(stringResource(R.string.tagline), color = Gold400, style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Simple icons for the category keys used in the admin dashboard; falls back to the first letter. */
private fun categorySymbol(category: CategoryDto): String = when (category.icon?.lowercase()) {
    "doctor", "doctors", "clinic" -> "🩺"
    "pharmacy" -> "💊"
    "restaurant", "food" -> "🍽️"
    "shop", "store", "market" -> "🛒"
    "craft", "crafts", "worker" -> "🛠️"
    "school", "education" -> "🏫"
    "bakery" -> "🥖"
    "car", "transport" -> "🚗"
    else -> category.name.take(1)
}

@Composable
private fun CategoryTile(category: CategoryDto, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(0.95f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(Gold100),
                contentAlignment = Alignment.Center,
            ) {
                Text(categorySymbol(category), fontSize = 24.sp, color = Green900, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                category.name,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun NewsRow(article: ArticleDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.width(4.dp).height(48.dp).clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.secondary),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(kindLabel(article.kind), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(formatDate(article.publishedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text(article.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
