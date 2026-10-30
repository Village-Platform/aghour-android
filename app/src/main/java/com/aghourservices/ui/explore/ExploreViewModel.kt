package com.aghourservices.ui.explore

import com.aghourservices.data.analytics.AnalyticsEvent
import com.aghourservices.data.analytics.AnalyticsService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aghourservices.data.remote.ApiResult
import com.aghourservices.data.remote.CategoryDto
import com.aghourservices.data.remote.FirmDto
import com.aghourservices.data.repository.CatalogRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExploreUiState(
    val query: String = "",
    val categoryId: Long? = null,
    val categories: List<CategoryDto> = emptyList(),
    val firms: List<FirmDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 0,
    val totalPages: Int = 1,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
) {
    val canLoadMore: Boolean get() = page < totalPages && !loading && !loadingMore && !refreshing
}

/**
 * Explore tab: every visible firm, filtered by category chips and a search box.
 * The category can also be chosen from Home (shared [selectedCategory] flow).
 */
@OptIn(FlowPreview::class)
class ExploreViewModel(
    private val catalog: CatalogRepository,
    private val selectedCategory: MutableStateFlow<Long?>,
    /** AdminRepository.changes: a super admin changed a firm or a category. */
    private val adminChanges: StateFlow<Int>,
    private val analytics: AnalyticsService,
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreUiState(categoryId = selectedCategory.value))
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()
    private val queries = MutableStateFlow("")
    private var job: Job? = null
    /** Last search sent to analytics: typing pauses, Search key and pull-to-refresh count once. */
    private var loggedSearch: Pair<String, Long?>? = null

    init {
        viewModelScope.launch { loadCategories() }
        // Category picked on Home (or by a chip here)
        viewModelScope.launch {
            selectedCategory.collect { id ->
                _state.update { it.copy(categoryId = id) }
                reload()
            }
        }
        // Search while typing, after a short pause
        viewModelScope.launch {
            queries.drop(1).debounce(400).distinctUntilChanged().collect { reload() }
        }
        viewModelScope.launch {
            adminChanges.drop(1).collect {
                loadCategories()
                refresh()
            }
        }
    }

    private suspend fun loadCategories() {
        (catalog.categories() as? ApiResult.Success)?.let { r -> _state.update { it.copy(categories = r.data) } }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        queries.value = query.trim()
    }

    fun selectCategory(id: Long?) {
        if (id != null && id != _state.value.categoryId) {
            val name = _state.value.categories.firstOrNull { it.id == id }?.name
            analytics.log(AnalyticsEvent.SelectCategory(id, name, AnalyticsEvent.Source.Explore))
        }
        selectedCategory.value = id // collected above -> reload
    }

    fun reload() {
        job?.cancel()
        _state.update { it.copy(loading = true, error = null, firms = emptyList(), page = 0, totalPages = 1) }
        job = viewModelScope.launch { loadPage(1) }
    }

    fun refresh() {
        job?.cancel()
        _state.update { it.copy(refreshing = true, error = null) }
        job = viewModelScope.launch { loadPage(1) }
    }

    fun loadMore() {
        val current = _state.value
        if (!current.canLoadMore) return
        _state.update { it.copy(loadingMore = true) }
        job = viewModelScope.launch { loadPage(current.page + 1) }
    }

    private suspend fun loadPage(page: Int) {
        val s = _state.value
        when (val result = catalog.firms(s.query, s.categoryId, page)) {
            is ApiResult.Success -> {
                if (page == 1) logSearch(s.query, s.categoryId, result.data.meta.total)
                _state.update {
                    it.copy(
                        firms = if (page == 1) result.data.data else it.firms + result.data.data,
                        total = result.data.meta.total,
                        page = result.data.meta.page,
                        totalPages = result.data.meta.totalPages,
                        loading = false,
                        refreshing = false,
                        loadingMore = false,
                    )
                }
            }
            is ApiResult.Failure -> _state.update {
                it.copy(error = result.message, loading = false, refreshing = false, loadingMore = false)
            }
        }
    }

    private fun logSearch(query: String, categoryId: Long?, total: Int) {
        val term = query.trim()
        if (term.isEmpty() || loggedSearch == term to categoryId) return
        loggedSearch = term to categoryId
        analytics.log(AnalyticsEvent.Search(term, categoryId, total))
    }
}
