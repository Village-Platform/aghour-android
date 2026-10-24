package com.aghour.app.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aghour.app.data.remote.ApiResult
import com.aghour.app.data.remote.ArticleDto
import com.aghour.app.data.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewsUiState(
    val articles: List<ArticleDto> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 1,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
) {
    val canLoadMore: Boolean get() = page < totalPages && !loading && !loadingMore && !refreshing
}

class NewsViewModel(private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(NewsUiState())
    val state: StateFlow<NewsUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        _state.value = NewsUiState(loading = true)
        viewModelScope.launch { loadPage(1) }
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true, error = null) }
        viewModelScope.launch { loadPage(1) }
    }

    fun loadMore() {
        val current = _state.value
        if (!current.canLoadMore) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch { loadPage(current.page + 1) }
    }

    private suspend fun loadPage(page: Int) {
        when (val result = catalog.articles(page)) {
            is ApiResult.Success -> _state.update {
                it.copy(
                    articles = if (page == 1) result.data.data else it.articles + result.data.data,
                    page = result.data.meta.page,
                    totalPages = result.data.meta.totalPages,
                    loading = false,
                    refreshing = false,
                    loadingMore = false,
                )
            }
            is ApiResult.Failure -> _state.update {
                it.copy(error = result.message, loading = false, refreshing = false, loadingMore = false)
            }
        }
    }
}

data class ArticleUiState(val loading: Boolean = true, val article: ArticleDto? = null, val error: String? = null)

class ArticleViewModel(private val catalog: CatalogRepository, private val articleId: Long) : ViewModel() {
    private val _state = MutableStateFlow(ArticleUiState())
    val state: StateFlow<ArticleUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = ArticleUiState(loading = true)
        viewModelScope.launch {
            _state.value = when (val result = catalog.article(articleId)) {
                is ApiResult.Success -> ArticleUiState(loading = false, article = result.data)
                is ApiResult.Failure -> ArticleUiState(loading = false, error = result.message)
            }
        }
    }
}
