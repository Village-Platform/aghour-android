package com.aghour.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghour.app.R
import com.aghour.app.data.remote.ArticleDto
import com.aghour.app.data.remote.CategoryDto
import com.aghour.app.data.remote.FirmDto
import com.aghour.app.ui.components.FirmRow
import com.aghour.app.ui.components.MessageBox
import com.aghour.app.ui.components.NewsRow
import com.aghour.app.ui.components.SearchPill
import com.aghour.app.ui.components.SectionHeader
import com.aghour.app.ui.components.SheetLayout
import com.aghour.app.ui.components.SkeletonRows
import com.aghour.app.ui.components.SymbolTile
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.components.categorySymbol
import com.aghour.app.ui.components.greeting
import com.aghour.app.ui.components.shimmer
import com.aghour.app.ui.theme.Cream
import com.aghour.app.ui.theme.Forest
import com.aghour.app.ui.theme.Gold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCategoryClick: (CategoryDto) -> Unit,
    onExplore: () -> Unit,
    onFirmClick: (FirmDto) -> Unit,
    onArticleClick: (ArticleDto) -> Unit,
    onSeeAllNews: () -> Unit,
    onAddFirm: () -> Unit,
) {
    val container = appContainer()
    val vm: HomeViewModel = viewModel { HomeViewModel(container.catalog) }
    val state by vm.state.collectAsStateWithLifecycle()
    val user by container.auth.currentUser.collectAsStateWithLifecycle()

    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
        SheetLayout(
            sheetPadding = PaddingValues(top = 18.dp, bottom = 24.dp),
            header = {
                Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 26.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(greeting(), color = Gold, style = MaterialTheme.typography.labelMedium)
                            Text(
                                user?.name?.let { stringResource(R.string.hey_name, it.substringBefore(' ')) }
                                    ?: stringResource(R.string.hey_guest),
                                color = Cream,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Image(painterResource(R.drawable.mark_cream), contentDescription = null, modifier = Modifier.size(46.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    SearchPill(stringResource(R.string.search_hint), onClick = onExplore)
                }
            },
        ) {
            // Categories as forest tiles (horizontal, like the shortcut row in the design)
            when {
                state.loading -> CategoriesSkeleton()
                state.error != null && state.categories.isEmpty() -> MessageBox(state.error!!, onRetry = { vm.load() })
                else -> LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(state.categories, key = { it.id }) { category ->
                        Column(
                            Modifier.width(64.dp).clip(RoundedCornerShape(12.dp)).clickable { onCategoryClick(category) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            SymbolTile(categorySymbol(category), size = 54.dp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                category.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Forest,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Cover banner -> "add your firm"
            Image(
                painter = painterResource(R.drawable.cover_banner),
                contentDescription = stringResource(R.string.add_your_firm),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .aspectRatio(2f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onAddFirm),
            )

            if (state.featuredFirms.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(stringResource(R.string.featured_firms), stringResource(R.string.see_all), onExplore)
                    state.featuredFirms.forEach { firm -> FirmRow(firm, onClick = { onFirmClick(firm) }) }
                }
            }

            if (state.latestNews.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(stringResource(R.string.section_latest_news), stringResource(R.string.see_all), onSeeAllNews)
                    state.latestNews.forEach { article -> NewsRow(article, onClick = { onArticleClick(article) }) }
                }
            }

            if (state.loading) SkeletonRows(3, Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun CategoriesSkeleton() {
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(5) { Box(Modifier.size(54.dp).clip(RoundedCornerShape(17.dp)).shimmer()) }
    }
}
