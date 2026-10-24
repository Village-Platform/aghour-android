package com.aghour.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aghour.app.data.remote.ApiResult
import com.aghour.app.data.remote.ArticleDto
import com.aghour.app.data.remote.CategoryDto
import com.aghour.app.data.repository.CatalogRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val categories: List<CategoryDto> = emptyList(),
    val latestNews: List<ArticleDto> = emptyList(),
    val error: String? = null,
)

class HomeViewModel(private val catalog: CatalogRepository) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    /** First load shows skeletons; pull-to-refresh keeps the current content visible. */
    fun load(refresh: Boolean = false) {
        _state.update { it.copy(loading = !refresh, refreshing = refresh, error = null) }
        viewModelScope.launch {
            val categories = async { catalog.categories() }
            val news = async { catalog.articles(page = 1, perPage = 5) }

            when (val result = categories.await()) {
                is ApiResult.Success -> _state.update { it.copy(categories = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            }
            // News is a bonus section: if it fails, the home page still shows the categories.
            (news.await() as? ApiResult.Success)?.let { result ->
                _state.update { it.copy(latestNews = result.data.data) }
            }
            _state.update { it.copy(loading = false, refreshing = false) }
        }
    }

    fun refresh() = load(refresh = true)
}
