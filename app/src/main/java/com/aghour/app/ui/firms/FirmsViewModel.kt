package com.aghour.app.ui.firms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aghour.app.data.remote.ApiResult
import com.aghour.app.data.remote.FirmDto
import com.aghour.app.data.repository.CatalogRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FirmsUiState(
    val query: String = "",
    val firms: List<FirmDto> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 1,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
) {
    val canLoadMore: Boolean get() = page < totalPages && !loading && !loadingMore
}

/** Firms of one category, with search and "load more" pagination (uses the API's page meta). */
class FirmsViewModel(
    private val catalog: CatalogRepository,
    private val categoryId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(FirmsUiState())
    val state: StateFlow<FirmsUiState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        refresh()
    }

    fun onQueryChange(query: String) = _state.update { it.copy(query = query) }

    fun refresh() {
        job?.cancel()
        _state.update { it.copy(loading = true, error = null, firms = emptyList(), page = 0, totalPages = 1) }
        job = viewModelScope.launch { loadPage(1) }
    }

    fun loadMore() {
        val current = _state.value
        if (!current.canLoadMore) return
        _state.update { it.copy(loadingMore = true) }
        job = viewModelScope.launch { loadPage(current.page + 1) }
    }

    private suspend fun loadPage(page: Int) {
        when (val result = catalog.categoryFirms(categoryId, _state.value.query, page)) {
            is ApiResult.Success -> _state.update {
                it.copy(
                    firms = if (page == 1) result.data.data else it.firms + result.data.data,
                    page = result.data.meta.page,
                    totalPages = result.data.meta.totalPages,
                    loading = false,
                    loadingMore = false,
                )
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.message, loading = false, loadingMore = false) }
        }
    }
}
