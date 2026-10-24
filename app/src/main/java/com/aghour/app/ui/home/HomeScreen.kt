package com.aghour.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghour.app.R
import com.aghour.app.data.remote.ArticleDto
import com.aghour.app.data.remote.CategoryDto
import com.aghour.app.ui.components.BrandHeader
import com.aghour.app.ui.components.MessageBox
import com.aghour.app.ui.components.NewsMeta
import com.aghour.app.ui.components.SearchPill
import com.aghour.app.ui.components.SectionTitle
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.components.brandCardColors
import com.aghour.app.ui.components.brandCardElevation
import com.aghour.app.ui.components.shimmer
import com.aghour.app.ui.components.tileBackground
import com.aghour.app.ui.theme.Cream
import com.aghour.app.ui.theme.Gold400
import com.aghour.app.ui.theme.Green900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCategoryClick: (CategoryDto) -> Unit,
    onArticleClick: (ArticleDto) -> Unit,
    onSeeAllNews: () -> Unit,
    onSearchClick: () -> Unit,
) {
    val container = appContainer()
    val vm: HomeViewModel = viewModel { HomeViewModel(container.catalog) }
    val state by vm.state.collectAsStateWithLifecycle()
    val user by container.auth.currentUser.collectAsStateWithLifecycle()

    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                HomeHeader(
                    greeting = user?.name?.let { stringResource(R.string.greeting_user, it.substringBefore(' ')) }
                        ?: stringResource(R.string.greeting_guest),
                    onSearchClick = onSearchClick,
                )
            }

            item { SectionTitle(stringResource(R.string.section_categories), Modifier.padding(horizontal = 20.dp)) }

            when {
                state.loading -> item { CategoriesSkeleton() }

                state.error != null && state.categories.isEmpty() ->
                    item { MessageBox(state.error!!, onRetry = { vm.load() }) }

                state.categories.isEmpty() -> item { MessageBox(stringResource(R.string.no_categories)) }

                // Rows of three tiles (simpler than nesting a grid inside a scrolling list).
                else -> items(state.categories.chunked(3), key = { row -> row.first().id }) { row ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { category ->
                            val index = state.categories.indexOf(category)
                            CategoryTile(category, index, Modifier.weight(1f)) { onCategoryClick(category) }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            if (state.latestNews.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SectionTitle(stringResource(R.string.section_latest_news))
                        TextButton(onClick = onSeeAllNews) { Text(stringResource(R.string.see_all)) }
                    }
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.latestNews, key = { it.id }) { article ->
                            NewsCarouselCard(article) { onArticleClick(article) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(greeting: String, onSearchClick: () -> Unit) {
    BrandHeader {
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.brand_mark_light),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(stringResource(R.string.app_name), color = Cream, style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.tagline), color = Gold400, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(greeting, color = Cream, style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.home_subtitle),
                color = Cream.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            SearchPill(stringResource(R.string.search_hint), onClick = onSearchClick)
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
    else -> category.name.trim().take(1)
}

@Composable
private fun CategoryTile(category: CategoryDto, index: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(0.92f),
        shape = RoundedCornerShape(20.dp),
        colors = brandCardColors(),
        elevation = brandCardElevation(),
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(tileBackground(index)),
                contentAlignment = Alignment.Center,
            ) {
                Text(categorySymbol(category), fontSize = 26.sp, color = Green900, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                category.name,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun CategoriesSkeleton() {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    Box(Modifier.weight(1f).aspectRatio(0.92f).clip(RoundedCornerShape(20.dp)).shimmer())
                }
            }
        }
    }
}

@Composable
private fun NewsCarouselCard(article: ArticleDto, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(280.dp).height(150.dp),
        shape = RoundedCornerShape(20.dp),
        colors = brandCardColors(),
        elevation = brandCardElevation(),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            NewsMeta(article)
            Spacer(Modifier.height(8.dp))
            Text(article.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                article.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

