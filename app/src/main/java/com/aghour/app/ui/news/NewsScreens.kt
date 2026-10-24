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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aghour.app.R
import com.aghour.app.data.remote.ArticleDto
import com.aghour.app.ui.components.AppTopBar
import com.aghour.app.ui.components.LoadingBox
import com.aghour.app.ui.components.MessageBox
import com.aghour.app.ui.components.Pill
import com.aghour.app.ui.components.appContainer
import com.aghour.app.ui.components.formatDate
import com.aghour.app.ui.components.kindLabel
import com.aghour.app.ui.home.NewsRow

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

    Scaffold(topBar = { AppTopBar(stringResource(R.string.tab_news)) }) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.error != null && state.articles.isEmpty() -> MessageBox(state.error!!, Modifier.padding(padding), onRetry = vm::refresh)
            state.articles.isEmpty() -> MessageBox(stringResource(R.string.no_news), Modifier.padding(padding))
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.articles, key = { it.id }) { article -> NewsRow(article, onClick = { onArticleClick(article) }) }
                if (state.loadingMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(28.dp))
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

    Scaffold(topBar = { AppTopBar(stringResource(R.string.tab_news), onBack) }) { padding ->
        val article = state.article
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            article == null -> MessageBox(state.error ?: "", Modifier.padding(padding), onRetry = vm::load)
            else -> Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(kindLabel(article.kind), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(formatDate(article.publishedAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                Text(article.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(article.author.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(16.dp))
                Text(article.body, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
