package com.aghour.app.ui.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghour.app.R
import com.aghour.app.data.remote.ArticleDto
import com.aghour.app.ui.components.AppTopBar
import com.aghour.app.ui.components.BrandHeader
import com.aghour.app.ui.components.MessageBox
import com.aghour.app.ui.components.NewsCard
import com.aghour.app.ui.components.NewsMeta
import com.aghour.app.ui.components.SkeletonCards
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.components.shareText
import com.aghour.app.ui.theme.Cream
import com.aghour.app.ui.theme.Gold400

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(onArticleClick: (ArticleDto) -> Unit) {
    val container = appContainer()
    val vm: NewsViewModel = viewModel { NewsViewModel(container.catalog) }
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val reachedEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 2
        }
    }
    LaunchedEffect(reachedEnd, state.canLoadMore) {
        if (reachedEnd && state.canLoadMore) vm.loadMore()
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(R.string.tab_news), subtitle = stringResource(R.string.tagline))

        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
            when {
                state.loading -> SkeletonCards(modifier = Modifier.padding(top = 16.dp))
                state.error != null && state.articles.isEmpty() -> MessageBox(state.error!!, onRetry = vm::reload)
                state.articles.isEmpty() -> MessageBox(stringResource(R.string.no_news))
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.articles, key = { it.id }) { article ->
                        NewsCard(article, onClick = { onArticleClick(article) })
                    }
                    if (state.loadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(28.dp), color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleScreen(articleId: Long, onBack: () -> Unit) {
    val container = appContainer()
    val vm: ArticleViewModel = viewModel { ArticleViewModel(container.catalog, articleId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val article = state.article

    if (article == null) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = stringResource(R.string.tab_news), onBack = onBack)
            if (state.loading) SkeletonCards(count = 2, modifier = Modifier.padding(top = 16.dp))
            else MessageBox(state.error ?: "", onRetry = vm::load)
        }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BrandHeader {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(bottom = 26.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Cream)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { context.shareText("${article.title}\n\n${article.body}") }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share), tint = Cream)
                    }
                }
                Column(Modifier.padding(horizontal = 20.dp)) {
                    NewsMeta(article, onDark = true)
                    Spacer(Modifier.height(10.dp))
                    Text(article.title, color = Cream, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(article.author.name, color = Gold400, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Text(
            article.body,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
        )
    }
}
